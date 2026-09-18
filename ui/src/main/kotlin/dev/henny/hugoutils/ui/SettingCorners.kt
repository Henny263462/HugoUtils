package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt

class SettingCorners(
    private val key: String,
    private val read: () -> Int,
    private val write: (Int) -> Unit,
    private val maxRadius: Int = UiMetrics.CORNER_MAX
) : SettingBlock {
    override var bounds: UiRect = UiRect(0, 0, 0, 0)
        private set
    private var slider = UiRect(0, 0, 0, 0)
    private var previewSmall = UiRect(0, 0, 0, 0)
    private var previewMid = UiRect(0, 0, 0, 0)
    private var previewLarge = UiRect(0, 0, 0, 0)
    private var dragging = false

    override fun layout(x: Int, y: Int, width: Int): Int {
        bounds = UiRect(x, y, width, HEIGHT)
        val previewW = ((bounds.w - 36) / 3).coerceAtLeast(70)
        previewSmall = UiRect(bounds.x + 10, bounds.y + 42, previewW, 36)
        previewMid = UiRect(previewSmall.right + 8, previewSmall.y, previewW, 36)
        previewLarge = UiRect(previewMid.right + 8, previewSmall.y, previewW, 36)
        slider = UiRect(bounds.x + 10, previewSmall.bottom + 18, bounds.w - 20, 20)
        return HEIGHT
    }

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        val radius = read()
        UiWidgets.hoverCard(context, bounds, mouseX.toDouble(), mouseY.toDouble(), key = key)
        Labels.title(context, renderer, bounds.x + 12, bounds.y + 12, "Ecken")
        val value = if (radius <= 0) "Eckig" else "$radius px rund"
        Labels.trailing(context, renderer, bounds, value, bounds.y + 12, UiDraw.theme.accent)
        preview(context, renderer, previewSmall, UiMetrics.CORNER_SM, "Klein")
        preview(context, renderer, previewMid, UiMetrics.CORNER, "Karten")
        preview(context, renderer, previewLarge, UiMetrics.CORNER_LG, "Groß")
        UiWidgets.labeledSlider(
            context,
            renderer,
            slider,
            "Rundung",
            if (radius <= 0) "0" else "$radius",
            radius / maxRadius.toFloat(),
            mouseX.toDouble(),
            mouseY.toDouble(),
            key = "$key-slider",
            immediate = dragging
        )
        Labels.dim(context, renderer, slider.x, slider.bottom + 6, "Eckig")
        Labels.dim(context, renderer, slider.right - renderer.getWidth("Rund"), slider.bottom + 6, "Rund")
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!SliderMath.contains(slider, mouseX, mouseY)) return false
        dragging = true
        apply(mouseX)
        return true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        if (!dragging) return false
        apply(mouseX)
        return true
    }

    override fun mouseReleased() {
        dragging = false
    }

    private fun apply(mouseX: Double) {
        write((SliderMath.normalized(slider, mouseX) * maxRadius).roundToInt())
    }

    private fun preview(context: DrawContext, renderer: TextRenderer, rect: UiRect, radius: Int, label: String) {
        UiDraw.panel(context, rect, UiDraw.theme.inset, UiDraw.theme.accent, radius)
        Labels.title(
            context,
            renderer,
            rect.x + (rect.w - renderer.getWidth(label)) / 2,
            rect.y + (rect.h - 8) / 2,
            label
        )
    }

    companion object {
        const val HEIGHT = 150
    }
}
