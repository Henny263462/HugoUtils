package dev.henny.hugoutils.client.config

import net.minecraft.util.math.ColorHelper

class GlintStyle : HsvColor {
    var enabled: Boolean = false
    var mode: String = Mode.CUSTOM.id
    override var hue: Float = 0.78f
    override var saturation: Float = 0.85f
    override var brightness: Float = 1.0f
    var transparency: Float = 0.25f
    var speed: Float = 0.5f
    var filter: ItemFilter = ItemFilter()

    fun copyFrom(other: GlintStyle) {
        enabled = other.enabled
        mode = other.mode
        hue = other.hue
        saturation = other.saturation
        brightness = other.brightness
        transparency = other.transparency
        speed = other.speed
        val sourceFilter = other.filter as ItemFilter?
        if (sourceFilter != null) {
            filter.copyFrom(sourceFilter)
        }
    }

    fun clamp() {
        mode = Mode.from(mode).id
        hue = GlowStyle.wrapHue(hue)
        saturation = saturation.coerceIn(0f, 1f)
        brightness = brightness.coerceIn(0f, 1f)
        transparency = transparency.coerceIn(0f, 1f)
        speed = speed.coerceIn(0f, 1f)
        filter.clamp()
    }

    fun mode(): Mode = Mode.from(mode)

    fun rgb(): IntArray = GlowStyle.hsvToRgb(hue, saturation, brightness)

    fun rgbInt(): Int {
        val c = rgb()
        return ColorHelper.getArgb(255, c[0], c[1], c[2])
    }

    fun glintAlpha(): Float = 1f - transparency

    fun speedMultiplier(): Float = 0.2f + speed * 2.3f

    override fun hex(): String {
        val c = rgb()
        return "%02X%02X%02X".format(c[0], c[1], c[2])
    }

    override fun setFromHex(hex: String): Boolean {
        val cleaned = hex.removePrefix("#").trim()
        if (cleaned.length != 6 || !cleaned.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return false
        }
        val packed = cleaned.toInt(16)
        val hsv = GlowStyle.rgbToHsv((packed shr 16) and 0xFF, (packed shr 8) and 0xFF, packed and 0xFF)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        return true
    }

    enum class Mode(val id: String, val label: String) {
        VANILLA("vanilla", "Vanilla"),
        STRONG("strong", "Stark"),
        CUSTOM("custom", "Custom"),
        RAINBOW("rainbow", "Regenbogen");

        companion object {
            fun from(value: String?): Mode =
                entries.firstOrNull { it.id.equals(value, ignoreCase = true) } ?: CUSTOM
        }
    }
}
