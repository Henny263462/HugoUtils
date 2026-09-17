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

    fun clearAnimationStates() {
        toggleStates.clear()
        buttonStates.clear()
        sliderStates.clear()
    }

    private fun state(map: MutableMap<Any, State>, key: Any, initial: Float = 0f): State {
        map[key]?.let { return it }
        if (map.size >= MAX_ANIMATION_STATES) {
            repeat(MAX_ANIMATION_STATES / 4) {
                map.remove(map.keys.firstOrNull() ?: return@repeat)
            }
        }
        return State(initial).also { map[key] = it }
    }

    fun toggle(
        context: DrawContext,
        rect: UiRect,
        enabled: Boolean,
        mouseX: Double,
        mouseY: Double,
        interactive: Boolean = true,
        key: Any = rect
    ) {
        val state = state(toggleStates, key, if (enabled) 1f else 0f)
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
        val state = state(toggleStates, key, progress)
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
        val state = state(buttonStates, key)
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
            Theme.lerpColor(theme.border, if (style == ButtonStyle.DANGER) theme.danger else theme.accent, state.hover.value),
            UiMetrics.CORNER_SM
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
        val state = state(sliderStates, key, target)
        state.value.animateTo(target)
        state.hover.animateTo(if (hovered && enabled) 1f else 0f)
        state.value.update(UiFrame.deltaSeconds)
        state.hover.update(UiFrame.deltaSeconds)
        val theme = UiDraw.theme
        UiDraw.panel(context, rect, theme.inset, Theme.lerpColor(theme.border, theme.accent, state.hover.value), UiMetrics.CORNER_SM)
        val fill = (rect.w * state.value.value).roundToInt().coerceIn(0, rect.w)
        if (fill > 2) {
            UiDraw.roundedFill(
                context,
                rect.x + 1,
                rect.y + 1,
                fill - 2,
                rect.h - 2,
                if (enabled) theme.accent else theme.textDim,
                UiMetrics.CORNER_SM
            )
        }
        val knobX = (rect.x + fill).coerceIn(rect.x + 2, rect.right - 2)
        UiDraw.roundedFill(context, knobX - 3, rect.y - 1, 6, rect.h + 2, if (enabled) theme.knob else theme.textDim, UiMetrics.CORNER_SM)
    }

    fun card(context: DrawContext, rect: UiRect) {
        UiDraw.shadow(context, rect, .16f)
        UiDraw.panel(context, rect, UiDraw.theme.card, UiDraw.theme.border)
    }

    fun hoverCard(
        context: DrawContext,
        rect: UiRect,
        mouseX: Double,
        mouseY: Double,
        key: Any = rect,
        selected: Boolean = false
    ) {
        val state = state(buttonStates, key)
        val hovered = rect.contains(mouseX, mouseY)
        state.hover.animateTo(if (hovered) 1f else 0f)
        state.value.animateTo(if (selected) 1f else 0f)
        state.hover.update(UiFrame.deltaSeconds)
        state.value.update(UiFrame.deltaSeconds)
        val theme = UiDraw.theme
        UiDraw.shadow(context, rect, .12f + state.hover.value * .2f)
        UiDraw.panel(
            context,
            rect,
            Theme.lerpColor(theme.card, theme.cardHover, (state.hover.value * .85f + state.value.value * .15f).coerceIn(0f, 1f)),
            Theme.lerpColor(theme.border, theme.accent, (state.hover.value * .7f + state.value.value).coerceIn(0f, 1f))
        )
    }

    fun heading(
        context: DrawContext,
        renderer: TextRenderer,
        x: Int,
        y: Int,
        title: String,
        subtitle: String? = null
    ) {
        context.drawText(renderer, title, x, y, UiDraw.theme.text, false)
        if (!subtitle.isNullOrBlank()) {
            context.drawText(renderer, subtitle, x, y + 14, UiDraw.theme.textMuted, false)
        }
    }

    fun labeledToggle(
        context: DrawContext,
        renderer: TextRenderer,
        label: String,
        toggle: UiRect,
        enabled: Boolean,
        mouseX: Double,
        mouseY: Double,
        interactive: Boolean = true,
        key: Any = toggle
    ) {
        context.drawText(renderer, label, toggle.x - renderer.getWidth(label) - 10, toggle.y + 3, UiDraw.theme.text, false)
        toggle(context, toggle, enabled, mouseX, mouseY, interactive, key)
    }

    fun labeledSlider(
        context: DrawContext,
        renderer: TextRenderer,
        rect: UiRect,
        label: String,
        valueText: String,
        normalized: Float,
        mouseX: Double,
        mouseY: Double,
        enabled: Boolean = true,
        key: Any = rect
    ) {
        val hovered = enabled && rect.contains(mouseX, mouseY)
        context.drawText(renderer, label, rect.x, rect.y, if (hovered) UiDraw.theme.text else UiDraw.theme.textMuted, false)
        context.drawText(
            renderer,
            valueText,
            rect.right - renderer.getWidth(valueText),
            rect.y,
            UiDraw.theme.text,
            false
        )
        sliderTrack(context, UiRect(rect.x, rect.y + 12, rect.w, 6), normalized, hovered, enabled, key)
    }

    fun choice(
        context: DrawContext,
        renderer: TextRenderer,
        rect: UiRect,
        title: String,
        detail: String? = null,
        selected: Boolean,
        mouseX: Double,
        mouseY: Double
    ) {
        val hovered = rect.contains(mouseX, mouseY)
        UiDraw.panel(
            context,
            rect,
            if (selected) UiDraw.theme.accentSoft else if (hovered) UiDraw.theme.surfaceRaised else UiDraw.theme.inset,
            if (selected || hovered) UiDraw.theme.accent else UiDraw.theme.border
        )
        val marker = if (selected) "●" else "○"
        context.drawText(renderer, marker, rect.x + 7, rect.y + (rect.h - 8) / 2, if (selected) UiDraw.theme.accent else UiDraw.theme.textDim, false)
        context.drawText(renderer, title, rect.x + 22, rect.y + (rect.h - 8) / 2, UiDraw.theme.text, false)
        if (!detail.isNullOrBlank()) {
            val shown = UiDraw.ellipsize(renderer, detail, (rect.w / 2).coerceAtLeast(40))
            context.drawText(renderer, shown, rect.right - renderer.getWidth(shown) - 7, rect.y + (rect.h - 8) / 2, UiDraw.theme.textMuted, false)
        }
    }

    fun listRow(
        context: DrawContext,
        renderer: TextRenderer,
        rect: UiRect,
        label: String,
        selected: Boolean,
        mouseX: Double,
        mouseY: Double,
        detail: String? = null
    ) {
        val hovered = rect.contains(mouseX, mouseY)
        UiDraw.panel(
            context,
            rect,
            if (selected) UiDraw.theme.accentSoft else if (hovered) UiDraw.theme.surfaceRaised else UiDraw.theme.inset,
            if (selected || hovered) UiDraw.theme.accent else UiDraw.theme.border
        )
        val shownDetail = detail?.takeIf { it.isNotBlank() }?.let { UiDraw.ellipsize(renderer, it, (rect.w / 3).coerceAtLeast(36)) }
        val detailWidth = shownDetail?.let { renderer.getWidth(it) + 10 } ?: 0
        context.drawText(
            renderer,
            UiDraw.ellipsize(renderer, label, rect.w - 12 - detailWidth),
            rect.x + 6,
            rect.y + (rect.h - 8) / 2,
            UiDraw.theme.text,
            false
        )
        if (shownDetail != null) {
            context.drawText(
                renderer,
                shownDetail,
                rect.right - renderer.getWidth(shownDetail) - 7,
                rect.y + (rect.h - 8) / 2,
                UiDraw.theme.accent,
                false
            )
        }
    }

    fun navItem(
        context: DrawContext,
        renderer: TextRenderer,
        rect: UiRect,
        label: String,
        selected: Boolean,
        hovered: Boolean,
        compact: Boolean,
        available: Boolean,
        key: Any = label
    ) {
        val state = state(buttonStates, key)
        state.hover.animateTo(if (hovered && available) 1f else 0f)
        state.value.animateTo(if (selected) 1f else 0f)
        state.hover.update(UiFrame.deltaSeconds)
        state.value.update(UiFrame.deltaSeconds)
        val theme = UiDraw.theme
        val fill = Theme.lerpColor(
            Theme.lerpColor(0x00000000, theme.surfaceRaised, state.hover.value),
            theme.accentSoft,
            state.value.value
        )
        if (state.value.value > 0.02f || state.hover.value > 0.02f) {
            UiDraw.roundedFill(context, rect, fill, UiMetrics.CORNER_SM)
        }
        if (selected) {
            UiDraw.roundedFill(context, rect.x, rect.y + 3, 2, rect.h - 6, theme.accent, 1)
        }
        val color = when {
            selected -> theme.text
            !available -> theme.comingSoon
            else -> Theme.lerpColor(theme.textMuted, theme.text, state.hover.value)
        }
        val shown = UiDraw.ellipsize(renderer, label, rect.w - if (compact) 12 else 14)
        context.drawText(
            renderer,
            shown,
            rect.x + if (compact) 8 else 10,
            rect.y + (rect.h - 8) / 2,
            color,
            false
        )
    }

    private const val MAX_ANIMATION_STATES = 256
}
