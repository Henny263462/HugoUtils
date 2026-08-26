package dev.henny.hugoutils.client.config

class ItemFilter {
    var mode: String = Mode.ALL.id
    var items: MutableList<String> = ArrayList()

    fun copyFrom(other: ItemFilter) {
        mode = other.mode
        items.clear()
        items.addAll(other.items)
    }

    fun clamp() {
        mode = Mode.from(mode).id
        items.removeIf { it.isBlank() }
        items.replaceAll { it.trim().lowercase() }
        val unique = LinkedHashSet(items)
        items.clear()
        items.addAll(unique)
    }

    enum class Mode(val id: String, val label: String) {
        ALL("all", "Alle Items"),
        WHITELIST("whitelist", "Nur ausgewählte"),
        BLACKLIST("blacklist", "Alle außer");

        companion object {
            fun from(value: String?): Mode =
                entries.firstOrNull { it.id.equals(value, ignoreCase = true) } ?: ALL
        }
    }
}
