package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt

class ProgressBar(
    bounds: UiRect = UiRect(0, 0, 100, 8),
    var progress: Float = 0f
) : Control(bounds) {
    private val displayed = AnimatedFloat(progress.coerceIn(0f, 1f), .12f)

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        displayed.animateTo(progress.coerceIn(0f, 1f))
        displayed.update(UiFrame.deltaSeconds)
        val radius = UiMetrics.radiusSmFor(bounds.h)
        Rounded.fill(context, bounds, UiDraw.theme.inset, radius)
        val fill = ((bounds.w - 2) * displayed.value).roundToInt()
        if (fill > 0) {
            Rounded.fill(context, bounds.x + 1, bounds.y + 1, fill, bounds.h - 2, UiDraw.theme.accent, radius)
        }
    }
}

class Sparkline(
    bounds: UiRect = UiRect(0, 0, 80, 28),
    var values: List<Float> = emptyList(),
    var color: Int = UiDraw.theme.chart,
    var fill: Boolean = false
) : Control(bounds) {
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible || values.size < 2) return
        Charts.sparkline(context, bounds, values, color, fill)
    }
}
