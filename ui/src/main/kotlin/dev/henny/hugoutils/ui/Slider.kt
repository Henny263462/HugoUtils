package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt

class Slider(
    initialValue: Float = 0f,
    var min: Float = 0f,
    var max: Float = 1f,
    val onChange: (Float) -> Unit = {},
    bounds: UiRect = UiRect(0, 0, 100, 12)
) : Control(bounds) {
    var value = initialValue.coerceIn(min, max)
        private set
    private var dragging = false
    private val displayed = AnimatedFloat(value, .1f)

    fun set(value: Float, notify: Boolean = false) {
        this.value = value.coerceIn(min, max)
        if (notify) onChange(this.value)
    }

    private fun apply(mouseX: Double) {
        val progress = SliderMath.normalized(bounds, mouseX)
        set(min + (max - min) * progress, true)
    }

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && SliderMath.contains(bounds, mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered, dragging)
        val t = UiDraw.theme
        val progress = if (max == min) 0f else (value - min) / (max - min)
        if (dragging) displayed.snapTo(progress) else displayed.animateTo(progress)
        displayed.update(UiFrame.deltaSeconds)
        val track = SliderMath.trackIn(bounds)
        val radius = UiMetrics.radiusSmFor(track.h)
        Rounded.fill(context, track.x, track.y, track.w, track.h, Theme.lerpColor(t.border, t.textDim, interaction.hover.value), radius)
        val filled = (track.w * displayed.value).roundToInt()
        if (filled > 0) Rounded.fill(context, track.x, track.y, filled, track.h, t.accent, radius)
        val knob = if (hovered || dragging) 8 else 6
        val knobX = (track.x + filled).coerceIn(track.x, track.right)
        Rounded.fill(
            context,
            knobX - knob / 2,
            track.y + track.h / 2 - knob / 2,
            knob,
            knob,
            t.knob,
            UiMetrics.radiusSmFor(knob)
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !SliderMath.contains(bounds, mouseX, mouseY)) return false
        dragging = true
        interaction.pulse()
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
}
