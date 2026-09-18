package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

object Labels {
    fun title(context: DrawContext, renderer: TextRenderer, x: Int, y: Int, text: String, color: Int = UiDraw.theme.text) {
        context.drawText(renderer, text, x, y, color, false)
    }

    fun muted(context: DrawContext, renderer: TextRenderer, x: Int, y: Int, text: String, maxWidth: Int = Int.MAX_VALUE) {
        val shown = if (maxWidth == Int.MAX_VALUE) text else UiDraw.ellipsize(renderer, text, maxWidth)
        context.drawText(renderer, shown, x, y, UiDraw.theme.textMuted, false)
    }

    fun dim(context: DrawContext, renderer: TextRenderer, x: Int, y: Int, text: String) {
        context.drawText(renderer, text, x, y, UiDraw.theme.textDim, false)
    }

    fun value(context: DrawContext, renderer: TextRenderer, x: Int, y: Int, text: String, color: Int = UiDraw.theme.accent) {
        context.drawText(renderer, text, x, y, color, false)
    }

    fun trailing(
        context: DrawContext,
        renderer: TextRenderer,
        rect: UiRect,
        text: String,
        y: Int,
        color: Int,
        padding: Int = 12
    ) {
        context.drawText(renderer, text, rect.right - renderer.getWidth(text) - padding, y, color, false)
    }
}
