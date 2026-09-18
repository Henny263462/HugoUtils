package dev.henny.hugoutils.ui

import net.minecraft.util.math.ColorHelper
import kotlin.math.floor

interface HsvColor {
    var hue: Float
    var saturation: Float
    var brightness: Float
}

object Hsv {
    fun toRgb(hue: Float, saturation: Float, brightness: Float): IntArray {
        val h = ((hue % 1f) + 1f) % 1f
        val s = saturation.coerceIn(0f, 1f)
        val v = brightness.coerceIn(0f, 1f)
        val sector = floor(h * 6f).toInt()
        val fraction = h * 6f - sector
        val p = v * (1f - s)
        val q = v * (1f - fraction * s)
        val t = v * (1f - (1f - fraction) * s)
        val (r, g, b) = when (sector % 6) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        return intArrayOf((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    fun toArgb(color: HsvColor, alpha: Int = 255): Int {
        val rgb = toRgb(color.hue, color.saturation, color.brightness)
        return ColorHelper.getArgb(alpha.coerceIn(0, 255), rgb[0], rgb[1], rgb[2])
    }

    fun hex(color: HsvColor): String = toRgb(color.hue, color.saturation, color.brightness)
        .joinToString("") { "%02X".format(it) }

    fun setFromHex(color: HsvColor, value: String): Boolean {
        val clean = value.removePrefix("#")
        if (!clean.matches(Regex("[0-9a-fA-F]{6}"))) return false
        val rgb = clean.toInt(16)
        val r = ((rgb shr 16) and 0xff) / 255f
        val g = ((rgb shr 8) and 0xff) / 255f
        val b = (rgb and 0xff) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        color.hue = when {
            delta == 0f -> 0f
            max == r -> ((g - b) / delta / 6f + 1f) % 1f
            max == g -> ((b - r) / delta + 2f) / 6f
            else -> ((r - g) / delta + 4f) / 6f
        }
        color.saturation = if (max == 0f) 0f else delta / max
        color.brightness = max
        return true
    }
}
