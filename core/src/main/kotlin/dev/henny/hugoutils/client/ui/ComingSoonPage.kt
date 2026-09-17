package dev.henny.hugoutils.client.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class ComingSoonPage(
    override val category: ConfigCategory,
    private val heading: String = category.title,
    private val message: String = "Coming soon",
    private val detail: String = "More features coming soon."
) : ConfigPage {
    override val id = "page:${category.id}"
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, height.coerceAtLeast(220))
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        val font = client.textRenderer
        UiWidgets.hoverCard(context, frame, mouseX.toDouble(), mouseY.toDouble(), key = category.id)
        val titleW = font.getWidth(heading)
        val msgW = font.getWidth(message)
        context.drawText(font, heading, frame.x + (frame.w - titleW) / 2, frame.y + frame.h / 2 - 28, HugoTheme.text, false)
        context.drawText(
            font,
            message,
            frame.x + (frame.w - msgW) / 2,
            frame.y + frame.h / 2 - 8,
            HugoTheme.accent,
            false
        )
        context.drawText(
            font,
            UiDraw.ellipsize(font, detail, frame.w - 40),
            frame.x + (frame.w - font.getWidth(detail).coerceAtMost(frame.w - 40)) / 2,
            frame.y + frame.h / 2 + 10,
            HugoTheme.textMuted,
            false
        )
    }

    override fun persist() = Unit
}
