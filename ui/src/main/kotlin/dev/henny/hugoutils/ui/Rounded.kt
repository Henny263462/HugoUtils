package dev.henny.hugoutils.ui

import net.minecraft.client.gui.DrawContext
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Anti-aliased rounded rectangles. Coverage tables are cached per radius so
 * high corner values stay cheap after the first frame.
 */
object Rounded {
    private val coverage = arrayOfNulls<FloatArray>(CACHE_SIZE)
    private val solidSpan = arrayOfNulls<IntArray>(CACHE_SIZE)

    fun cornerRadius(width: Int, height: Int, requested: Int = UiMetrics.CORNER): Int =
        requested.coerceIn(0, min(width, height) / 2)

    fun circleSpan(radius: Int, row: Int): Int {
        if (radius <= 0 || row !in 0 until radius) return 0
        val r = radius - 0.5
        val y = r - row
        return sqrt((r * r - y * y).coerceAtLeast(0.0)).toInt().coerceIn(1, radius)
    }

    fun coverage(radius: Int, col: Int, row: Int): Float {
        if (radius <= 0 || col !in 0 until radius || row !in 0 until radius) return 0f
        return table(radius)[row * radius + col]
    }

    fun fill(
        context: DrawContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        color: Int,
        radius: Int = UiMetrics.CORNER
    ) = fill(context, x, y, w, h, color, CornerRadii.all(radius))

    fun fill(
        context: DrawContext,
        rect: UiRect,
        color: Int,
        radius: Int = UiMetrics.CORNER
    ) = fill(context, rect.x, rect.y, rect.w, rect.h, color, radius)

    fun fill(
        context: DrawContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        color: Int,
        radii: CornerRadii
    ) {
        if (w <= 0 || h <= 0) return
        val c = radii.clamp(w, h)
        if (c.isZero) {
            UiDraw.fill(context, x, y, w, h, color)
            return
        }
        val top = maxOf(c.tl, c.tr)
        val bottom = maxOf(c.bl, c.br)
        if (h - top - bottom > 0) UiDraw.fill(context, x, y + top, w, h - top - bottom, color)
        if (top > 0) UiDraw.fill(context, x + c.tl, y, (w - c.tl - c.tr).coerceAtLeast(0), top, color)
        if (bottom > 0) UiDraw.fill(context, x + c.bl, y + h - bottom, (w - c.bl - c.br).coerceAtLeast(0), bottom, color)
        if (c.tl < top) UiDraw.fill(context, x, y + c.tl, c.tl, top - c.tl, color)
        if (c.tr < top) UiDraw.fill(context, x + w - c.tr, y + c.tr, c.tr, top - c.tr, color)
        if (c.bl < bottom) UiDraw.fill(context, x, y + h - bottom, c.bl, bottom - c.bl, color)
        if (c.br < bottom) UiDraw.fill(context, x + w - c.br, y + h - bottom, c.br, bottom - c.br, color)
        paintCorner(context, x, y, c.tl, color, 1, 1)
        paintCorner(context, x + w - 1, y, c.tr, color, -1, 1)
        paintCorner(context, x + w - 1, y + h - 1, c.br, color, -1, -1)
        paintCorner(context, x, y + h - 1, c.bl, color, 1, -1)
    }

    fun border(
        context: DrawContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        color: Int,
        radius: Int = UiMetrics.CORNER
    ) = border(context, x, y, w, h, color, CornerRadii.all(radius))

