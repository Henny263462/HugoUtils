package dev.henny.hugoutils.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
    private val codePattern = Regex("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}$")
    private val busy = AtomicBoolean(false)
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "HugoUtils-Auth").apply { isDaemon = true }
    }

    @JvmStatic
    fun login(rawCode: String, source: FabricClientCommandSource) {
        login(rawCode) { message, error ->
            if (error) source.sendError(Text.literal(message))
            else source.sendFeedback(Text.literal(message))
        }
    }

    fun login(rawCode: String, feedback: (message: String, error: Boolean) -> Unit) {
        val code = rawCode.trim().uppercase()
        if (!codePattern.matches(code)) {
            dispatchFeedback(feedback, "Ungültiger Login-Code. Öffne hugo.henny.dev/anmelden.", true)
            return
        }
        if (!busy.compareAndSet(false, true)) {
            dispatchFeedback(feedback, "Ein Web-Login läuft bereits.", true)
            return
        }
        dispatchFeedback(feedback, "HugoUtils: Minecraft-Konto wird sicher bestätigt …", false)
        executor.execute {
            try {
                performLogin(code)
                dispatchFeedback(feedback, "HugoUtils: Web-Login bestätigt. Kehre zum Browser zurück.", false)
            } catch (error: LoginFailure) {
                dispatchFeedback(feedback, error.userMessage, true)
            } catch (_: Exception) {
                dispatchFeedback(feedback, "HugoUtils: Login-Dienst ist gerade nicht erreichbar.", true)
            } finally {
                busy.set(false)
            }
        }
    }

    private fun performLogin(code: String) {
        val session = MinecraftClient.getInstance().session
        val uuid = session.uuidOrNull ?: throw LoginFailure("Offline-Konten können den Web-Login nicht verwenden.")
        val accessToken = session.accessToken
        if (accessToken.isBlank()) throw LoginFailure("Deine Minecraft-Sitzung hat keinen gültigen Zugriffstoken.")

        val challenge = postJson(
            ApiConfig.CLIENT_LOGIN_URL,
            JsonObject().apply {
                addProperty("action", "challenge")
                addProperty("code", code)
                addProperty("playerName", session.username)
                addProperty("playerUuid", uuid.toString())
            }
        )
        if (challenge.statusCode() != 200) throw apiFailure(challenge)
        val serverId = parse(challenge).get("serverId")?.asString
            ?.takeIf { it.matches(Regex("^[0-9a-f]{40}$")) }
            ?: throw LoginFailure("Der Login-Dienst hat ungültig geantwortet.")

        val join = postJson(
            ApiConfig.MOJANG_SESSION_JOIN_URL,
            JsonObject().apply {
                addProperty("accessToken", accessToken)
                addProperty("selectedProfile", uuid.toString().replace("-", ""))
                addProperty("serverId", serverId)
            }
        )
        if (join.statusCode() != 204) {
            throw LoginFailure("Minecraft konnte deine Sitzung nicht bestätigen. Starte den Launcher neu.")
        }

        repeat(4) { attempt ->
            val complete = postJson(
                ApiConfig.CLIENT_LOGIN_URL,
                JsonObject().apply {
                    addProperty("action", "complete")
                    addProperty("code", code)
                }
            )
            if (complete.statusCode() == 200) return
            if (complete.statusCode() != 401 || attempt == 3) throw apiFailure(complete)
            Thread.sleep(350L * (attempt + 1))
        }
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

    private fun parse(response: HttpResponse<String>): JsonObject =
        JsonParser.parseString(response.body()).asJsonObject

    private fun apiFailure(response: HttpResponse<String>): LoginFailure {
        val code = runCatching { parse(response).get("error")?.asString }.getOrNull()
        val message = when (code) {
            "invalid_code" -> "Der Code ist ungültig oder abgelaufen."
            "verification_failed" -> "Minecraft konnte deine Sitzung nicht bestätigen."
            "verification_unavailable" -> "Mojangs Sitzungsdienst ist gerade nicht erreichbar."
            else -> "Web-Login fehlgeschlagen (HTTP ${response.statusCode()})."
        }
        return LoginFailure("HugoUtils: $message")
    }

    private fun dispatchFeedback(feedback: (String, Boolean) -> Unit, message: String, error: Boolean) {
        MinecraftClient.getInstance().execute {
            feedback(message, error)
        }
    }

    private class LoginFailure(val userMessage: String) : RuntimeException(userMessage)
}
