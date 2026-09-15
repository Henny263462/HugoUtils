package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.gui.capture.GuiCaptureController
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class GuiCaptureConfigPage : ConfigPage {
    override val category = ConfigCategory.TOOLS
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, 154)
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        val font = client.textRenderer
        UiDraw.panel(context, frame, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, "GUI-Wissenslayer", frame.x + 10, frame.y + 12, HugoTheme.text, false)
        val lines = listOf(
            "1. Öffne ein Container-GUI und drücke F8.",
            "2. Trage die stabile screenId und die Screen-Metadaten ein.",
            "3. Klicke Slots an und beschreibe Rolle sowie Klick-Aktionen.",
            "4. „Export“ schreibt capture.json und index.json lokal.",
            "",
            "Pfad: config/hugoutils/gui-knowledge/{screenId}/",
            GuiCaptureController.statusText()
        )
        lines.forEachIndexed { index, line ->
            context.drawText(
                font,
                UiDraw.ellipsize(font, line, frame.w - 20),
                frame.x + 10,
                frame.y + 34 + index * 16,
                if (index >= 5) HugoTheme.accent else HugoTheme.textMuted,
                false
            )
        }
    }

    override fun persist() = Unit
}
