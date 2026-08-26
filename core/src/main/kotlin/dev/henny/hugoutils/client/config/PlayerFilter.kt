package dev.henny.hugoutils.client.config

import java.util.Locale
import java.util.UUID

class PlayerFilter {
    var mode: String = Mode.ALL.id
    var players: MutableList<Entry> = ArrayList()

    fun copyFrom(other: PlayerFilter) {
        mode = other.mode
        players.clear()
        players.addAll(other.players.map { Entry(it.name, it.uuid) })
    }

    fun clamp() {
        mode = Mode.from(mode).id
        val unique = LinkedHashMap<String, Entry>()
        players.forEach { entry ->
            entry.name = entry.name.trim().take(16)
            entry.uuid = entry.uuid.trim().lowercase(Locale.ROOT)
            if (entry.name.isNotBlank() || parseUuid(entry.uuid) != null) {
                unique[key(entry.name, entry.uuid)] = entry
            }
        }
        players.clear()
        players.addAll(unique.values)
    }

    fun allows(name: String?, uuid: UUID?): Boolean {
        val selected = players.any { entry ->
            (uuid != null && parseUuid(entry.uuid) == uuid) ||
                (!name.isNullOrBlank() && entry.name.equals(name, ignoreCase = true))
        }
        return when (Mode.from(mode)) {
            Mode.ALL -> true
            Mode.WHITELIST -> selected
            Mode.BLACKLIST -> !selected
        }
    }

    fun toggle(name: String, uuid: UUID?) {
        val index = players.indexOfFirst {
            (uuid != null && parseUuid(it.uuid) == uuid) || it.name.equals(name, ignoreCase = true)
        }
        if (index >= 0) players.removeAt(index)
        else players += Entry(name.trim(), uuid?.toString().orEmpty())
        clamp()
    }

    data class Entry(var name: String = "", var uuid: String = "")

    enum class Mode(val id: String, val label: String) {
        ALL("all", "Alle Spieler"),
        WHITELIST("whitelist", "Nur ausgewählte"),
        BLACKLIST("blacklist", "Alle außer");

        companion object {
            fun from(value: String?): Mode =
                entries.firstOrNull { it.id.equals(value, ignoreCase = true) } ?: ALL
        }
    }

    companion object {
        fun parseUuid(value: String): UUID? = runCatching { UUID.fromString(value) }.getOrNull()
        private fun key(name: String, uuid: String): String =
            parseUuid(uuid)?.toString() ?: name.lowercase(Locale.ROOT)
    }
}
