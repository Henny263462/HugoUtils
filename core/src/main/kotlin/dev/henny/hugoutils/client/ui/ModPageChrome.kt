package dev.henny.hugoutils.client.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

object ModPageChrome {
    fun hero(
        context: DrawContext,
        font: TextRenderer,
        rect: UiRect,
        title: String,
        detail: String,
        enabled: Boolean?,
        mouseX: Double,
        mouseY: Double,
        toggle: UiRect? = null,
        swatch: Int? = null,
        key: String = title
    ) {
        UiWidgets.hoverCard(context, rect, mouseX, mouseY, key = key, selected = enabled == true)
        var textX = rect.x + 12
        swatch?.let { color ->
            UiDraw.panel(context, UiRect(textX, rect.y + 12, 16, 16), color, HugoTheme.cardBorder)
            textX += 24
        }
        context.drawText(font, title, textX, rect.y + 10, HugoTheme.text, false)
        context.drawText(
            font,
            UiDraw.ellipsize(font, detail, rect.w - (if (toggle != null) 70 else 28) - (if (swatch != null) 24 else 0)),
            textX,
            rect.y + 26,
            HugoTheme.textMuted,
            false
        )
        if (enabled != null) {
            val badge = if (enabled) "Aktiv" else "Aus"
            context.drawText(
                font,
                badge,
                textX,
                rect.y + 40,
                if (enabled) HugoTheme.success else HugoTheme.textDim,
                false
            )
        }
        if (toggle != null && enabled != null) {
            UiWidgets.toggle(context, toggle, enabled, mouseX, mouseY, key = "$key-toggle")
        }
    }

    fun stat(
        context: DrawContext,
        font: TextRenderer,
        rect: UiRect,
        label: String,
        value: String,
        mouseX: Double,
        mouseY: Double
    ) {
        UiWidgets.hoverCard(context, rect, mouseX, mouseY, key = "stat-$label")
        context.drawText(font, label, rect.x + 10, rect.y + 8, HugoTheme.textDim, false)
        context.drawText(font, UiDraw.ellipsize(font, value, rect.w - 20), rect.x + 10, rect.y + 24, HugoTheme.text, false)
    }

    fun option(
        context: DrawContext,
        font: TextRenderer,
        rect: UiRect,
        title: String,
        detail: String,
        selected: Boolean,
        mouseX: Double,
        mouseY: Double,
        toggle: UiRect? = null,
        enabled: Boolean? = null
    ) {
        UiWidgets.hoverCard(context, rect, mouseX, mouseY, key = title, selected = selected)
        context.drawText(font, title, rect.x + 10, rect.y + 10, HugoTheme.text, false)
        context.drawText(
            font,
            UiDraw.ellipsize(font, detail, rect.w - if (toggle != null) 56 else 20),
            rect.x + 10,
            rect.y + 26,
            HugoTheme.textMuted,
            false
        )
        if (toggle != null && enabled != null) {
            UiWidgets.toggle(context, toggle, enabled, mouseX, mouseY, key = "$title-toggle")
        }
    }
}
