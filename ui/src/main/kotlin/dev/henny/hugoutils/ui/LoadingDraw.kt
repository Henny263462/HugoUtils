package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object LoadingDraw {
    fun pulse(nowMs: Long = System.currentTimeMillis()): Float {
        val wave = sin(nowMs / 280.0 * PI).toFloat()
        return 0.42f + 0.38f * (0.5f + 0.5f * wave)
    }

    fun spinner(
        context: DrawContext,
        cx: Int,
        cy: Int,
        radius: Int = 8,
        color: Int = UiDraw.theme.accent,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val ticks = 10
        val phase = ((nowMs / 70L) % ticks).toInt()
        for (index in 0 until ticks) {
            val falloff = ((index - phase + ticks) % ticks) / ticks.toFloat()
            val angle = index * (PI * 2.0 / ticks)
            val size = if (falloff < 0.18f) 3 else 2
            val px = (cx + cos(angle) * radius - size / 2.0).toInt()
            val py = (cy + sin(angle) * radius - size / 2.0).toInt()
            UiDraw.fill(context, px, py, size, size, UiDraw.theme.withAlpha(color, (50 + ((1f - falloff) * 205)).toInt()))
        }
    }

    fun skeleton(context: DrawContext, rect: UiRect, nowMs: Long = System.currentTimeMillis()) {
        UiDraw.panel(context, rect, Theme.lerpColor(UiDraw.theme.inset, UiDraw.theme.cardHover, pulse(nowMs)), UiDraw.theme.border)
    }

    fun bone(context: DrawContext, x: Int, y: Int, w: Int, h: Int, nowMs: Long = System.currentTimeMillis()) {
        if (w <= 0 || h <= 0) return
        Rounded.fill(
            context,
            x,
            y,
            w,
            h,
            Theme.lerpColor(UiDraw.theme.inset, UiDraw.theme.cardHover, pulse(nowMs)),
            UiMetrics.CORNER_SM
        )
    }

    fun row(context: DrawContext, rect: UiRect, nowMs: Long = System.currentTimeMillis()) {
        skeleton(context, rect, nowMs)
        val pad = 6
        val icon = 16.coerceAtMost(rect.h - pad * 2)
        bone(context, rect.x + pad, rect.y + (rect.h - icon) / 2, icon, icon, nowMs)
        val textX = rect.x + pad + icon + 6
        val textW = (rect.right - 10 - textX).coerceAtLeast(12)
        bone(context, textX, rect.y + 6, (textW * 0.62f).toInt().coerceAtLeast(16), 6, nowMs)
        bone(context, textX, rect.y + rect.h - 12, (textW * 0.38f).toInt().coerceAtLeast(12), 5, nowMs)
    }

    fun overlay(context: DrawContext, rect: UiRect, renderer: TextRenderer? = null, label: String = "Lädt …") {
        UiDraw.fill(context, rect, UiDraw.alpha(UiDraw.theme.overlay, 0.28f))
        spinner(context, rect.x + rect.w / 2, rect.y + rect.h / 2 - if (renderer != null) 6 else 0)
        if (renderer != null) {
            val textW = renderer.getWidth(label)
            context.drawText(renderer, label, rect.x + (rect.w - textW) / 2, rect.y + rect.h / 2 + 12, UiDraw.theme.textMuted, false)
        }
    }
}