    fun border(
        context: DrawContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        color: Int,
        radii: CornerRadii
    ) {
        if (w <= 0 || h <= 0) return
        val c = radii.clamp(w, h)
        if (c.isZero) {
            UiDraw.border(context, x, y, w, h, color)
            return
        }
        UiDraw.fill(context, x + c.tl, y, (w - c.tl - c.tr).coerceAtLeast(0), 1, color)
        UiDraw.fill(context, x + c.bl, y + h - 1, (w - c.bl - c.br).coerceAtLeast(0), 1, color)
        UiDraw.fill(context, x, y + c.tl, 1, (h - c.tl - c.bl).coerceAtLeast(0), color)
        UiDraw.fill(context, x + w - 1, y + c.tr, 1, (h - c.tr - c.br).coerceAtLeast(0), color)
        paintCornerBorder(context, x, y, c.tl, color, 1, 1)
        paintCornerBorder(context, x + w - 1, y, c.tr, color, -1, 1)
        paintCornerBorder(context, x + w - 1, y + h - 1, c.br, color, -1, -1)
        paintCornerBorder(context, x, y + h - 1, c.bl, color, 1, -1)
    }

    fun panel(
        context: DrawContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        fill: Int,
        border: Int,
        radius: Int = UiMetrics.CORNER
    ) = panel(context, x, y, w, h, fill, border, CornerRadii.all(radius))

    fun panel(
        context: DrawContext,
        rect: UiRect,
        fill: Int,
        border: Int,
        radius: Int = UiMetrics.CORNER
    ) = panel(context, rect.x, rect.y, rect.w, rect.h, fill, border, radius)

    fun panel(
        context: DrawContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        fill: Int,
        border: Int,
        radii: CornerRadii
    ) {
        fill(context, x, y, w, h, fill, radii)
        border(context, x, y, w, h, border, radii)
    }

    fun panel(
        context: DrawContext,
        rect: UiRect,
        fill: Int,
        border: Int,
        radii: CornerRadii
    ) = panel(context, rect.x, rect.y, rect.w, rect.h, fill, border, radii)

    private fun paintCorner(
        context: DrawContext,
        cx: Int,
        cy: Int,
        radius: Int,
        color: Int,
        dirX: Int,
        dirY: Int
    ) {
        val r = radius.coerceIn(0, CACHE_SIZE - 1)
        if (r <= 0) return
        val spans = rowSpans(r)
        var row = 0
        while (row < r) {
            val span = spans[row]
            var height = 1
            while (row + height < r && spans[row + height] == span) height++
            val startX = if (dirX > 0) cx + (r - span) else cx - (r - 1)
            val startY = if (dirY > 0) cy + row else cy - (row + height - 1)
            UiDraw.fill(context, startX, startY, span, height, color)
            row += height
        }
    }

    private fun paintCornerBorder(
        context: DrawContext,
        cx: Int,
        cy: Int,
        radius: Int,
        color: Int,
        dirX: Int,
        dirY: Int
    ) {
        val r = radius.coerceIn(0, CACHE_SIZE - 1)
        if (r <= 0) return
        val spans = rowSpans(r)
        var row = 0
        while (row < r) {
            val inset = r - spans[row]
            var height = 1
            while (row + height < r && r - spans[row + height] == inset) height++
            val px = cx + dirX * inset
            val startY = if (dirY > 0) cy + row else cy - (row + height - 1)
            UiDraw.fill(context, px, startY, 1, height, color)
            row += height
        }
    }

    private fun rowSpans(radius: Int): IntArray {
        val key = radius.coerceIn(1, CACHE_SIZE - 1)
        solidSpan[key]?.let { return it }
        val data = IntArray(key) { row -> circleSpan(key, row) }
        solidSpan[key] = data
        return data
    }

    private fun table(radius: Int): FloatArray {
        val key = radius.coerceIn(1, CACHE_SIZE - 1)
        coverage[key]?.let { return it }
        val data = FloatArray(key * key)
        val center = key.toFloat()
        for (row in 0 until key) {
            for (col in 0 until key) {
                val dist = hypot(center - col - 0.5, center - row - 0.5).toFloat()
                data[row * key + col] = (key - dist + 0.5f).coerceIn(0f, 1f)
            }
        }
        coverage[key] = data
        return data
    }

    internal fun clearCacheForTests() {
        coverage.fill(null)
        solidSpan.fill(null)
    }

    private const val CACHE_SIZE = UiMetrics.CORNER_MAX + 16
}
