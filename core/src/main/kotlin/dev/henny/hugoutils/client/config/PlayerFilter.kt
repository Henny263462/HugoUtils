package dev.henny.hugoutils.client.config

import net.minecraft.util.math.ColorHelper
import java.util.Locale
import java.util.UUID

class PlayerFilter {
    var mode: String = Mode.ALL.id
    var players: MutableList<Entry> = ArrayList()

    @Synchronized
    fun copyFrom(other: PlayerFilter) {
        mode = other.mode
        val copied = synchronized(other) { other.players.map { it.copy() } }
        players.clear()
        players.addAll(copied)
    }

    @Synchronized
    fun clamp() {
        mode = Mode.from(mode).id
        val unique = LinkedHashMap<String, Entry>()
        snapshot().forEach { entry ->
            entry.clamp()
            if (entry.name.isNotBlank() || parseUuid(entry.uuid) != null) {
                unique[key(entry.name, entry.uuid)] = entry
            }
        }
        players.clear()
        players.addAll(unique.values)
    }

    @Synchronized
    fun snapshot(): List<Entry> = ArrayList(players)

    fun allows(name: String?, uuid: UUID?): Boolean {
        val selected = find(name, uuid) != null
        return when (Mode.from(mode)) {
            Mode.ALL -> true
            Mode.WHITELIST -> selected
            Mode.BLACKLIST -> !selected
        }
    }

    @Synchronized
    fun find(name: String?, uuid: UUID?): Entry? =
        snapshot().firstOrNull { entry ->
            (uuid != null && parseUuid(entry.uuid) == uuid) ||
                (!name.isNullOrBlank() && entry.name.equals(name, ignoreCase = true))
        }

    @Synchronized
    fun toggle(name: String, uuid: UUID?, defaultColor: GlowStyle? = null) {
        val existing = find(name, uuid)
        if (existing != null) {
            players.removeAll { entry ->
                (uuid != null && parseUuid(entry.uuid) == uuid) ||
                    entry.name.equals(name, ignoreCase = true)
            }
        } else {
            val entry = Entry(name.trim(), uuid?.toString().orEmpty())
            if (defaultColor != null) {
                entry.hue = defaultColor.hue
                entry.saturation = defaultColor.saturation
                entry.brightness = defaultColor.brightness
            }
            players += entry
        }
        clamp()
    }

    @Synchronized
    fun remove(entry: Entry) {
        players.removeAll { candidate ->
            entryKey(candidate) == entryKey(entry)
        }
        clamp()
    }

    fun outlineArgb(name: String?, uuid: UUID?, style: GlowStyle): Int {
        val alpha = (style.opacity * 255f).toInt().coerceIn(0, 255)
        if (alpha == 0) return 0
        val rgb = resolveRgb(name, uuid, style)
        return ColorHelper.getArgb(alpha, rgb[0], rgb[1], rgb[2])
    }

    fun glowArgb(name: String?, uuid: UUID?, style: GlowStyle, alphaScale: Float = 1f): Int {
        val alpha = (style.opacity * 255f * alphaScale).toInt().coerceIn(0, 255)
        val rgb = resolveRgb(name, uuid, style)
        return ColorHelper.getArgb(alpha, rgb[0], rgb[1], rgb[2])
    }

    fun previewRgb(entry: Entry, style: GlowStyle): IntArray =
        if (entry.customColor) {
            GlowStyle.hsvToRgb(entry.hue, entry.saturation, entry.brightness)
        } else {
            style.rgb()
        }

    private fun resolveRgb(name: String?, uuid: UUID?, style: GlowStyle): IntArray {
        val entry = find(name, uuid)
        return if (entry != null && entry.customColor) {
            GlowStyle.hsvToRgb(entry.hue, entry.saturation, entry.brightness)
        } else {
            style.rgb()
        }
    }

    class Entry(
        var name: String = "",
        var uuid: String = "",
        var customColor: Boolean = false,
        override var hue: Float = 0.32f,
        override var saturation: Float = 0.82f,
        override var brightness: Float = 1.0f
    ) : HsvColor {
        fun copy(): Entry = Entry(name, uuid, customColor, hue, saturation, brightness)

        fun clamp() {
            name = name.trim().take(16)
            uuid = uuid.trim().lowercase(Locale.ROOT)
            hue = GlowStyle.wrapHue(hue)
            saturation = saturation.coerceIn(0f, 1f)
            brightness = brightness.coerceIn(0f, 1f)
        }

        override fun hex(): String {
            val c = GlowStyle.hsvToRgb(hue, saturation, brightness)
            return "%02X%02X%02X".format(c[0], c[1], c[2])
        }

        override fun setFromHex(hex: String): Boolean {
            val cleaned = hex.removePrefix("#").trim()
            if (cleaned.length != 6 || !cleaned.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                return false
            }
            val packed = cleaned.toInt(16)
            val r = (packed shr 16) and 0xFF
            val g = (packed shr 8) and 0xFF
            val b = packed and 0xFF
            val hsv = GlowStyle.rgbToHsv(r, g, b)
            hue = hsv[0]
            saturation = hsv[1]
            brightness = hsv[2]
            customColor = true
            return true
        }
    }

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

        fun entryKey(entry: Entry): String =
            parseUuid(entry.uuid)?.toString() ?: entry.name.lowercase(Locale.ROOT)

        private fun key(name: String, uuid: String): String =
            parseUuid(uuid)?.toString() ?: name.lowercase(Locale.ROOT)
    }
}
