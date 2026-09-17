package dev.henny.hugoutils.api

import com.google.gson.JsonObject

object ClientAuth {
    data class Begin(val serverId: String, val expiresAt: String?)
    data class Complete(val token: String, val expiresAt: String?)

    fun beginBody(playerName: String, playerUuid: String): JsonObject = JsonObject().apply {
        addProperty("playerName", playerName)
        addProperty("playerUuid", playerUuid)
    }

    fun completeBody(playerName: String, playerUuid: String, serverId: String): JsonObject = JsonObject().apply {
        addProperty("playerName", playerName)
        addProperty("playerUuid", playerUuid)
        addProperty("serverId", serverId)
    }

    fun codeBody(code: String, playerName: String, playerUuid: String): JsonObject = JsonObject().apply {
        addProperty("code", code)
        addProperty("loginCode", code)
        addProperty("playerName", playerName)
        addProperty("playerUuid", playerUuid)
    }

    fun normalizeLoginCode(raw: String): String =
        raw.filter { it.isLetterOrDigit() }.take(CODE_LENGTH)

    fun isClientToken(value: String): Boolean = value.startsWith("hsm_cli_")

    fun parseBegin(body: JsonObject): Begin {
        val serverId = JsonView.str(body, "serverId")
            ?.takeIf { it.matches(SERVER_ID) }
            ?: throw ClientApiException(400, "invalid_response", "Der Login-Dienst hat ungültig geantwortet.")
        return Begin(serverId, JsonView.str(body, "expiresAt"))
    }

    fun parseComplete(body: JsonObject): Complete {
        val nested = body.getAsJsonObject("data")
            ?: body.getAsJsonObject("session")
            ?: body.getAsJsonObject("auth")
        val deeper = nested?.getAsJsonObject("session") ?: nested?.getAsJsonObject("data")
        val token = JsonView.str(body, "token", "accessToken", "clientToken")
            ?: JsonView.str(nested, "token", "accessToken", "clientToken")
            ?: JsonView.str(deeper, "token", "accessToken", "clientToken")
            ?: throw ClientApiException(400, "invalid_response", "Kein Client-Token erhalten.")
        if (!token.startsWith("hsm_cli_")) {
            throw ClientApiException(400, "invalid_response", "Der Login-Dienst hat ein unbekanntes Token geliefert.")
        }
        return Complete(
            token,
            JsonView.str(body, "expiresAt")
                ?: JsonView.str(nested, "expiresAt")
                ?: JsonView.str(deeper, "expiresAt")
        )
    }

    fun userMessage(error: String?, fallback: String): String = when (error) {
        "invalid_request" -> "Die Anmeldung wurde vom Server abgelehnt."
        "verification_failed" -> "Minecraft konnte deine Sitzung nicht bestätigen."
        "unauthorized" -> "Bitte erneut anmelden."
        "invalid_code" -> "Dieser Anmelde-Code ist ungültig oder abgelaufen."
        "not_found", "http_404" -> "Dieser Anmelde-Code ist ungültig oder der Login-Weg ist nicht verfügbar."
        "rate_limited" -> "Zu viele Anfragen. Warte kurz."
        else -> fallback
    }

    const val CODE_LENGTH = 6
    private val SERVER_ID = Regex("^[0-9a-f]{40}$")
}

data class ClientSession(
    val token: String,
    val playerName: String,
    val playerUuid: String,
    val expiresAt: String? = null
)

class ClientApiException(
    val status: Int,
    val error: String,
    override val message: String
) : RuntimeException(message)
