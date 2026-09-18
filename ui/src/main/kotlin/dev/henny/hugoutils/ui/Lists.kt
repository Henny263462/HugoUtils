package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

class Dropdown<T>(
    items: List<T>,
    val label: (T) -> String = { it.toString() },
    val onSelect: (T) -> Unit = {},
    bounds: UiRect = UiRect(0, 0, 160, UiMetrics.CONTROL_HEIGHT)
) : Control(bounds) {
    var items: List<T> = items
    var open = false
    var selected: T? = null

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered, focused = open)
        UiDraw.panel(context, bounds, if (hovered || open) UiDraw.theme.surfaceRaised else UiDraw.theme.inset, if (open) UiDraw.theme.accent else UiDraw.theme.border)
        val shown = selected?.let(label) ?: "Auswählen …"
        context.drawText(
            renderer,
            UiDraw.ellipsize(renderer, shown, bounds.w - 22),
            bounds.x + 7,
            bounds.y + (bounds.h - 8) / 2,
            if (selected == null) UiDraw.theme.textMuted else UiDraw.theme.text,
            false
        )
        context.drawText(renderer, if (open) "▲" else "▼", bounds.right - 14, bounds.y + (bounds.h - 8) / 2, UiDraw.theme.textMuted, false)
        if (open) {
            items.take(8).forEachIndexed { index, value ->
                val row = UiRect(bounds.x, bounds.bottom + index * bounds.h, bounds.w, bounds.h)
                val rowHover = row.contains(mouseX.toDouble(), mouseY.toDouble())
                UiDraw.panel(context, row, if (rowHover) UiDraw.theme.surfaceRaised else UiDraw.theme.panel, UiDraw.theme.border)
                context.drawText(
                    renderer,
                    UiDraw.ellipsize(renderer, label(value), row.w - 12),
                    row.x + 7,
                    row.y + (row.h - 8) / 2,
                    UiDraw.theme.text,
                    false
                )
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !visible) return false
        if (bounds.contains(mouseX, mouseY)) {
            open = !open
            interaction.pulse()
            return true
        }
        if (open) {
            val index = ((mouseY - bounds.bottom) / bounds.h).toInt()
            if (index in 0 until minOf(items.size, 8) && mouseX >= bounds.x && mouseX < bounds.right) {
                return select(items[index])
            }
            open = false
        }
        return false
    }

    fun select(value: T): Boolean {
        if (value !in items) return false
        selected = value
        open = false
        onSelect(value)
        return true
    }
}

class SearchList<T>(items: List<T>, val searchText: (T) -> String = { it.toString() }) {
    var items: List<T> = items
    var query: String = ""
    val filtered: List<T> get() {
        val needle = query.trim().lowercase()
        return if (needle.isEmpty()) items else items.filter { needle in searchText(it).lowercase() }
    }
}

class ScrollPane(var viewport: UiRect = UiRect(0, 0, 0, 0)) {
    var contentHeight: Int = 0
    var scroll: Int = 0
        private set
    private val animatedScroll = AnimatedFloat(0f, .1f)
    val displayedScroll: Float
        get() {
            animatedScroll.animateTo(scroll.toFloat())
            return animatedScroll.update(UiFrame.deltaSeconds)
        }
    val maxScroll: Int get() = (contentHeight - viewport.h).coerceAtLeast(0)
    fun scrollBy(pixels: Int): Int {
        scroll = (scroll + pixels).coerceIn(0, maxScroll)
        return scroll
    }
    fun reset() { scroll = 0 }
}
