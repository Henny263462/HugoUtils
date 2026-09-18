package dev.henny.hugoutils.ui

import net.minecraft.util.math.ColorHelper

data class Theme(
    val background: Int = 0xE80C0C0C.toInt(),
    val overlay: Int = 0xCC000000.toInt(),
    val panel: Int = 0xFF2A2A2A.toInt(),
    val panelBorder: Int = 0xFF1A1A1A.toInt(),
    val sidebar: Int = 0xFF222222.toInt(),
    val card: Int = 0xFF313131.toInt(),
    val cardHover: Int = 0xFF3A3A3A.toInt(),
    val border: Int = 0xFF3F3F3F.toInt(),
    val inset: Int = 0xFF1F1F1F.toInt(),
    val accent: Int = 0xFF3C78C8.toInt(),
    val accentSoft: Int = 0x403C78C8,
    val accentMuted: Int = 0xFF2F5A96.toInt(),
    val text: Int = 0xFFFFFFFF.toInt(),
    val textMuted: Int = 0xFFB0B0B0.toInt(),
    val textDim: Int = 0xFF7A7A7A.toInt(),
    val success: Int = 0xFF4CAF7A.toInt(),
    val danger: Int = 0xFFC84A4A.toInt(),
    val tooltip: Int = 0xF01A1A1A.toInt(),
    val trackOff: Int = 0xFF3A3A3A.toInt(),
    val knob: Int = 0xFFF0F0F0.toInt(),
    val comingSoon: Int = 0xFF555555.toInt(),
    val helper: Int = 0xFF8A8A8A.toInt(),
    val glintPurple: Int = 0xFF9B5CFF.toInt(),
    val surfaceRaised: Int = 0xFF383838.toInt(),
    val surfacePressed: Int = 0xFF1C1C1C.toInt(),
    val focusRing: Int = 0xFF5A9AE8.toInt(),
    val shadow: Int = 0x88000000.toInt(),
    val chart: Int = 0xFF5EE6A0.toInt()
) {
    fun withAlpha(color: Int, alpha: Int): Int = ColorHelper.getArgb(
        alpha.coerceIn(0, 255), ColorHelper.getRed(color), ColorHelper.getGreen(color), ColorHelper.getBlue(color)
    )

    companion object {
        @JvmField val DEFAULT = Theme()

        fun lerpColor(from: Int, to: Int, progress: Float): Int {
            val t = progress.coerceIn(0f, 1f)
            fun channel(a: Int, b: Int) = (a + (b - a) * t).toInt()
            return ColorHelper.getArgb(
                channel(ColorHelper.getAlpha(from), ColorHelper.getAlpha(to)),
                channel(ColorHelper.getRed(from), ColorHelper.getRed(to)),
                channel(ColorHelper.getGreen(from), ColorHelper.getGreen(to)),
                channel(ColorHelper.getBlue(from), ColorHelper.getBlue(to))
            )
        }
    }
}
