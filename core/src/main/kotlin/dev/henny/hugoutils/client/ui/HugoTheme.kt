package dev.henny.hugoutils.client.ui

/** Source-compatible facade; all theme behavior and values live in hugoutils-ui. */
object HugoTheme {
    private val value get() = dev.henny.hugoutils.ui.UiDraw.theme
    val bg get() = value.background
    val overlay get() = value.overlay
    val panel get() = value.panel
    val panelBorder get() = value.panelBorder
    val sidebar get() = value.sidebar
    val card get() = value.card
    val cardHover get() = value.cardHover
    val cardBorder get() = value.border
    val inset get() = value.inset
    val accent get() = value.accent
    val accentSoft get() = value.accentSoft
    val accentMuted get() = value.accentMuted
    val text get() = value.text
    val textMuted get() = value.textMuted
    val textDim get() = value.textDim
    val success get() = value.success
    val danger get() = value.danger
    val tooltipBg get() = value.tooltip
    val trackOff get() = value.trackOff
    val knob get() = value.knob
    val comingSoon get() = value.comingSoon
    val helper get() = value.helper
    val glintPurple get() = value.glintPurple
    fun withAlpha(color: Int, alpha: Int) = value.withAlpha(color, alpha)
    fun lerpColor(from: Int, to: Int, delta: Float) =
        dev.henny.hugoutils.ui.Theme.lerpColor(from, to, delta)
}
