package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt

/**
 * Shared rendering for pages that cannot own Control instances yet.
 * Stable keys preserve animation state across layout passes.
 */
object UiWidgets {
    private class State(initial: Float = 0f) {
        val value = AnimatedFloat(initial, .15f)
        val hover = AnimatedFloat(0f, .11f)
    }

    private val toggleStates = mutableMapOf<Any, State>()
    private val buttonStates = mutableMapOf<Any, State>()
    private val sliderStates = mutableMapOf<Any, State>()

    fun toggle(
        context: DrawContext,
        rect: UiRect,
        enabled: Boolean,
        mouseX: Double,
        mouseY: Double,
        interactive: Boolean = true,
        key: Any = rect
    ) {
        val state = toggleStates.getOrPut(key) { State(if (enabled) 1f else 0f) }
        val hovered = interactive && rect.contains(mouseX, mouseY)
        state.value.animateTo(if (enabled) 1f else 0f)
        state.hover.animateTo(if (hovered) 1f else 0f)
        state.value.update(UiFrame.deltaSeconds)
        state.hover.update(UiFrame.deltaSeconds)

        val theme = UiDraw.theme
        val track = Theme.lerpColor(theme.trackOff, theme.success, state.value.value)
        UiDraw.panel(
            context,
            rect,
            Theme.lerpColor(track, theme.surfaceRaised, state.hover.value * .18f),
            Theme.lerpColor(theme.border, theme.accent, state.hover.value)
        )
        val knobWidth = 12
        val knobX = rect.x + 2 + ((rect.w - knobWidth - 4) * state.value.value).roundToInt()
        val knob = UiRect(knobX, rect.y + 2, knobWidth, rect.h - 4)
        UiDraw.shadow(context, knob, .3f)
        UiDraw.fill(context, knob, if (interactive) theme.knob else theme.textDim)
    }

    fun toggleProgress(
        context: DrawContext,
        rect: UiRect,
        progress: Float,
        mouseX: Double,
        mouseY: Double,
        interactive: Boolean = true,
        key: Any = rect
    ) {
        val state = toggleStates.getOrPut(key) { State(progress) }
        state.value.snapTo(progress.coerceIn(0f, 1f))
        val hovered = interactive && rect.contains(mouseX, mouseY)
        state.hover.animateTo(if (hovered) 1f else 0f)
        state.hover.update(UiFrame.deltaSeconds)
        val theme = UiDraw.theme
        val track = Theme.lerpColor(theme.trackOff, theme.success, state.value.value)
        UiDraw.panel(context, rect, track, Theme.lerpColor(theme.border, theme.accent, state.hover.value))
        val knobWidth = 12
        val knobX = rect.x + 2 + ((rect.w - knobWidth - 4) * state.value.value).roundToInt()
        UiDraw.fill(context, UiRect(knobX, rect.y + 2, knobWidth, rect.h - 4), if (interactive) theme.knob else theme.textDim)
    }

    fun button(
        context: DrawContext,
        renderer: TextRenderer,
        rect: UiRect,
        label: String,
        mouseX: Double,
        mouseY: Double,
        enabled: Boolean = true,
        style: ButtonStyle = ButtonStyle.SECONDARY,
        key: Any = rect
    ) {
        val state = buttonStates.getOrPut(key) { State() }
        val hovered = enabled && rect.contains(mouseX, mouseY)
        state.hover.animateTo(if (hovered) 1f else 0f)
        state.hover.update(UiFrame.deltaSeconds)
        val theme = UiDraw.theme
        val base = when (style) {
            ButtonStyle.PRIMARY -> theme.accentMuted
            ButtonStyle.SECONDARY -> theme.inset
            ButtonStyle.GHOST -> theme.sidebar
            ButtonStyle.DANGER -> theme.withAlpha(theme.danger, 42)
        }
        val target = when (style) {
            ButtonStyle.PRIMARY -> theme.accent
            ButtonStyle.DANGER -> theme.danger
            else -> theme.surfaceRaised
        }
        UiDraw.shadow(context, rect, state.hover.value * .24f)
        UiDraw.panel(
            context,
            rect,
            Theme.lerpColor(base, target, state.hover.value * .7f),
            Theme.lerpColor(theme.border, if (style == ButtonStyle.DANGER) theme.danger else theme.accent, state.hover.value)
        )
        val shown = UiDraw.ellipsize(renderer, label, rect.w - 8)
        context.drawText(
            renderer,
            shown,
            rect.x + (rect.w - renderer.getWidth(shown)) / 2,
            rect.y + (rect.h - 8) / 2,
            if (enabled) theme.text else theme.textDim,
            false
        )
    }

    fun sliderTrack(
        context: DrawContext,
        rect: UiRect,
        normalized: Float,
        hovered: Boolean,
        enabled: Boolean = true,
        key: Any = rect
    ) {
        val target = normalized.coerceIn(0f, 1f)
        val state = sliderStates.getOrPut(key) { State(target) }
        state.value.animateTo(target)
        state.hover.animateTo(if (hovered && enabled) 1f else 0f)
        state.value.update(UiFrame.deltaSeconds)
        state.hover.update(UiFrame.deltaSeconds)
        val theme = UiDraw.theme
        UiDraw.panel(context, rect, theme.inset, Theme.lerpColor(theme.border, theme.accent, state.hover.value))
        val fill = (rect.w * state.value.value).roundToInt().coerceIn(0, rect.w)
        if (fill > 2) UiDraw.fill(context, rect.x + 1, rect.y + 1, fill - 2, rect.h - 2, if (enabled) theme.accent else theme.textDim)
        val knobX = (rect.x + fill).coerceIn(rect.x + 2, rect.right - 2)
        UiDraw.fill(context, knobX - 2, rect.y - 2, 4, rect.h + 4, if (enabled) theme.knob else theme.textDim)
    }
}
