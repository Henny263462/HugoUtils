package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.ColorHelper
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

data class UiRect(val x: Int, val y: Int, val w: Int, val h: Int) {
    val right: Int get() = x + w
    val bottom: Int get() = y + h
    fun right(): Int = right
    fun bottom(): Int = bottom
    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseX < right && mouseY >= y && mouseY < bottom
    fun scaleFromCenter(scale: Float): UiRect {
        val width = (w * scale).toInt().coerceAtLeast(1)
        val height = (h * scale).toInt().coerceAtLeast(1)
        return UiRect(x + (w - width) / 2, y + (h - height) / 2, width, height)
    }
}

data class Theme(
    val background: Int = 0xE80C0C0C.toInt(),
    val overlay: Int = 0xCC000000.toInt(),
    val panel: Int = 0xFF2A2A2A.toInt(),
    val panelBorder: Int = 0xFF1A1A1A.toInt(),
    val sidebar: Int = 0xFF222222.toInt(),
    val card: Int = 0xFF313131.toInt(),
    val cardHover: Int = 0xFF3A3A3A.toInt(),
    val border: Int = 0xFF3F3F3F.toInt(),
    val inset: Int = 0xFF1F1F1F.toInt(),
    val accent: Int = 0xFF3C78C8.toInt(),
    val accentSoft: Int = 0x403C78C8,
    val accentMuted: Int = 0xFF2F5A96.toInt(),
    val text: Int = 0xFFFFFFFF.toInt(),
    val textMuted: Int = 0xFFB0B0B0.toInt(),
    val textDim: Int = 0xFF7A7A7A.toInt(),
    val success: Int = 0xFF4CAF7A.toInt(),
    val danger: Int = 0xFFC84A4A.toInt(),
    val tooltip: Int = 0xF01A1A1A.toInt(),
    val trackOff: Int = 0xFF3A3A3A.toInt(),
    val knob: Int = 0xFFF0F0F0.toInt(),
    val comingSoon: Int = 0xFF555555.toInt(),
    val helper: Int = 0xFF8A8A8A.toInt(),
    val glintPurple: Int = 0xFF9B5CFF.toInt(),
    val surfaceRaised: Int = 0xFF383838.toInt(),
    val surfacePressed: Int = 0xFF1C1C1C.toInt(),
    val focusRing: Int = 0xFF5A9AE8.toInt(),
    val shadow: Int = 0x88000000.toInt()
) {
    fun withAlpha(color: Int, alpha: Int): Int = ColorHelper.getArgb(
        alpha.coerceIn(0, 255), ColorHelper.getRed(color), ColorHelper.getGreen(color), ColorHelper.getBlue(color)
    )

    companion object {
        @JvmField val DEFAULT = Theme()

        fun lerpColor(from: Int, to: Int, progress: Float): Int {
            val t = progress.coerceIn(0f, 1f)
            fun channel(a: Int, b: Int) = (a + (b - a) * t).toInt()
            return ColorHelper.getArgb(
                channel(ColorHelper.getAlpha(from), ColorHelper.getAlpha(to)),
                channel(ColorHelper.getRed(from), ColorHelper.getRed(to)),
                channel(ColorHelper.getGreen(from), ColorHelper.getGreen(to)),
                channel(ColorHelper.getBlue(from), ColorHelper.getBlue(to))
            )
        }
    }
}

object UiMetrics {
    const val SPACE_1 = 4
    const val SPACE_2 = 8
    const val SPACE_3 = 12
    const val SPACE_4 = 16
    const val CONTROL_HEIGHT = 22
    const val COMPACT_HEIGHT = 18
    const val CARD_PADDING = 12
    const val PANEL_MIN_WIDTH = 380
    const val PANEL_MAX_WIDTH = 980
    const val PANEL_MIN_HEIGHT = 260
    const val PANEL_MAX_HEIGHT = 560
    const val CORNER_MAX = 12
    var corner: Int = 0
        set(value) { field = value.coerceIn(0, CORNER_MAX) }
    val CORNER get() = corner
    val CORNER_SM get() = if (corner <= 0) 0 else (corner / 2).coerceAtLeast(1)
    val CORNER_LG get() = (corner + 2).coerceAtMost(16)
    const val RAIL_WIDTH = 92
    const val SUBNAV_WIDTH = 112
}

