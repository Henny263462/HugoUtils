package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

class SettingSlider(
    private val key: String,
    var title: String,
    var minLabel: String = "",
    var maxLabel: String = "",
    private val valueText: () -> String,
    private val readNormalized: () -> Float,
    private val writeNormalized: (Float) -> Unit
) : SettingBlock {
    override var bounds: UiRect = UiRect(0, 0, 0, 0)
        private set
    private var slider = UiRect(0, 0, 0, 0)
    private var dragging = false

    override fun layout(x: Int, y: Int, width: Int): Int {
        bounds = UiRect(x, y, width, HEIGHT)
        slider = UiRect(bounds.x + 12, bounds.y + 36, bounds.w - 24, 20)
        return HEIGHT
    }

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        UiWidgets.hoverCard(context, bounds, mouseX.toDouble(), mouseY.toDouble(), key = key)
        Labels.title(context, renderer, bounds.x + 12, bounds.y + 12, title)
        Labels.trailing(context, renderer, bounds, valueText(), bounds.y + 12, UiDraw.theme.accent)
        val hovered = SliderMath.contains(slider, mouseX.toDouble(), mouseY.toDouble())
        UiWidgets.sliderTrack(
            context,
            SliderMath.trackIn(slider),
            readNormalized(),
            hovered,
            key = "$key-slider",
            immediate = true
        )
        if (minLabel.isNotEmpty()) Labels.dim(context, renderer, slider.x, slider.bottom + 6, minLabel)
        if (maxLabel.isNotEmpty()) {
            Labels.dim(context, renderer, slider.right - renderer.getWidth(maxLabel), slider.bottom + 6, maxLabel)
        }
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
        writeNormalized(SliderMath.normalized(slider, mouseX))
    }

    companion object {
        const val HEIGHT = 86
    }
}
