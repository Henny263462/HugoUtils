package dev.henny.hugoutils.client.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.MathHelper

data class UiRect(val x: Int, val y: Int, val w: Int, val h: Int) {
    fun contains(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h
    }

    fun right(): Int = x + w
    fun bottom(): Int = y + h
}

object UiDraw {
    fun fill(context: DrawContext, rect: UiRect, color: Int) {
        if (rect.w <= 0 || rect.h <= 0) {
            return
        }
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), color)
    }

    fun fill(context: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w <= 0 || h <= 0) {
            return
        }
        context.fill(x, y, x + w, y + h, color)
    }

    fun border(context: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int) {
        context.fill(x, y, x + w, y + 1, color)
        context.fill(x, y + h - 1, x + w, y + h, color)
        context.fill(x, y, x + 1, y + h, color)
        context.fill(x + w - 1, y, x + w, y + h, color)
    }

    fun panel(context: DrawContext, x: Int, y: Int, w: Int, h: Int, fill: Int, border: Int) {
        fill(context, x, y, w, h, fill)
        border(context, x, y, w, h, border)
    }

    fun panel(context: DrawContext, rect: UiRect, fill: Int, border: Int) {
        panel(context, rect.x, rect.y, rect.w, rect.h, fill, border)
    }

    fun ellipsize(textRenderer: TextRenderer, text: String, maxWidth: Int): String {
        if (maxWidth <= 0 || textRenderer.getWidth(text) <= maxWidth) {
            return text
        }
        var value = text
        while (value.isNotEmpty() && textRenderer.getWidth("$value…") > maxWidth) {
            value = value.dropLast(1)
        }
        return if (value.isEmpty()) "…" else "$value…"
    }

    fun lerp(current: Float, target: Float, speed: Float): Float {
        return MathHelper.lerp(speed.coerceIn(0f, 1f), current, target)
    }

    fun wrap(textRenderer: TextRenderer, text: String, maxWidth: Int): List<String> {
        val lines = ArrayList<String>()
        for (paragraph in text.split('\n')) {
            var line = ""
            for (word in paragraph.split(' ')) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (line.isEmpty() || textRenderer.getWidth(candidate) <= maxWidth) {
                    line = candidate
                } else {
                    lines += line
                    line = word
                }
            }
            if (line.isNotEmpty()) {
                lines += line
            }
        }
        return lines
    }

    fun tooltip(
        context: DrawContext,
        textRenderer: TextRenderer,
        text: String,
        mouseX: Int,
        mouseY: Int,
        screenW: Int,
        screenH: Int
    ) {
        val lines = wrap(textRenderer, text, 190)
        if (lines.isEmpty()) {
            return
        }
        val w = lines.maxOf { textRenderer.getWidth(it) } + 12
        val h = lines.size * 10 + 10
        var x = mouseX + 12
        var y = mouseY + 6
        if (x + w > screenW - 4) x = mouseX - w - 6
        if (y + h > screenH - 4) y = screenH - h - 4
        panel(context, x, y, w, h, HugoTheme.tooltipBg, HugoTheme.accentMuted)
        fill(context, x, y, 2, h, HugoTheme.accent)
        lines.forEachIndexed { index, line ->
            context.drawText(textRenderer, line, x + 6, y + 6 + index * 10, HugoTheme.text, false)
        }
    }

    fun chip(
        context: DrawContext,
        textRenderer: TextRenderer,
        rect: UiRect,
        label: String,
        selected: Boolean,
        hovered: Boolean
    ) {
        fill(context, rect, if (selected) HugoTheme.accentSoft else if (hovered) 0x18FFFFFF else HugoTheme.inset)
        border(context, rect.x, rect.y, rect.w, rect.h, if (selected) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = ellipsize(textRenderer, label, rect.w - 6)
        context.drawText(
            textRenderer,
            shown,
            rect.x + (rect.w - textRenderer.getWidth(shown)) / 2,
            rect.y + 5,
            if (selected) HugoTheme.text else HugoTheme.textMuted,
            false
        )
    }

    fun helperBadge(context: DrawContext, textRenderer: TextRenderer, rect: UiRect, hovered: Boolean) {
        fill(context, rect, if (hovered) HugoTheme.accentSoft else HugoTheme.inset)
        border(context, rect.x, rect.y, rect.w, rect.h, if (hovered) HugoTheme.accent else HugoTheme.cardBorder)
        context.drawText(textRenderer, "?", rect.x + 4, rect.y + 3, if (hovered) HugoTheme.accent else HugoTheme.helper, false)
    }

    fun scrollbar(context: DrawContext, scissor: UiRect, scroll: Int, maxScroll: Int) {
        if (maxScroll <= 0) {
            return
        }
        val trackX = scissor.right() + 5
        val trackH = scissor.h
        fill(context, trackX, scissor.y, 3, trackH, HugoTheme.inset)
        val thumbH = (trackH * scissor.h / (scissor.h + maxScroll)).coerceIn(14, trackH)
        val thumbY = scissor.y + ((trackH - thumbH) * (scroll / maxScroll.toFloat())).toInt()
        fill(context, trackX, thumbY, 3, thumbH, HugoTheme.accentMuted)
    }
}
