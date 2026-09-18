package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

class Tabs<T>(
    var values: List<T>,
    var selected: T,
    val label: (T) -> String = { it.toString() },
    val onSelect: (T) -> Unit = {},
    bounds: UiRect = UiRect(0, 0, 160, UiMetrics.CONTROL_HEIGHT)
) : Control(bounds) {
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible || values.isEmpty()) return
        val width = bounds.w / values.size
        values.forEachIndexed { index, value ->
            val row = UiRect(
                bounds.x + index * width,
                bounds.y,
                if (index == values.lastIndex) bounds.right - (bounds.x + index * width) else width,
                bounds.h
            )
            val hovered = enabled && row.contains(mouseX.toDouble(), mouseY.toDouble())
            UiDraw.panel(
                context,
                row,
                if (value == selected) UiDraw.theme.accentSoft else if (hovered) UiDraw.theme.surfaceRaised else UiDraw.theme.inset,
                if (value == selected) UiDraw.theme.accent else UiDraw.theme.border
            )
            val text = UiDraw.ellipsize(renderer, label(value), row.w - 8)
            context.drawText(
                renderer,
                text,
                row.x + (row.w - renderer.getWidth(text)) / 2,
                row.y + (row.h - 8) / 2,
                if (value == selected) UiDraw.theme.text else UiDraw.theme.textMuted,
                false
            )
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !visible || !bounds.contains(mouseX, mouseY) || values.isEmpty()) return false
        val index = (((mouseX - bounds.x) / bounds.w) * values.size).toInt().coerceIn(0, values.lastIndex)
        return select(values[index])
    }

    fun select(value: T): Boolean {
        if (value !in values) return false
        selected = value
        onSelect(value)
        return true
    }
}
