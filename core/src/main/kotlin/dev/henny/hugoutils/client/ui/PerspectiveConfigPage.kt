package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.PerspectiveMode
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class PerspectiveConfigPage : ConfigPage {
    override val category = ConfigCategory.PERSPECTIVE
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var choices = emptyList<Pair<PerspectiveMode, UiRect>>()
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, 154)
        choices = PerspectiveMode.entries.mapIndexed { index, mode ->
            mode to UiRect(x + 10, y + 58 + index * 26, width - 20, 22)
        }
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        val font = client.textRenderer
        val selected = PerspectiveMode.fromId(ConfigManager.config.perspectiveMode)

        UiDraw.panel(context, frame, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, "F5-Reihenfolge", frame.x + 10, frame.y + 12, HugoTheme.text, false)
        context.drawText(
            font,
            "Lege fest, welche Perspektiven die F5-Taste durchschaltet.",
            frame.x + 10,
            frame.y + 30,
            HugoTheme.textMuted,
            false
        )

        for ((mode, rect) in choices) {
            val active = mode == selected
            val hovered = rect.contains(lastMouseX, lastMouseY)
            UiDraw.fill(context, rect, if (active) HugoTheme.accentSoft else if (hovered) 0x18FFFFFF else HugoTheme.inset)
            UiDraw.border(
                context,
                rect.x,
                rect.y,
                rect.w,
                rect.h,
                if (active || hovered) HugoTheme.accent else HugoTheme.cardBorder
            )
            val marker = if (active) "●" else "○"
            context.drawText(font, marker, rect.x + 7, rect.y + 7, if (active) HugoTheme.accent else HugoTheme.textDim, false)
            context.drawText(font, mode.label, rect.x + 22, rect.y + 7, HugoTheme.text, false)
            val description = UiDraw.ellipsize(font, mode.description, rect.w / 2)
            context.drawText(
                font,
                description,
                rect.right() - font.getWidth(description) - 7,
                rect.y + 7,
                HugoTheme.textMuted,
                false
            )
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        choices.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (mode, _) ->
            ConfigManager.update { it.perspectiveMode = mode.id }
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun persist() = ConfigManager.requestSave()
}
