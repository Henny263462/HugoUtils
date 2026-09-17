package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.api.AuthApiClient
import dev.henny.hugoutils.api.ClientAuth
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW

class WebsiteCodePopup(
    private val onFinished: (ok: Boolean) -> Unit = {}
) : ConfigPopup {
    private val client = MinecraftClient.getInstance()
    private val digits = CharArray(ClientAuth.CODE_LENGTH)
    private var filled = 0
    private var caret = 0
    private var blink = 0
    private var frame = UiRect(0, 0, 0, 0)
    private var boxes = emptyList<UiRect>()
    private var confirm = UiRect(0, 0, 0, 0)
    private var cancel = UiRect(0, 0, 0, 0)
    private var paste = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)
    private var status = "6 Zeichen von hugo.henny.dev, ohne I, O, 0 und 1."
    private var statusError = false
    private var working = false
    private var pendingToken: String? = null

    override fun layout(screenWidth: Int, screenHeight: Int) {
        val width = 340
        val height = 186
        frame = UiRect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height)
        val box = 30
        val gap = 6
        val groupGap = 12
        val rowW = ClientAuth.CODE_LENGTH * box + (ClientAuth.CODE_LENGTH - 2) * gap + groupGap
        val startX = frame.x + (frame.w - rowW) / 2
        boxes = (0 until ClientAuth.CODE_LENGTH).map { index ->
            val extra = if (index >= ClientAuth.CODE_LENGTH / 2) groupGap - gap else 0
            UiRect(startX + index * (box + gap) + extra, frame.y + 52, box, 36)
        }
        paste = UiRect(frame.x + 16, frame.bottom - 36, 84, 22)
        cancel = UiRect(frame.right - 176, frame.bottom - 36, 74, 22)
        confirm = UiRect(frame.right - 94, frame.bottom - 36, 78, 22)
        close = UiRect(frame.right - 24, frame.y + 10, 14, 14)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        blink++
        val font = client.textRenderer
        UiDraw.fill(context, 0, 0, client.window.scaledWidth, client.window.scaledHeight, 0xB0000000.toInt())
        UiDraw.shadow(context, frame)
        UiDraw.panel(context, frame, HugoTheme.panel, HugoTheme.panelBorder)
        context.drawText(font, "Website anmelden", frame.x + 16, frame.y + 10, HugoTheme.text, false)
        context.drawText(font, "×", close.x + 3, close.y + 2, HugoTheme.textMuted, false)
        context.drawText(
            font,
            "Ohne Mod: /msg <Bot> AB23CD",
            frame.x + 16,
            frame.y + 24,
            HugoTheme.textDim,
            false
        )
        val token = pendingToken
        if (token != null) {
            UiDraw.panel(context, UiRect(frame.x + 16, frame.y + 48, frame.w - 32, 36), HugoTheme.inset, HugoTheme.accent)
            context.drawText(
                font,
                UiDraw.ellipsize(font, "Token  ${token.take(18)}…", frame.w - 48),
                frame.x + 24,
                frame.y + 60,
                HugoTheme.text,
                false
            )
        } else {
            boxes.forEachIndexed { index, rect ->
                val focused = index == caret.coerceIn(0, ClientAuth.CODE_LENGTH - 1)
                val hasValue = index < filled
                UiDraw.panel(
                    context,
                    rect,
                    if (focused) HugoTheme.accentSoft else HugoTheme.inset,
                    if (focused) HugoTheme.accent else HugoTheme.cardBorder
                )
                if (hasValue) {
                    val glyph = digits[index].toString()
                    context.drawText(
                        font,
                        glyph,
                        rect.x + (rect.w - font.getWidth(glyph)) / 2,
                        rect.y + 14,
                        HugoTheme.text,
                        false
                    )
                } else if (focused && (blink / 8) % 2 == 0) {
                    UiDraw.fill(context, rect.x + rect.w / 2, rect.y + 10, 1, rect.h - 20, HugoTheme.accent)
                }
            }
        }
        context.drawText(
            font,
            UiDraw.ellipsize(font, status, frame.w - 32),
            frame.x + 16,
            frame.bottom - 58,
            if (statusError) HugoTheme.danger else HugoTheme.textDim,
            false
        )
        drawButton(context, paste, "Einfügen", mouseX, mouseY, !working)
        drawButton(context, cancel, "Abbrechen", mouseX, mouseY, true)
        drawButton(context, confirm, if (working) "…" else "Anmelden", mouseX, mouseY, !working && canSubmit())
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (close.contains(mouseX, mouseY) || !frame.contains(mouseX, mouseY)) {
            PopupManager.close()
            return true
        }
        boxes.forEachIndexed { index, rect ->
            if (rect.contains(mouseX, mouseY)) {
                pendingToken = null
                caret = index.coerceAtMost(filled)
                return true
            }
        }
        if (paste.contains(mouseX, mouseY) && !working) {
            applyPaste(client.keyboard.clipboard)
            return true
        }
        if (cancel.contains(mouseX, mouseY)) {
            PopupManager.close()
            return true
        }
        if (confirm.contains(mouseX, mouseY) && !working) {
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
        if (working) return true
        if (isPaste(input)) {
            applyPaste(client.keyboard.clipboard)
            return true
        }
        when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                pendingToken = null
                if (filled > 0) {
                    filled--
                    caret = filled
                    digits[filled] = 0.toChar()
                }
                return true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                submit()
                return true
            }
            GLFW.GLFW_KEY_LEFT -> {
                caret = (caret - 1).coerceAtLeast(0)
                return true
            }
            GLFW.GLFW_KEY_RIGHT -> {
                caret = (caret + 1).coerceAtMost(filled.coerceAtMost(ClientAuth.CODE_LENGTH - 1))
                return true
            }
        }
        return true
    }

    override fun charTyped(input: CharInput): Boolean {
        if (working || !input.isValidChar) return true
        val ch = input.asString().firstOrNull() ?: return true
        if (!ClientAuth.isWebsiteCodeChar(ch)) return true
        pendingToken = null
        if (filled >= ClientAuth.CODE_LENGTH) return true
        digits[filled] = ch.uppercaseChar()
        filled++
        caret = filled.coerceAtMost(ClientAuth.CODE_LENGTH - 1)
        return true
    }

    private fun applyPaste(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            status = "Zwischenablage ist leer."
            statusError = true
            return
        }
        if (ClientAuth.isClientToken(trimmed)) {
            pendingToken = trimmed
            filled = 0
            status = "Token erkannt. Anmelden klicken."
            statusError = false
            return
        }
        pendingToken = null
        val normalized = ClientAuth.normalizeLoginCode(trimmed)
        filled = 0
        normalized.forEachIndexed { index, ch ->
            digits[index] = ch
            filled++
        }
        caret = filled.coerceAtMost(ClientAuth.CODE_LENGTH - 1)
        status = if (filled == ClientAuth.CODE_LENGTH) {
            "Code eingefügt. Anmelden klicken."
        } else {
            "Code eingefügt. ${ClientAuth.CODE_LENGTH - filled} Zeichen fehlen."
        }
        statusError = filled == 0
    }

    private fun canSubmit(): Boolean =
        pendingToken != null || filled == ClientAuth.CODE_LENGTH

    private fun submit() {
        val token = pendingToken
        if (token != null) {
            submitRaw(token)
            return
        }
        if (filled != ClientAuth.CODE_LENGTH) {
            status = "Bitte ${ClientAuth.CODE_LENGTH} Zeichen aus ABCDEFGHJKLMNPQRSTUVWXYZ23456789."
            statusError = true
            return
        }
        submitRaw(String(digits.copyOf(filled)))
    }

    private fun submitRaw(code: String) {
        if (working) return
        working = true
        status = "Code wird geprüft …"
        statusError = false
        AuthApiClient.loginWithCode(code) { message, error ->
            working = false
            status = message
            statusError = error
            if (!error) {
                onFinished(true)
                PopupManager.close()
            }
        }
    }

    private fun isPaste(input: KeyInput): Boolean =
        input.isPaste || (input.key() == GLFW.GLFW_KEY_V && input.modifiers() and GLFW.GLFW_MOD_CONTROL != 0)

    private fun drawButton(context: DrawContext, rect: UiRect, label: String, mouseX: Int, mouseY: Int, enabled: Boolean) {
        UiWidgets.button(context, client.textRenderer, rect, label, mouseX.toDouble(), mouseY.toDouble(), enabled)
    }
}
