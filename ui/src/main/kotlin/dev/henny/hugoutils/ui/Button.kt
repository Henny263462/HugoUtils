package dev.henny.hugoutils.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt

open class Button(
    val label: String,
    val onClick: () -> Unit,
    bounds: UiRect = UiRect(0, 0, 0, 0),
    var style: ButtonStyle = ButtonStyle.SECONDARY
) : Control(bounds) {
    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered)
        val t = UiDraw.theme
        val base = when (style) {
            ButtonStyle.PRIMARY -> t.accentMuted
            ButtonStyle.SECONDARY -> t.inset
            ButtonStyle.GHOST -> t.sidebar
            ButtonStyle.DANGER -> t.withAlpha(t.danger, 45)
        }
        val hover = when (style) {
            ButtonStyle.PRIMARY -> t.accent
            ButtonStyle.DANGER -> t.danger
            else -> t.surfaceRaised
        }
        val fill = Theme.lerpColor(base, hover, interaction.hover.value * .72f)
        val border = Theme.lerpColor(t.border, if (style == ButtonStyle.DANGER) t.danger else t.accent, interaction.hover.value)
        UiDraw.shadow(context, bounds, .25f * interaction.hover.value)
        UiDraw.panel(context, bounds, fill, border, UiMetrics.CORNER_SM)
        val shown = UiDraw.ellipsize(renderer, label, bounds.w - 8)
        val offset = interaction.press.value.roundToInt()
        context.drawText(
            renderer,
            shown,
            bounds.x + (bounds.w - renderer.getWidth(shown)) / 2,
            bounds.y + (bounds.h - 8) / 2 + offset,
            if (enabled) t.text else t.textDim,
            false
        )
        if (hovered) tooltip?.let {
            val window = MinecraftClient.getInstance().window
            UiDraw.tooltip(context, renderer, it, mouseX, mouseY, window.scaledWidth, window.scaledHeight)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!enabled || !visible || !bounds.contains(mouseX, mouseY)) return false
        interaction.pulse()
        onClick()
        return true
    }
}

class IconButton(
    icon: String,
    onClick: () -> Unit,
    bounds: UiRect = UiRect(0, 0, UiMetrics.CONTROL_HEIGHT, UiMetrics.CONTROL_HEIGHT),
    style: ButtonStyle = ButtonStyle.GHOST
) : Button(icon, onClick, bounds, style)
