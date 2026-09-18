package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.ColorHelper

object UiDraw {
    var theme: Theme = Theme.DEFAULT

    fun cornerRadius(width: Int, height: Int, requested: Int = UiMetrics.CORNER): Int =
        Rounded.cornerRadius(width, height, requested)

    fun circleSpan(radius: Int, row: Int): Int = Rounded.circleSpan(radius, row)

    fun fill(context: DrawContext, rect: UiRect, color: Int) = fill(context, rect.x, rect.y, rect.w, rect.h, color)
    fun fill(context: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w > 0 && h > 0) context.fill(x, y, x + w, y + h, color)
    }

    fun border(context: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w <= 0 || h <= 0) return
        context.fill(x, y, x + w, y + 1, color)
        context.fill(x, y + h - 1, x + w, y + h, color)
        context.fill(x, y, x + 1, y + h, color)
        context.fill(x + w - 1, y, x + w, y + h, color)
    }

    fun roundedFill(context: DrawContext, rect: UiRect, color: Int, radius: Int = UiMetrics.CORNER) =
        Rounded.fill(context, rect, color, radius)

    fun roundedFill(context: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int, radius: Int = UiMetrics.CORNER) =
        Rounded.fill(context, x, y, w, h, color, radius)

    fun roundedFill(context: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int, radii: CornerRadii) =
        Rounded.fill(context, x, y, w, h, color, radii)

    fun roundedBorder(context: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int, radius: Int = UiMetrics.CORNER) =
        Rounded.border(context, x, y, w, h, color, radius)

    fun panel(context: DrawContext, rect: UiRect, fill: Int, border: Int, radius: Int = UiMetrics.CORNER) =
        Rounded.panel(context, rect, fill, border, radius)

    fun panel(context: DrawContext, x: Int, y: Int, w: Int, h: Int, fill: Int, border: Int, radius: Int = UiMetrics.CORNER) =
        Rounded.panel(context, x, y, w, h, fill, border, radius)

    fun panel(context: DrawContext, rect: UiRect, fill: Int, border: Int, radii: CornerRadii) =
        Rounded.panel(context, rect, fill, border, radii)

    fun ellipsize(renderer: TextRenderer, text: String, maxWidth: Int): String =
        TextDraw.ellipsize(renderer, text, maxWidth)

    fun wrap(renderer: TextRenderer, text: String, maxWidth: Int): List<String> =
        TextDraw.wrap(renderer, text, maxWidth)

    fun lerp(current: Float, target: Float, speed: Float): Float =
        current + (target - current) * speed.coerceIn(0f, 1f)

    fun alpha(color: Int, opacity: Float): Int =
        theme.withAlpha(color, (ColorHelper.getAlpha(color) * opacity.coerceIn(0f, 1f)).toInt())

    fun shadow(context: DrawContext, rect: UiRect, strength: Float = 1f) {
        if (strength < 0.04f) return
        fill(context, rect.x + 3, rect.y + 4, rect.w, rect.h, alpha(theme.shadow, strength))
    }

    fun chip(context: DrawContext, renderer: TextRenderer, rect: UiRect, label: String, selected: Boolean, hovered: Boolean) {
        val t = theme
        panel(
            context,
            rect,
            if (selected) t.accentSoft else if (hovered) 0x18FFFFFF else t.inset,
            if (selected) t.accent else t.border,
            UiMetrics.CORNER_SM
        )
        val shown = ellipsize(renderer, label, rect.w - 6)
        context.drawText(renderer, shown, rect.x + (rect.w - renderer.getWidth(shown)) / 2, rect.y + 5, if (selected) t.text else t.textMuted, false)
    }

    fun helperBadge(context: DrawContext, renderer: TextRenderer, rect: UiRect, hovered: Boolean) {
        val t = theme
        panel(context, rect, if (hovered) t.accentSoft else t.inset, if (hovered) t.accent else t.border)
        context.drawText(renderer, "?", rect.x + 4, rect.y + 3, if (hovered) t.accent else t.textMuted, false)
    }

    fun tooltip(
        context: DrawContext,
        renderer: TextRenderer,
        text: String,
        mouseX: Int,
        mouseY: Int,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val lines = wrap(renderer, text, 190)
        if (lines.isEmpty()) return
        val width = lines.maxOf { renderer.getWidth(it) } + 12
        val height = lines.size * 10 + 10
        var x = mouseX + 12
        var y = mouseY + 6
        if (x + width > screenWidth - 4) x = mouseX - width - 6
        if (y + height > screenHeight - 4) y = screenHeight - height - 4
        panel(context, x, y, width, height, theme.tooltip, theme.accentMuted, UiMetrics.CORNER_SM)
        fill(context, x, y + 4, 2, height - 8, theme.accent)
        lines.forEachIndexed { index, line ->
            context.drawText(renderer, line, x + 6, y + 6 + index * 10, theme.text, false)
        }
    }

    fun scrollbar(context: DrawContext, viewport: UiRect, scroll: Int, maxScroll: Int) {
        if (maxScroll <= 0) return
        val thumbH = (viewport.h * viewport.h / (viewport.h + maxScroll)).coerceIn(14, viewport.h)
        val thumbY = viewport.y + ((viewport.h - thumbH) * scroll / maxScroll.toFloat()).toInt()
        roundedFill(context, viewport.right + 5, viewport.y, 3, viewport.h, theme.inset, 1)
        roundedFill(context, viewport.right + 5, thumbY, 3, thumbH, theme.accentMuted, 1)
    }

    fun chart(context: DrawContext, rect: UiRect, values: List<Float>, color: Int) =
        Charts.chart(context, rect, values, color)

    fun dualChart(
        context: DrawContext,
        rect: UiRect,
        first: List<Float>,
        firstColor: Int,
        second: List<Float>,
        secondColor: Int
    ) = Charts.dualChart(context, rect, first, firstColor, second, secondColor)

    fun sparkline(
        context: DrawContext,
        rect: UiRect,
        values: List<Float>,
        color: Int,
        fill: Boolean = false,
        minOverride: Float? = null,
        maxOverride: Float? = null
    ) = Charts.sparkline(context, rect, values, color, fill, minOverride, maxOverride)

    fun sampleCount(width: Int, valueCount: Int): Int = Charts.sampleCount(width, valueCount)
    fun yOnChart(rect: UiRect, value: Float, min: Float, max: Float): Int = Charts.yOnChart(rect, value, min, max)
    fun valueAt(values: List<Float>, t: Float): Float = Charts.valueAt(values, t)
    fun chartHover(
        rect: UiRect,
        values: List<Float>,
        mouseX: Double,
        minOverride: Float? = null,
        maxOverride: Float? = null
    ): Pair<Int, Int>? = Charts.chartHover(rect, values, mouseX, minOverride, maxOverride)

    fun downsample(values: List<Float>, maxPoints: Int): List<Float> = Charts.downsample(values, maxPoints)
    fun line(context: DrawContext, x0: Int, y0: Int, x1: Int, y1: Int, color: Int) =
        Charts.line(context, x0, y0, x1, y1, color)

    fun chartHoverX(rect: UiRect, count: Int, index: Int): Int = Charts.chartHoverX(rect, count, index)
    fun chartHoverY(
        rect: UiRect,
        values: List<Float>,
        index: Int,
        minOverride: Float? = null,
        maxOverride: Float? = null
    ): Int = Charts.chartHoverY(rect, values, index, minOverride, maxOverride)

    fun loadingPulse(nowMs: Long = System.currentTimeMillis()): Float = LoadingDraw.pulse(nowMs)
    fun spinner(
        context: DrawContext,
        cx: Int,
        cy: Int,
        radius: Int = 8,
        color: Int = theme.accent,
        nowMs: Long = System.currentTimeMillis()
    ) = LoadingDraw.spinner(context, cx, cy, radius, color, nowMs)

    fun skeleton(context: DrawContext, rect: UiRect, nowMs: Long = System.currentTimeMillis()) =
        LoadingDraw.skeleton(context, rect, nowMs)

    fun skeletonBone(context: DrawContext, x: Int, y: Int, w: Int, h: Int, nowMs: Long = System.currentTimeMillis()) =
        LoadingDraw.bone(context, x, y, w, h, nowMs)

    fun skeletonRow(context: DrawContext, rect: UiRect, nowMs: Long = System.currentTimeMillis()) =
        LoadingDraw.row(context, rect, nowMs)

    fun loadingOverlay(context: DrawContext, rect: UiRect, renderer: TextRenderer? = null, label: String = "Lädt …") =
        LoadingDraw.overlay(context, rect, renderer, label)
}
