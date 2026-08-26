package dev.henny.hugoutils.client.config

import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper

class GlowStyle : HsvColor {
    var enabled: Boolean = true
    override var hue: Float = 0.52f
    override var saturation: Float = 0.82f
    override var brightness: Float = 1.0f
    var opacity: Float = 0.68f
    var thicknessPixels: Int = 2
    var filter: ItemFilter = ItemFilter()
    var playerFilter: PlayerFilter = PlayerFilter()

    fun copyFrom(other: GlowStyle) {
        enabled = other.enabled
        hue = other.hue
        saturation = other.saturation
        brightness = other.brightness
        opacity = other.opacity
        thicknessPixels = other.thicknessPixels
        val sourceFilter = other.filter as ItemFilter?
        if (sourceFilter != null) {
            filter.copyFrom(sourceFilter)
        }
        val sourcePlayerFilter = other.playerFilter as PlayerFilter?
        if (sourcePlayerFilter != null) {
            playerFilter.copyFrom(sourcePlayerFilter)
        }
    }

    fun clamp() {
        hue = wrapHue(hue)
        saturation = saturation.coerceIn(0f, 1f)
        brightness = brightness.coerceIn(0f, 1f)
        opacity = opacity.coerceIn(0f, 1f)
        thicknessPixels = thicknessPixels.coerceIn(1, 4)
        filter.clamp()
        playerFilter.clamp()
    }

    fun rgb(): IntArray = hsvToRgb(hue, saturation, brightness)

    fun rgbInt(): Int {
        val c = rgb()
        return ColorHelper.getArgb(255, c[0], c[1], c[2])
    }

    fun glowArgb(alphaScale: Float = 1f): Int {
        val c = rgb()
        val alpha = (opacity * 255f * alphaScale).toInt().coerceIn(0, 255)
        return ColorHelper.getArgb(alpha, c[0], c[1], c[2])
    }

    /** Color used by Minecraft's native entity-outline framebuffer. */
    fun outlineArgb(): Int {
        val c = rgb()
        val alpha = (opacity * 255f).toInt().coerceIn(0, 255)
        return if (alpha == 0) 0 else ColorHelper.getArgb(alpha, c[0], c[1], c[2])
    }

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
        val r = (packed shr 16) and 0xFF
        val g = (packed shr 8) and 0xFF
        val b = packed and 0xFF
        val hsv = rgbToHsv(r, g, b)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        return true
    }

    companion object {
        fun droppedDefault() = GlowStyle().apply {
            enabled = true
            hue = 0.52f
            saturation = 0.84f
            brightness = 1.0f
            opacity = 0.7f
            thicknessPixels = 2
        }

        fun heldDefault() = GlowStyle().apply {
            enabled = false
            hue = 0.86f
            saturation = 0.72f
            brightness = 1.0f
            opacity = 0.64f
            thicknessPixels = 2
        }

        fun playerDefault() = GlowStyle().apply {
            enabled = false
            hue = 0.32f
            saturation = 0.82f
            brightness = 1.0f
            opacity = 0.72f
            thicknessPixels = 2
        }

        fun wrapHue(value: Float): Float {
            var hue = value % 1f
            if (hue < 0f) hue += 1f
            return hue
        }

        fun hsvToRgb(hue: Float, saturation: Float, value: Float): IntArray {
            val h = wrapHue(hue) * 6f
            val s = saturation.coerceIn(0f, 1f)
            val v = value.coerceIn(0f, 1f)
            val c = v * s
            val x = c * (1f - kotlin.math.abs(h % 2f - 1f))
            val m = v - c
            val (rp, gp, bp) = when {
                h < 1f -> Triple(c, x, 0f)
                h < 2f -> Triple(x, c, 0f)
                h < 3f -> Triple(0f, c, x)
                h < 4f -> Triple(0f, x, c)
                h < 5f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            return intArrayOf(
                ((rp + m) * 255f).toInt().coerceIn(0, 255),
                ((gp + m) * 255f).toInt().coerceIn(0, 255),
                ((bp + m) * 255f).toInt().coerceIn(0, 255)
            )
        }

        fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray {
            val rf = r / 255f
            val gf = g / 255f
            val bf = b / 255f
            val max = maxOf(rf, gf, bf)
            val min = minOf(rf, gf, bf)
            val delta = max - min
            val hue = when {
                delta == 0f -> 0f
                max == rf -> wrapHue(((gf - bf) / delta) / 6f)
                max == gf -> wrapHue((2f + (bf - rf) / delta) / 6f)
                else -> wrapHue((4f + (rf - gf) / delta) / 6f)
            }
            val saturation = if (max == 0f) 0f else delta / max
            return floatArrayOf(hue, saturation, max)
        }

        fun lerp(from: Float, to: Float, delta: Float): Float =
            MathHelper.lerp(delta.coerceIn(0f, 1f), from, to)
    }
}
