package dev.henny.hugoutils.ui

import net.minecraft.client.gui.DrawContext
import kotlin.math.abs

object Charts {
    fun sparkline(
        context: DrawContext,
        rect: UiRect,
        values: List<Float>,
        color: Int,
        fill: Boolean = false,
        minOverride: Float? = null,
        maxOverride: Float? = null
    ) {
        if (values.size < 2 || rect.w < 4 || rect.h < 4) return
        val sampled = downsample(values, sampleCount(rect.w, values.size))
        val min = minOverride ?: sampled.min()
        val max = (maxOverride ?: sampled.max()).coerceAtLeast(min + 0.0001f)
        if (fill) fillUnder(context, rect, sampled, min, max, UiDraw.alpha(color, 0.18f))
        var prevX = rect.x
        var prevY = yOnChart(rect, sampled[0], min, max)
        for (index in 1 until sampled.size) {
            val x = rect.x + ((index / (sampled.size - 1f)) * (rect.w - 1)).toInt()
            val y = yOnChart(rect, sampled[index], min, max)
            line(context, prevX, prevY, x, y, color)
            prevX = x
            prevY = y
        }
    }

    fun chart(context: DrawContext, rect: UiRect, values: List<Float>, color: Int) {
        if (rect.w < 8 || rect.h < 8) return
        sparkline(context, rect, values, color, fill = true)
    }

    fun dualChart(
        context: DrawContext,
        rect: UiRect,
        first: List<Float>,
        firstColor: Int,
        second: List<Float>,
        secondColor: Int
    ) {
        if (rect.w < 8 || rect.h < 8) return
        val combined = first + second
        if (combined.size < 2) return
        val min = combined.min()
        val max = combined.max().coerceAtLeast(min + 0.0001f)
        sparkline(context, rect, first, firstColor, true, min, max)
        sparkline(context, rect, second, secondColor, false, min, max)
    }

    fun sampleCount(width: Int, valueCount: Int): Int {
        if (valueCount <= 2) return valueCount
        val budget = if (width < 80) 12 else (width / 2).coerceIn(24, 180)
        return valueCount.coerceAtMost(budget)
    }

    fun yOnChart(rect: UiRect, value: Float, min: Float, max: Float): Int {
        val span = (max - min).coerceAtLeast(0.0001f)
        return rect.bottom - 1 - ((value - min) / span * (rect.h - 1)).toInt()
    }

    fun valueAt(values: List<Float>, t: Float): Float {
        if (values.isEmpty()) return 0f
        if (values.size == 1) return values[0]
        val pos = t.coerceIn(0f, 1f) * (values.size - 1)
        val index = pos.toInt().coerceIn(0, values.lastIndex)
        val next = (index + 1).coerceAtMost(values.lastIndex)
        val fraction = pos - index
        return values[index] * (1f - fraction) + values[next] * fraction
    }

    fun chartHover(
        rect: UiRect,
        values: List<Float>,
        mouseX: Double,
        minOverride: Float? = null,
        maxOverride: Float? = null
    ): Pair<Int, Int>? {
        if (values.size < 2 || mouseX < rect.x || mouseX >= rect.right) return null
        val t = ((mouseX - rect.x) / rect.w.toDouble()).toFloat().coerceIn(0f, 1f)
        val min = minOverride ?: values.min()
        val max = (maxOverride ?: values.max()).coerceAtLeast(min + 0.0001f)
        val x = mouseX.toInt().coerceIn(rect.x, rect.right - 1)
        return x to yOnChart(rect, valueAt(values, t), min, max)
    }

    fun chartHoverX(rect: UiRect, count: Int, index: Int): Int {
        if (count <= 1) return rect.x
        return rect.x + ((index / (count - 1f)) * (rect.w - 1)).toInt()
    }

    fun chartHoverY(
        rect: UiRect,
        values: List<Float>,
        index: Int,
        minOverride: Float? = null,
        maxOverride: Float? = null
    ): Int {
        val value = values.getOrNull(index) ?: return rect.bottom
        val min = minOverride ?: values.min()
        val max = (maxOverride ?: values.max()).coerceAtLeast(min + 0.0001f)
        return yOnChart(rect, value, min, max)
    }

    fun downsample(values: List<Float>, maxPoints: Int): List<Float> {
        if (maxPoints <= 1 || values.size <= maxPoints) return values
        val last = values.size - 1
        return (0 until maxPoints).map { index ->
            values[(index * last) / (maxPoints - 1)]
        }
    }

    fun line(context: DrawContext, x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        if (x0 == x1 && y0 == y1) {
            UiDraw.fill(context, x0, y0, 1, 1, color)
            return
        }
        if (y0 == y1) {
            UiDraw.fill(context, minOf(x0, x1), y0, abs(x1 - x0) + 1, 1, color)
            return
        }
        if (x0 == x1) {
            UiDraw.fill(context, x0, minOf(y0, y1), 1, abs(y1 - y0) + 1, color)
            return
        }
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val sx = if (x0 < x1) 1 else -1
        val dy = -abs(y1 - y0)
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        var runX = x
        var runLen = 1
        while (true) {
            if (x == x1 && y == y1) {
                hRun(context, runX, x, y, runLen, color)
                return
            }
            val doubled = 2 * err
            val moveX = doubled >= dy
            val moveY = doubled <= dx
            if (moveX) err += dy
            if (moveY) err += dx
            if (moveY) {
                hRun(context, runX, x, y, runLen, color)
                if (moveX) x += sx
                y += sy
                runX = x
                runLen = 1
            } else {
                x += sx
                runLen++
            }
        }
    }

    private fun hRun(context: DrawContext, runX: Int, x: Int, y: Int, length: Int, color: Int) {
        UiDraw.fill(context, minOf(runX, x), y, length, 1, color)
    }

    private fun fillUnder(
        context: DrawContext,
        rect: UiRect,
        values: List<Float>,
        min: Float,
        max: Float,
        color: Int
    ) {
        if (values.size < 2) return
        var x = rect.x
        val last = (rect.w - 1).coerceAtLeast(1).toFloat()
        while (x < rect.right) {
            val y = yOnChart(rect, valueAt(values, (x - rect.x) / last), min, max)
            var width = 1
            while (x + width < rect.right) {
                val nextY = yOnChart(rect, valueAt(values, (x + width - rect.x) / last), min, max)
                if (nextY != y) break
                width++
            }
            val height = (rect.bottom - y).coerceAtLeast(0)
            if (height > 0) UiDraw.fill(context, x, y, width, height, color)
            x += width
        }
    }
}
