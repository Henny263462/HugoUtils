package dev.henny.hugoutils.api

import com.google.gson.JsonObject
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object AuthApiClient {
    private val busy = AtomicBoolean(false)
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "HugoUtils-Auth").apply { isDaemon = true }
    }

    @JvmStatic
    fun login(source: FabricClientCommandSource) {
        login { message, error ->
            if (error) source.sendError(Text.literal(message))
            else source.sendFeedback(Text.literal(message))
        }
    }

    fun login(feedback: (message: String, error: Boolean) -> Unit) {
        login(feedback, silent = false)
    }

    fun ensureLoggedIn() {
        login({ _, _ -> }, silent = true)
    }

    fun login(feedback: (message: String, error: Boolean) -> Unit, silent: Boolean) {
        if (!busy.compareAndSet(false, true)) {
            if (!silent) dispatchFeedback(feedback, "Ein Login läuft bereits.", true)
            return
        }
        if (!silent) dispatchFeedback(feedback, "Minecraft-Konto wird bestätigt …", false)
        executor.execute {
            try {
                if (ClientSessionStore.hasToken()) {
                    try {
                        ClientApi.me(force = true)
                        ClientFlags.refreshFromApi()
                        MarketPriceCache.prefetch()
                        if (!silent) dispatchFeedback(feedback, "Angemeldet. Market, Shop und AFK sind jetzt verfügbar.", false)
                        return@execute
                    } catch (error: ClientApiException) {
                        if (error.status != 401 && error.error != "unauthorized") throw error
                        ClientSessionStore.clear()
                        ClientApi.invalidate()
                        ClientFlags.clear()
                    }
                }
                performLogin()
                if (!silent) dispatchFeedback(feedback, "Angemeldet. Market, Shop und AFK sind jetzt verfügbar.", false)
                else dispatchFeedback(feedback, "Angemeldet.", false)
            } catch (error: ClientApiException) {
                if (!silent) dispatchFeedback(feedback, ClientAuth.userMessage(error.error, error.message), true)
            } catch (error: LoginFailure) {
                if (!silent) dispatchFeedback(feedback, error.userMessage, true)
            } catch (_: Exception) {
                if (!silent) dispatchFeedback(feedback, "Login-Dienst ist gerade nicht erreichbar.", true)
            } finally {
                busy.set(false)
            }
        }
    }

    fun loginWithCode(code: String, feedback: (message: String, error: Boolean) -> Unit) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) {
            dispatchFeedback(feedback, "Bitte einen Code einfügen.", true)
            return
        }
        dispatchFeedback(feedback, "Code wird geprüft …", false)
        executor.execute {
            val deadline = System.currentTimeMillis() + 12_000L
            while (!busy.compareAndSet(false, true)) {
                if (System.currentTimeMillis() > deadline) {
                    dispatchFeedback(feedback, "Ein Login läuft bereits. Bitte kurz warten und erneut versuchen.", true)
                    return@execute
                }
                Thread.sleep(40)
            }
            try {
                val session = MinecraftClient.getInstance().session
                val uuid = session.uuidOrNull ?: throw LoginFailure("Offline-Konten können den Client-Login nicht verwenden.")
                val playerName = session.username
                val playerUuid = uuid.toString()
                val complete = if (ClientAuth.isClientToken(trimmed)) {
                    ClientAuth.parseComplete(JsonObject().apply {
                        addProperty("token", trimmed)
                    })
                } else {
                    val normalized = ClientAuth.normalizeLoginCode(trimmed)
                    if (normalized.length != ClientAuth.CODE_LENGTH) {
                        throw LoginFailure("Der Code muss ${ClientAuth.CODE_LENGTH} Zeichen haben.")
                    }
                    ClientAuth.parseComplete(ClientApi.loginCode(normalized, playerName, playerUuid))
                }
                ClientSessionStore.save(ClientSession(complete.token, playerName, playerUuid, complete.expiresAt))
                runCatching { ClientApi.me(force = true) }
                ClientFlags.refreshFromApi()
                MarketPriceCache.prefetch()
                dispatchFeedback(feedback, "Angemeldet. Market, Shop und AFK sind jetzt verfügbar.", false)
            } catch (error: ClientApiException) {
                dispatchFeedback(feedback, ClientAuth.userMessage(error.error, error.message), true)
            } catch (error: LoginFailure) {
                dispatchFeedback(feedback, error.userMessage, true)
            } catch (_: Exception) {
                dispatchFeedback(feedback, "Login-Dienst ist gerade nicht erreichbar.", true)
            } finally {
                busy.set(false)
            }
        }
    }

    fun logout(feedback: (message: String, error: Boolean) -> Unit) {
        executor.execute {
            try {
                if (ClientSessionStore.hasToken()) runCatching { ClientApi.revoke() }
            } finally {
                ClientSessionStore.clear()
                ClientApi.invalidate()
                ClientFlags.clear()
                dispatchFeedback(feedback, "Abgemeldet.", false)
            }
        }
    }

    private fun performLogin() {
        val session = MinecraftClient.getInstance().session
        val uuid = session.uuidOrNull ?: throw LoginFailure("Offline-Konten können den Client-Login nicht verwenden.")
        val accessToken = session.accessToken
        if (accessToken.isBlank()) throw LoginFailure("Deine Minecraft-Sitzung hat keinen gültigen Zugriffstoken.")
        val playerName = session.username
        val playerUuid = uuid.toString()

        val begin = ClientAuth.parseBegin(ClientApi.begin(playerName, playerUuid))
        val join = postJson(
            ApiConfig.MOJANG_SESSION_JOIN_URL,
            JsonObject().apply {
                addProperty("accessToken", accessToken)
                addProperty("selectedProfile", uuid.toString().replace("-", ""))
                addProperty("serverId", begin.serverId)
            }
        )
        if (join.statusCode() != 204) {
            throw LoginFailure("Minecraft konnte deine Sitzung nicht bestätigen. Starte den Launcher neu.")
        }

        var last: ClientApiException? = null
        repeat(4) { attempt ->
            try {
                val complete = ClientAuth.parseComplete(ClientApi.complete(playerName, playerUuid, begin.serverId))
                ClientSessionStore.save(
                    ClientSession(complete.token, playerName, playerUuid, complete.expiresAt)
                )
                runCatching { ClientApi.me(force = true) }
                ClientFlags.refreshFromApi()
                MarketPriceCache.prefetch()
                return
            } catch (error: ClientApiException) {
                last = error
                if (error.error != "verification_failed" || attempt == 3) throw error
                Thread.sleep(350L * (attempt + 1))
            }
        }
        throw last ?: LoginFailure("Anmeldung fehlgeschlagen.")
    }

    private fun postJson(url: String, body: JsonObject): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", ApiConfig.USER_AGENT)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()
        return http.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun dispatchFeedback(feedback: (String, Boolean) -> Unit, message: String, error: Boolean) {
        MinecraftClient.getInstance().execute { feedback(message, error) }
    }

    private class LoginFailure(val userMessage: String) : RuntimeException(userMessage)
}
