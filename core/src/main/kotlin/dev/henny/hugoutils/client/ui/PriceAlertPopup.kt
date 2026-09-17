package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.PriceAlert
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.TextFieldLogic
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW

class PriceAlertPopup(
    private val itemId: String,
    private val itemName: String,
    private val minecraftId: String?,
    private val suggested: Double?
) : ConfigPopup {
    private val client = MinecraftClient.getInstance()
    private val field = TextFieldLogic(maxLength = 24).apply {
        suggested?.let { setText(it.toLong().toString()) }
    }
    private var frame = UiRect(0, 0, 0, 0)
    private var input = UiRect(0, 0, 0, 0)
    private var below = UiRect(0, 0, 0, 0)
    private var above = UiRect(0, 0, 0, 0)
    private var confirm = UiRect(0, 0, 0, 0)
    private var cancel = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)
    private var focused = true
    private var wantAbove = false
    private var caret = 0
    private var error: String? = null

    override fun layout(screenWidth: Int, screenHeight: Int) {
        frame = UiRect((screenWidth - 300) / 2, (screenHeight - 148) / 2, 300, 148)
        input = UiRect(frame.x + 16, frame.y + 52, frame.w - 32, 22)
        below = UiRect(frame.x + 16, input.bottom() + 8, 126, 20)
        above = UiRect(below.right() + 8, below.y, 134, 20)
        cancel = UiRect(frame.right() - 168, frame.bottom() - 36, 70, 20)
        confirm = UiRect(frame.right() - 90, frame.bottom() - 36, 74, 20)
        close = UiRect(frame.right() - 24, frame.y + 10, 14, 14)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        caret++
        val font = client.textRenderer
        UiDraw.fill(context, 0, 0, client.window.scaledWidth, client.window.scaledHeight, 0xB0000000.toInt())
        UiDraw.shadow(context, frame)
        UiDraw.panel(context, frame, HugoTheme.panel, HugoTheme.panelBorder)
        context.drawText(font, "Preiswecker", frame.x + 16, frame.y + 14, HugoTheme.text, false)
        context.drawText(font, "×", close.x + 3, close.y + 2, HugoTheme.textMuted, false)
        context.drawText(
            font,
            UiDraw.ellipsize(font, itemName, frame.w - 32),
            frame.x + 16,
            frame.y + 32,
            HugoTheme.textMuted,
            false
        )
        UiDraw.panel(context, input, HugoTheme.inset, if (focused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = if (field.text.isEmpty()) "Preis, z. B. 1500000" else field.text
        context.drawText(
            font,
            UiDraw.ellipsize(font, shown, input.w - 12),
            input.x + 6,
            input.y + 7,
            if (field.text.isEmpty()) HugoTheme.textDim else HugoTheme.text,
            false
        )
        if (focused && field.text.isNotEmpty() && (caret / 10) % 2 == 0) {
            UiDraw.fill(context, input.x + 6 + font.getWidth(field.text).coerceAtMost(input.w - 14), input.y + 5, 1, input.h - 10, HugoTheme.accent)
        }
        drawChip(context, below, "Bei oder unter", !wantAbove, mouseX, mouseY)
        drawChip(context, above, "Bei oder über", wantAbove, mouseX, mouseY)
        error?.let {
            context.drawText(font, it, frame.x + 16, frame.bottom() - 52, HugoTheme.danger, false)
        }
        UiWidgets.button(context, font, cancel, "Abbrechen", mouseX.toDouble(), mouseY.toDouble(), true, ButtonStyle.GHOST)
        UiWidgets.button(context, font, confirm, "Setzen", mouseX.toDouble(), mouseY.toDouble(), true, ButtonStyle.PRIMARY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (close.contains(mouseX, mouseY) || !frame.contains(mouseX, mouseY) || cancel.contains(mouseX, mouseY)) {
            PopupManager.close()
            return true
        }
        if (below.contains(mouseX, mouseY)) {
            wantAbove = false
            return true
        }
        if (above.contains(mouseX, mouseY)) {
            wantAbove = true
            return true
        }
        focused = input.contains(mouseX, mouseY)
        if (confirm.contains(mouseX, mouseY)) {
            submit()
            return true
        }
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            PopupManager.close()
            return true
        }
        if (!focused) return true
        if (input.isPaste) {
            field.setText(client.keyboard.clipboard.take(24))
            return true
        }
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                field.backspace()
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                submit()
                true
            }
            else -> false
        }
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!focused || !input.isValidChar) return true
        val ch = input.asString().firstOrNull() ?: return true
        if (ch.isDigit() || ch == '.' || ch == ',' || ch == 'k' || ch == 'K' || ch == 'm' || ch == 'M') {
            field.insert(ch.toString())
        }
        return true
    }

    private fun submit() {
        val target = parsePrice(field.text) ?: run {
            error = "Ungültiger Preis."
            return
        }
        ConfigManager.upsertPriceAlert(
            PriceAlert().apply {
                itemId = this@PriceAlertPopup.itemId
                name = itemName
                minecraftId = this@PriceAlertPopup.minecraftId.orEmpty()
                this.target = target
                above = wantAbove
                source = "any"
                fired = false
            }
        )
        PopupManager.close()
    }

    private fun drawChip(context: DrawContext, rect: UiRect, label: String, selected: Boolean, mouseX: Int, mouseY: Int) {
        UiWidgets.button(
            context,
            client.textRenderer,
            rect,
            label,
            mouseX.toDouble(),
            mouseY.toDouble(),
            true,
            if (selected) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY
        )
    }

    companion object {
        fun parsePrice(raw: String): Double? {
            val trimmed = raw.trim().replace(" ", "").replace("$", "")
            if (trimmed.isEmpty()) return null
            val multiplier = when {
                trimmed.endsWith("m", true) -> 1_000_000.0
                trimmed.endsWith("k", true) -> 1_000.0
                else -> 1.0
            }
            val number = trimmed.dropLast(if (multiplier == 1.0) 0 else 1)
            val parsed = when {
                number.count { it == '.' } > 1 && !number.contains(',') -> number.replace(".", "").toDoubleOrNull()
                number.contains('.') && number.contains(',') -> number.replace(".", "").replace(',', '.').toDoubleOrNull()
                else -> number.replace(',', '.').toDoubleOrNull()
            } ?: return null
            return parsed * multiplier
        }
    }
}
