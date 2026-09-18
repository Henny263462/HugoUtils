package dev.henny.hugoutils.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.henny.hugoutils.client.rtp.RtpShare
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
                        val me = ClientApi.me(force = true)
                        refreshAccount(me)
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
                if (ClientAuth.isClientToken(trimmed)) {
                    val session = MinecraftClient.getInstance().session
                    val uuid = session.uuidOrNull ?: throw LoginFailure("Offline-Konten können den Client-Login nicht verwenden.")
                    ClientAuth.parseComplete(JsonObject().apply { addProperty("token", trimmed) })
                    ClientSessionStore.save(ClientSession(trimmed, session.username, uuid.toString(), null))
                    runCatching { refreshAccount(ClientApi.me(force = true)) }
                    dispatchFeedback(feedback, "Market-Token gespeichert.", false)
                    return@execute
                }
                loginWebsite(trimmed)
                if (!ClientSessionStore.hasToken()) {
                    runCatching { performLogin() }
                }
                val extra = if (ClientSessionStore.hasToken()) " Market ist verbunden." else ""
                dispatchFeedback(feedback, "Website angemeldet. Die Seite übernimmt die Session selbst.$extra", false)
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
        joinMojang(begin.serverId)
        var last: ClientApiException? = null
        repeat(4) { attempt ->
            try {
                val complete = ClientAuth.parseComplete(ClientApi.complete(playerName, playerUuid, begin.serverId))
                ClientSessionStore.save(
                    ClientSession(complete.token, playerName, playerUuid, complete.expiresAt)
                )
                runCatching { refreshAccount(ClientApi.me(force = true)) }
                return
            } catch (error: ClientApiException) {
                last = error
                if (error.error != "verification_failed" || attempt == 3) throw error
                Thread.sleep(350L * (attempt + 1))
            }
        }
        throw last ?: LoginFailure("Anmeldung fehlgeschlagen.")
    }

    private fun loginWebsite(raw: String) {
        val code = ClientAuth.normalizeWebsiteCode(raw)
        if (!ClientAuth.isValidWebsiteCode(code)) {
            throw LoginFailure("Der Code muss 6 Zeichen aus ABCDEFGHJKLMNPQRSTUVWXYZ23456789 sein. Ohne I, O, 0 und 1.")
        }
        val session = MinecraftClient.getInstance().session
        val uuid = session.uuidOrNull ?: throw LoginFailure("Offline-Konten können den Website-Login nicht verwenden.")
        val playerName = session.username
        val playerUuid = uuid.toString()
        val challenge = postWebsite(ClientAuth.websiteChallengeBody(code, playerName, playerUuid))
        val serverId = ClientAuth.parseBegin(challenge).serverId
        joinMojang(serverId)
        var last: ClientApiException? = null
        repeat(3) { attempt ->
            try {
                ClientAuth.parseWebsiteComplete(postWebsite(ClientAuth.websiteCompleteBody(code)))
                return
            } catch (error: ClientApiException) {
                last = error
                if (error.error != "verification_failed" || attempt == 2) throw error
                Thread.sleep(350L * (attempt + 1))
            }
        }
        throw last ?: LoginFailure("Website-Anmeldung fehlgeschlagen.")
    }

    private fun refreshAccount(me: JsonObject) {
        ClientFlags.refreshFromApi()
        MarketPriceCache.prefetch()
        runCatching { RtpShare.syncFromMe(me) }
    }

    private fun joinMojang(serverId: String) {
        val session = MinecraftClient.getInstance().session
        val uuid = session.uuidOrNull ?: throw LoginFailure("Offline-Konten können den Client-Login nicht verwenden.")
        val accessToken = session.accessToken
        if (accessToken.isBlank()) throw LoginFailure("Deine Minecraft-Sitzung hat keinen gültigen Zugriffstoken.")
        val join = postJson(
            ApiConfig.MOJANG_SESSION_JOIN_URL,
            JsonObject().apply {
                addProperty("accessToken", accessToken)
                addProperty("selectedProfile", uuid.toString().replace("-", ""))
                addProperty("serverId", serverId)
            }
        )
        if (join.statusCode() !in 200..204) {
            throw LoginFailure("Minecraft konnte deine Sitzung nicht bestätigen. Starte den Launcher neu.")
        }
    }

    private fun postWebsite(body: JsonObject): JsonObject {
        val response = postJson(ApiConfig.CLIENT_LOGIN_URL, body)
        val json = runCatching { JsonParser.parseString(response.body().ifBlank { "{}" }).asJsonObject }.getOrNull()
            ?: JsonObject()
        if (response.statusCode() !in 200..299) {
            val error = JsonView.str(json, "error") ?: "http_${response.statusCode()}"
            throw ClientApiException(
                response.statusCode(),
                error,
                ClientAuth.userMessage(error, "Website-Login fehlgeschlagen (HTTP ${response.statusCode()}).")
            )
        }
        return json
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
