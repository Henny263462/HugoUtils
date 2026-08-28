package dev.henny.hugoutils.client.ui

import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper

object HugoTheme {
    const val bg = 0xE80B0D12.toInt()
    const val overlay = 0xB2080A10.toInt()
    const val panel = 0xF210131A.toInt()
    const val panelBorder = 0xFF262C38.toInt()
    const val sidebar = 0xF10E1118.toInt()
    const val card = 0xFF161A23.toInt()
    const val cardHover = 0xFF1B2130.toInt()
    const val cardBorder = 0xFF2A3140.toInt()
    const val inset = 0xFF0C0F16.toInt()
    const val accent = 0xFF6EE7FF.toInt()
    const val accentSoft = 0x336EE7FF
    const val accentMuted = 0xFF3E8EA3.toInt()
    const val text = 0xFFF4F6FA.toInt()
    const val textMuted = 0xFF8B93A7.toInt()
    const val textDim = 0xFF5C6478.toInt()
    const val success = 0xFF3DDC97.toInt()
    const val trackOff = 0xFF2A3140.toInt()
    const val knob = 0xFFF7FAFC.toInt()
    const val comingSoon = 0xFF3A4152.toInt()
    const val tooltipBg = 0xF6121620.toInt()
    const val helper = 0xFF7A8299.toInt()
    const val danger = 0xFFFF6B6B.toInt()
    const val glintPurple = 0xFF9B5CFF.toInt()

    fun withAlpha(color: Int, alpha: Int): Int {
        return ColorHelper.getArgb(
            alpha.coerceIn(0, 255),
            ColorHelper.getRed(color),
            ColorHelper.getGreen(color),
            ColorHelper.getBlue(color)
        )
    }

    fun lerpColor(from: Int, to: Int, delta: Float): Int {
        val t = delta.coerceIn(0f, 1f)
        return ColorHelper.getArgb(
            MathHelper.lerp(t, ColorHelper.getAlpha(from).toFloat(), ColorHelper.getAlpha(to).toFloat()).toInt(),
            MathHelper.lerp(t, ColorHelper.getRed(from).toFloat(), ColorHelper.getRed(to).toFloat()).toInt(),
            MathHelper.lerp(t, ColorHelper.getGreen(from).toFloat(), ColorHelper.getGreen(to).toFloat()).toInt(),
            MathHelper.lerp(t, ColorHelper.getBlue(from).toFloat(), ColorHelper.getBlue(to).toFloat()).toInt()
        )
    }
}
