package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.api.ApiConfig
import dev.henny.hugoutils.api.AuthApiClient
import dev.henny.hugoutils.ui.TextFieldLogic
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW

class SignInConfigPage : ConfigPage {
    override val category = ConfigCategory.SIGN_IN
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var openButton = UiRect(0, 0, 0, 0)
    private var codeInput = UiRect(0, 0, 0, 0)
    private var loginButton = UiRect(0, 0, 0, 0)
    private val code = TextFieldLogic(maxLength = CODE_LENGTH)
    private var focused = false
    private var status = "Öffne zuerst die Anmeldeseite und übernimm den sechsstelligen Code."
    private var statusError = false
    private var working = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var caretTicks = 0

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, 176)
        openButton = UiRect(x + 10, y + 48, width - 20, 22)
        codeInput = UiRect(x + 10, y + 96, width - 20 - 92, 24)
        loginButton = UiRect(codeInput.right() + 6, y + 96, 86, 24)
        return frame.h
    }

    override fun resetUi() {
        focused = false
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        caretTicks++
        val font = client.textRenderer

        UiDraw.panel(context, frame, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, "Mit HugoBot verbinden", frame.x + 10, frame.y + 12, HugoTheme.text, false)
        context.drawText(font, "Bestätige dein Minecraft-Konto direkt aus der Mod.", frame.x + 10, frame.y + 28, HugoTheme.textMuted, false)
        drawButton(context, openButton, "Anmeldeseite öffnen", true)
        context.drawText(font, "Login-Code", frame.x + 10, frame.y + 82, HugoTheme.textMuted, false)
        drawInput(context)
        drawButton(context, loginButton, if (working) "Prüfe…" else "Sign-in", canSubmit())
        context.drawText(
            font,
            UiDraw.ellipsize(font, status, frame.w - 20),
            frame.x + 10,
            frame.y + 138,
            if (statusError) HugoTheme.danger else HugoTheme.textDim,
            false
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (openButton.contains(mouseX, mouseY)) {
            Util.getOperatingSystem().open(ApiConfig.PUBLIC_LOGIN_URL)
            status = "Anmeldeseite geöffnet. Gib den dort angezeigten Code hier ein."
            statusError = false
            return true
        }
        if (codeInput.contains(mouseX, mouseY)) {
            focused = true
            return true
        }
        focused = false
        if (loginButton.contains(mouseX, mouseY) && canSubmit()) {
            submit()
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (!focused) return false
        if (input.isPaste) {
            code.setText(normalizeCode(client.keyboard.clipboard))
            status = if (code.text.length == CODE_LENGTH) "Code eingefügt – bereit zur Anmeldung." else
                "Zwischenablage enthält keinen vollständigen Login-Code."
            statusError = code.text.length != CODE_LENGTH
            return true
        }
        if (input.isCopy) {
            if (code.text.isNotEmpty()) client.keyboard.clipboard = code.text
            return true
        }
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                code.backspace()
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (canSubmit()) submit()
                true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                focused = false
                true
            }
            else -> false
        }
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!focused || !input.isValidChar || code.text.length >= CODE_LENGTH) return false
        val value = input.asString().uppercase()
        if (value.length != 1 || value[0] !in CODE_CHARS) return false
        code.insert(value)
        return true
    }

    override fun persist() = Unit

    private fun submit() {
        working = true
        focused = false
        status = "Minecraft-Konto wird bestätigt …"
        statusError = false
        AuthApiClient.login(code.text) { message, error ->
            working = false
            status = message.removePrefix("HugoUtils: ")
            statusError = error
            if (!error) code.setText("")
        }
    }

    private fun canSubmit(): Boolean = !working && code.text.length == CODE_LENGTH

    private fun normalizeCode(value: String): String =
        value.uppercase().filter { it in CODE_CHARS }.take(CODE_LENGTH)

    private fun drawInput(context: DrawContext) {
        val font = client.textRenderer
        val hovered = codeInput.contains(lastMouseX, lastMouseY)
        UiDraw.panel(
            context,
            codeInput,
            HugoTheme.inset,
            if (focused) HugoTheme.accent else if (hovered) HugoTheme.accentMuted else HugoTheme.cardBorder
        )
        val shown = if (code.text.isEmpty()) "ABC123" else code.text
        val color = if (code.text.isEmpty()) HugoTheme.textDim else HugoTheme.text
        context.drawText(font, shown, codeInput.x + 7, codeInput.y + 8, color, false)
        if (focused && code.text.isNotEmpty() && (caretTicks / 10) % 2 == 0) {
            val caretX = codeInput.x + 7 + font.getWidth(code.text)
            UiDraw.fill(context, caretX, codeInput.y + 5, 1, codeInput.h - 10, HugoTheme.accent)
        }
    }

    private fun drawButton(context: DrawContext, rect: UiRect, label: String, enabled: Boolean) {
        val hovered = enabled && rect.contains(lastMouseX, lastMouseY)
        UiDraw.fill(context, rect, if (hovered) HugoTheme.accentSoft else HugoTheme.inset)
        UiDraw.border(
            context,
            rect.x,
            rect.y,
            rect.w,
            rect.h,
            if (hovered) HugoTheme.accent else HugoTheme.cardBorder
        )
        val font = client.textRenderer
        val shown = UiDraw.ellipsize(font, label, rect.w - 8)
        context.drawText(
            font,
            shown,
            rect.x + (rect.w - font.getWidth(shown)) / 2,
            rect.y + 7,
            if (enabled) HugoTheme.text else HugoTheme.textDim,
            false
        )
    }

    companion object {
        private const val CODE_LENGTH = 6
        private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
