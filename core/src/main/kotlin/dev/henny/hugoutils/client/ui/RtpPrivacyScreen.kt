package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.rtp.RtpShare
import dev.henny.hugoutils.ui.Button
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.UiDraw
import dev.henny.hugoutils.ui.UiRect
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class RtpPrivacyScreen : Screen(Text.literal("RTP-Karte")) {
    private var frame = UiRect(0, 0, 0, 0)
    private val shareButton = Button("Teilen · 10% Rabatt", { RtpShare.choose(true) }, style = ButtonStyle.PRIMARY)
    private val privacyButton = Button("Privacy-Modus", { RtpShare.choose(false) }, style = ButtonStyle.GHOST)

    override fun shouldPause(): Boolean = false

    override fun shouldCloseOnEsc(): Boolean = true

    override fun close() {
        RtpShare.deferPrompt()
    }

    override fun init() {
        super.init()
        layoutButtons()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        layoutButtons()
        super.render(context, mouseX, mouseY, deltaTicks)
        val font = textRenderer
        UiDraw.fill(context, 0, 0, width, height, 0xB0000000.toInt())
        UiDraw.shadow(context, frame)
        UiDraw.panel(context, frame, UiDraw.theme.panel, UiDraw.theme.panelBorder)
        context.drawText(font, "RTP-Punkte teilen?", frame.x + 16, frame.y + 14, UiDraw.theme.text, false)
        wrap(
            "Die Mod speichert nur Random-Teleport-Ziele (Dimension, Biom, Koordinaten) für eine spätere Karte. Teilen bringt 10% Rabatt auf Premium. Privacy speichert nichts.",
            frame.w - 32
        ).forEachIndexed { index, line ->
            context.drawText(font, line, frame.x + 16, frame.y + 36 + index * 12, UiDraw.theme.textMuted, false)
        }
        shareButton.render(context, font, mouseX, mouseY)
        privacyButton.render(context, font, mouseX, mouseY)
        PopupManager.active?.let { popup ->
            popup.layout(width, height)
            popup.render(context, mouseX, mouseY)
        }
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val x = click.x()
        val y = click.y()
        PopupManager.active?.let { return it.mouseClicked(x, y) }
        if (shareButton.mouseClicked(x, y) || privacyButton.mouseClicked(x, y)) return true
        return true
    }

    private fun layoutButtons() {
        val width = 360
        val height = 168
        frame = UiRect((this.width - width) / 2, (this.height - height) / 2, width, height)
        shareButton.bounds = UiRect(frame.x + 16, frame.bottom - 36, 168, 22)
        privacyButton.bounds = UiRect(frame.right - 154, frame.bottom - 36, 138, 22)
    }

    private fun wrap(text: String, maxWidth: Int): List<String> {
        val font = textRenderer
        val words = text.split(" ")
        val lines = ArrayList<String>()
        var current = ""
        for (word in words) {
            val next = if (current.isEmpty()) word else "$current $word"
            if (font.getWidth(next) <= maxWidth) {
                current = next
            } else {
                if (current.isNotEmpty()) lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines.take(6)
    }
}
