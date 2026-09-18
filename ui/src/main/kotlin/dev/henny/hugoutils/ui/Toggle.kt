package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt

class Toggle(
    initialValue: Boolean = false,
    bounds: UiRect = UiRect(0, 0, 32, 14),
    val onChange: (Boolean) -> Unit = {}
) : Control(bounds) {
    var value = initialValue
        private set
    private val animation = Animation(if (initialValue) 1f else 0f, .14f)

    fun set(value: Boolean, notify: Boolean = false) {
        this.value = value
        animation.animateTo(if (value) 1f else 0f)
        if (notify) onChange(value)
    }

    fun tick(deltaSeconds: Float) = animation.update(deltaSeconds)

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered)
        animation.update(UiFrame.deltaSeconds)
        val t = UiDraw.theme
        val track = Theme.lerpColor(t.trackOff, t.accentMuted, animation.value)
        UiDraw.panel(
            context,
            bounds,
            Theme.lerpColor(track, t.surfaceRaised, interaction.hover.value * .25f),
            Theme.lerpColor(t.border, t.accent, interaction.hover.value),
            UiMetrics.radiusFor(bounds.h)
        )
        val knobSize = (bounds.h - 6).coerceAtLeast(8)
        val knobX = bounds.x + 3 + ((bounds.w - knobSize - 6) * animation.value).roundToInt()
        val knobRect = UiRect(knobX, bounds.y + (bounds.h - knobSize) / 2, knobSize, knobSize)
        UiDraw.shadow(context, knobRect, .35f)
        Rounded.fill(context, knobRect, t.knob, UiMetrics.radiusSmFor(knobSize))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !visible || !bounds.contains(mouseX, mouseY)) return false
        interaction.pulse()
        set(!value, true)
        return true
    }
}
