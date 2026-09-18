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

    fun websiteChallengeBody(code: String, playerName: String, playerUuid: String): JsonObject = JsonObject().apply {
        addProperty("action", "challenge")
        addProperty("code", code)
        addProperty("playerName", playerName)
        addProperty("playerUuid", playerUuid)
    }

    fun websiteCompleteBody(code: String): JsonObject = JsonObject().apply {
        addProperty("action", "complete")
        addProperty("code", code)
    }

    fun normalizeWebsiteCode(raw: String): String =
        raw.uppercase().filter { it in WEBSITE_CODE_ALPHABET }.take(CODE_LENGTH)

    fun isValidWebsiteCode(code: String): Boolean =
        code.length == CODE_LENGTH && code.all { it in WEBSITE_CODE_ALPHABET }

    fun isWebsiteCodeChar(ch: Char): Boolean = ch.uppercaseChar() in WEBSITE_CODE_ALPHABET

    fun normalizeLoginCode(raw: String): String = normalizeWebsiteCode(raw)

    fun isClientToken(value: String): Boolean = value.startsWith("hsm_cli_")

    fun parseBegin(body: JsonObject): Begin {
        val serverId = JsonView.str(body, "serverId")
            ?.takeIf { it.matches(SERVER_ID) }
            ?: throw ClientApiException(400, "invalid_response", "Der Login-Dienst hat ungültig geantwortet.")
        return Begin(serverId, JsonView.str(body, "expiresAt"))
    }

    fun parseWebsiteComplete(body: JsonObject): Pair<String, String> {
        if (JsonView.bool(body, "ok") == false) {
            throw ClientApiException(400, JsonView.str(body, "error") ?: "invalid_response", "Website-Anmeldung fehlgeschlagen.")
        }
        return (JsonView.str(body, "playerName", "name") ?: "") to (JsonView.str(body, "playerUuid", "uuid") ?: "")
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
        "invalid_request" -> "Die Anfrage wurde vom Server abgelehnt."
        "verification_failed" -> "Minecraft konnte deine Sitzung nicht bestätigen."
        "unauthorized" -> "Bitte erneut anmelden."
        "invalid_code" -> "Dieser Anmelde-Code ist ungültig oder abgelaufen."
        "not_found", "http_404" -> "Dieser Anmelde-Code ist ungültig oder der Login-Weg ist nicht verfügbar."
        "rate_limited" -> "Zu viele Anfragen. Warte kurz."
        else -> fallback
    }

    const val CODE_LENGTH = 6
    const val WEBSITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
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
    override val message: String,
    val body: JsonObject? = null
) : RuntimeException(message)
