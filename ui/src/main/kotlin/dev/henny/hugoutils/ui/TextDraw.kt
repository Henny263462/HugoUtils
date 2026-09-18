package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer

object TextDraw {
    fun ellipsize(renderer: TextRenderer, text: String, maxWidth: Int): String {
        if (maxWidth <= 0) return ""
        if (renderer.getWidth(text) <= maxWidth) return text
        val ellipsisWidth = renderer.getWidth("…")
        if (ellipsisWidth >= maxWidth) return "…"
        val trimmed = renderer.trimToWidth(text, maxWidth - ellipsisWidth)
        return if (trimmed.isEmpty()) "…" else "$trimmed…"
    }

    fun wrap(renderer: TextRenderer, text: String, maxWidth: Int): List<String> = buildList {
        text.split('\n').forEach { paragraph ->
            var line = ""
            paragraph.split(' ').forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (line.isEmpty() || renderer.getWidth(candidate) <= maxWidth) line = candidate
                else {
                    add(line)
                    line = word
                }
            }
            if (line.isNotEmpty()) add(line)
        }
    }
}