interface HsvColor {
    var hue: Float
    var saturation: Float
    var brightness: Float
}

object Hsv {
    fun toRgb(hue: Float, saturation: Float, brightness: Float): IntArray {
        val h = ((hue % 1f) + 1f) % 1f
        val s = saturation.coerceIn(0f, 1f)
        val v = brightness.coerceIn(0f, 1f)
        val sector = floor(h * 6f).toInt()
        val fraction = h * 6f - sector
        val p = v * (1f - s)
        val q = v * (1f - fraction * s)
        val t = v * (1f - (1f - fraction) * s)
        val (r, g, b) = when (sector % 6) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        return intArrayOf((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    fun toArgb(color: HsvColor, alpha: Int = 255): Int {
        val rgb = toRgb(color.hue, color.saturation, color.brightness)
        return ColorHelper.getArgb(alpha.coerceIn(0, 255), rgb[0], rgb[1], rgb[2])
    }

    fun hex(color: HsvColor): String = toRgb(color.hue, color.saturation, color.brightness)
        .joinToString("") { "%02X".format(it) }

    fun setFromHex(color: HsvColor, value: String): Boolean {
        val clean = value.removePrefix("#")
        if (!clean.matches(Regex("[0-9a-fA-F]{6}"))) return false
        val rgb = clean.toInt(16)
        val r = ((rgb shr 16) and 0xff) / 255f
        val g = ((rgb shr 8) and 0xff) / 255f
        val b = (rgb and 0xff) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        color.hue = when {
            delta == 0f -> 0f
            max == r -> ((g - b) / delta / 6f + 1f) % 1f
            max == g -> ((b - r) / delta + 2f) / 6f
            else -> ((r - g) / delta + 4f) / 6f
        }
        color.saturation = if (max == 0f) 0f else delta / max
        color.brightness = max
        return true
    }
}

object UiDraw {
    var theme: Theme = Theme.DEFAULT

    fun cornerRadius(width: Int, height: Int, requested: Int = UiMetrics.CORNER): Int =
        requested.coerceIn(0, min(width, height) / 2)

    fun circleSpan(radius: Int, row: Int): Int {
        if (radius <= 0 || row !in 0 until radius) return 0
        val r = radius - 0.5
        val y = r - row
        return sqrt((r * r - y * y).coerceAtLeast(0.0)).toInt().coerceIn(1, radius)
    }

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
        roundedFill(context, rect.x, rect.y, rect.w, rect.h, color, radius)

    fun roundedFill(context: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int, radius: Int = UiMetrics.CORNER) {
        if (w <= 0 || h <= 0) return
        val r = cornerRadius(w, h, radius)
        if (r <= 0) {
            fill(context, x, y, w, h, color)
            return
        }
        fill(context, x + r, y, w - 2 * r, h, color)
        fill(context, x, y + r, r, h - 2 * r, color)
        fill(context, x + w - r, y + r, r, h - 2 * r, color)
        for (row in 0 until r) {
            val span = circleSpan(r, row)
            fill(context, x + r - span, y + row, span, 1, color)
            fill(context, x + w - r, y + row, span, 1, color)
            fill(context, x + r - span, y + h - 1 - row, span, 1, color)
            fill(context, x + w - r, y + h - 1 - row, span, 1, color)
        }
    }

    fun roundedBorder(context: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int, radius: Int = UiMetrics.CORNER) {
        if (w <= 0 || h <= 0) return
        val r = cornerRadius(w, h, radius)
        if (r <= 0) {
            border(context, x, y, w, h, color)
            return
        }
        fill(context, x + r, y, w - 2 * r, 1, color)
        fill(context, x + r, y + h - 1, w - 2 * r, 1, color)
        fill(context, x, y + r, 1, h - 2 * r, color)
        fill(context, x + w - 1, y + r, 1, h - 2 * r, color)
        for (row in 0 until r) {
            val span = circleSpan(r, row)
            fill(context, x + r - span, y + row, 1, 1, color)
            fill(context, x + w - r + span - 1, y + row, 1, 1, color)
            fill(context, x + r - span, y + h - 1 - row, 1, 1, color)
            fill(context, x + w - r + span - 1, y + h - 1 - row, 1, 1, color)
        }
    }

    fun panel(context: DrawContext, rect: UiRect, fill: Int, border: Int, radius: Int = UiMetrics.CORNER) {
        roundedFill(context, rect, fill, radius)
        roundedBorder(context, rect.x, rect.y, rect.w, rect.h, border, radius)
    }
    fun panel(context: DrawContext, x: Int, y: Int, w: Int, h: Int, fill: Int, border: Int, radius: Int = UiMetrics.CORNER) =
        panel(context, UiRect(x, y, w, h), fill, border, radius)

    fun ellipsize(renderer: TextRenderer, text: String, maxWidth: Int): String {
        if (maxWidth <= 0 || renderer.getWidth(text) <= maxWidth) return text
        var value = text
        while (value.isNotEmpty() && renderer.getWidth("$value…") > maxWidth) value = value.dropLast(1)
        return if (value.isEmpty()) "…" else "$value…"
    }

    fun lerp(current: Float, target: Float, speed: Float): Float =
        current + (target - current) * speed.coerceIn(0f, 1f)

    fun alpha(color: Int, opacity: Float): Int =
        theme.withAlpha(color, (ColorHelper.getAlpha(color) * opacity.coerceIn(0f, 1f)).toInt())

    fun shadow(context: DrawContext, rect: UiRect, strength: Float = 1f) {
        val color = alpha(theme.shadow, strength)
        roundedFill(context, rect.x + 3, rect.y + 4, rect.w, rect.h, color, UiMetrics.CORNER)
    }

    fun wrap(renderer: TextRenderer, text: String, maxWidth: Int): List<String> = buildList {
        text.split('\n').forEach { paragraph ->
            var line = ""
            paragraph.split(' ').forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (line.isEmpty() || renderer.getWidth(candidate) <= maxWidth) line = candidate
                else { add(line); line = word }
            }
            if (line.isNotEmpty()) add(line)
        }
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
        if (fill) {
            fillUnder(context, rect, sampled, min, max, alpha(color, 0.18f))
        }
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

    private fun fillUnder(
        context: DrawContext,
        rect: UiRect,
        values: List<Float>,
        min: Float,
        max: Float,
        color: Int
    ) {
        if (values.size < 2) return
        for (x in rect.x until rect.right) {
            val t = (x - rect.x) / (rect.w - 1).coerceAtLeast(1).toFloat()
            val y = yOnChart(rect, valueAt(values, t), min, max)
            val h = (rect.bottom - y).coerceAtLeast(0)
            if (h > 0) fill(context, x, y, 1, h, color)
        }
    }

    fun downsample(values: List<Float>, maxPoints: Int): List<Float> {
        if (maxPoints <= 1 || values.size <= maxPoints) return values
        val last = values.size - 1
        return (0 until maxPoints).map { index ->
            values[(index * last) / (maxPoints - 1)]
        }
    }

    fun line(context: DrawContext, x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        var x = x0
        var y = y0
        val dx = kotlin.math.abs(x1 - x0)
        val sx = if (x0 < x1) 1 else -1
        val dy = -kotlin.math.abs(y1 - y0)
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            fill(context, x, y, 1, 1, color)
            if (x == x1 && y == y1) break
            val doubled = 2 * err
            if (doubled >= dy) {
                err += dy
                x += sx
            }
            if (doubled <= dx) {
                err += dx
                y += sy
            }
        }
    }

    fun chartHoverX(rect: UiRect, count: Int, index: Int): Int {
        if (count <= 1) return rect.x
        return rect.x + ((index / (count - 1f)) * (rect.w - 1)).toInt()
    }

    fun chartHoverY(rect: UiRect, values: List<Float>, index: Int, minOverride: Float? = null, maxOverride: Float? = null): Int {
        val value = values.getOrNull(index) ?: return rect.bottom
        val min = minOverride ?: values.min()
        val max = (maxOverride ?: values.max()).coerceAtLeast(min + 0.0001f)
        return rect.bottom - 1 - ((value - min) / (max - min) * (rect.h - 1)).toInt()
    }
}
