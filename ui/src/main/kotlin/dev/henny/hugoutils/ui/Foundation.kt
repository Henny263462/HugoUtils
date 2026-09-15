package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.ColorHelper
import kotlin.math.floor

data class UiRect(val x: Int, val y: Int, val w: Int, val h: Int) {
    val right: Int get() = x + w
    val bottom: Int get() = y + h
    fun right(): Int = right
    fun bottom(): Int = bottom
    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseX < right && mouseY >= y && mouseY < bottom
}

data class Theme(
    val background: Int = 0xE80B0D12.toInt(),
    val overlay: Int = 0xB2080A10.toInt(),
    val panel: Int = 0xF210131A.toInt(),
    val panelBorder: Int = 0xFF262C38.toInt(),
    val sidebar: Int = 0xF10E1118.toInt(),
    val card: Int = 0xFF161A23.toInt(),
    val cardHover: Int = 0xFF1B2130.toInt(),
    val border: Int = 0xFF2A3140.toInt(),
    val inset: Int = 0xFF0C0F16.toInt(),
    val accent: Int = 0xFF6EE7FF.toInt(),
    val accentSoft: Int = 0x336EE7FF,
    val accentMuted: Int = 0xFF3E8EA3.toInt(),
    val text: Int = 0xFFF4F6FA.toInt(),
    val textMuted: Int = 0xFF8B93A7.toInt(),
    val textDim: Int = 0xFF5C6478.toInt(),
    val success: Int = 0xFF3DDC97.toInt(),
    val danger: Int = 0xFFFF6B6B.toInt(),
    val tooltip: Int = 0xF6121620.toInt(),
    val trackOff: Int = 0xFF2A3140.toInt(),
    val knob: Int = 0xFFF7FAFC.toInt(),
    val comingSoon: Int = 0xFF3A4152.toInt(),
    val helper: Int = 0xFF7A8299.toInt(),
    val glintPurple: Int = 0xFF9B5CFF.toInt()
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
    fun panel(context: DrawContext, rect: UiRect, fill: Int, border: Int) {
        fill(context, rect, fill)
        border(context, rect.x, rect.y, rect.w, rect.h, border)
    }
    fun panel(context: DrawContext, x: Int, y: Int, w: Int, h: Int, fill: Int, border: Int) =
        panel(context, UiRect(x, y, w, h), fill, border)

    fun ellipsize(renderer: TextRenderer, text: String, maxWidth: Int): String {
        if (maxWidth <= 0 || renderer.getWidth(text) <= maxWidth) return text
        var value = text
        while (value.isNotEmpty() && renderer.getWidth("$value…") > maxWidth) value = value.dropLast(1)
        return if (value.isEmpty()) "…" else "$value…"
    }

    fun lerp(current: Float, target: Float, speed: Float): Float =
        current + (target - current) * speed.coerceIn(0f, 1f)

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
        fill(context, rect, if (selected) t.accentSoft else if (hovered) 0x18FFFFFF else t.inset)
        border(context, rect.x, rect.y, rect.w, rect.h, if (selected) t.accent else t.border)
        val shown = ellipsize(renderer, label, rect.w - 6)
        context.drawText(renderer, shown, rect.x + (rect.w - renderer.getWidth(shown)) / 2, rect.y + 5, if (selected) t.text else t.textMuted, false)
    }

    fun helperBadge(context: DrawContext, renderer: TextRenderer, rect: UiRect, hovered: Boolean) {
        val t = theme
        fill(context, rect, if (hovered) t.accentSoft else t.inset)
        border(context, rect.x, rect.y, rect.w, rect.h, if (hovered) t.accent else t.border)
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
        panel(context, x, y, width, height, theme.tooltip, theme.accentMuted)
        fill(context, x, y, 2, height, theme.accent)
        lines.forEachIndexed { index, line ->
            context.drawText(renderer, line, x + 6, y + 6 + index * 10, theme.text, false)
        }
    }

    fun scrollbar(context: DrawContext, viewport: UiRect, scroll: Int, maxScroll: Int) {
        if (maxScroll <= 0) return
        val thumbH = (viewport.h * viewport.h / (viewport.h + maxScroll)).coerceIn(14, viewport.h)
        val thumbY = viewport.y + ((viewport.h - thumbH) * scroll / maxScroll.toFloat()).toInt()
        fill(context, viewport.right + 5, viewport.y, 3, viewport.h, theme.inset)
        fill(context, viewport.right + 5, thumbY, 3, thumbH, theme.accentMuted)
    }
}
