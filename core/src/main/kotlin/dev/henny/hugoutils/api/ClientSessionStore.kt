package dev.henny.hugoutils.api

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object ClientSessionStore {
    internal var directoryOverride: Path? = null
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    @Volatile
    private var session: ClientSession? = null

    fun current(): ClientSession? = session
    fun hasToken(): Boolean = !session?.token.isNullOrBlank()
    fun token(): String? = session?.token

    @Synchronized
    fun load() {
        val path = file()
        if (!Files.exists(path)) {
            session = null
            return
        }
        session = runCatching {
            Files.newBufferedReader(path).use { reader ->
                val json = JsonParser.parseReader(reader).asJsonObject
                val token = json.get("token")?.asString.orEmpty()
                if (!token.startsWith("hsm_cli_")) null
                else ClientSession(
                    token = token,
                    playerName = json.get("playerName")?.asString.orEmpty(),
                    playerUuid = json.get("playerUuid")?.asString.orEmpty(),
                    expiresAt = json.get("expiresAt")?.asString
                )
            }
        }.getOrNull()
    }

    @Synchronized
    fun save(next: ClientSession) {
        session = next
        val path = file()
        Files.createDirectories(path.parent)
        val tmp = path.resolveSibling("${path.fileName}.tmp")
        Files.writeString(tmp, gson.toJson(JsonObject().apply {
            addProperty("token", next.token)
            addProperty("playerName", next.playerName)
            addProperty("playerUuid", next.playerUuid)
            next.expiresAt?.let { addProperty("expiresAt", it) }
        }))
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    @Synchronized
    fun clear() {
        session = null
        runCatching { Files.deleteIfExists(file()) }
    }

    private fun file(): Path =
        (directoryOverride ?: FabricLoader.getInstance().configDir.resolve("hugoutils"))
            .resolve("client-session.json")
}
