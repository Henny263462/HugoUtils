package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.TextFieldLogic
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW

class FeedbackComposePopup(
    private val onFinished: (ok: Boolean) -> Unit = {}
) : ConfigPopup {
    private val client = MinecraftClient.getInstance()
    private val titleField = TextFieldLogic(maxLength = 120)
    private val bodyField = TextFieldLogic(maxLength = 4000)
    private var frame = UiRect(0, 0, 0, 0)
    private var titleBox = UiRect(0, 0, 0, 0)
    private var bodyBox = UiRect(0, 0, 0, 0)
    private var confirm = UiRect(0, 0, 0, 0)
    private var cancel = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)
    private var kindHits = emptyList<Pair<String, UiRect>>()
    private var kind = "issue"
    private var focus = Focus.TITLE
    private var working = false
    private var status = "Feature, Issue oder Anmerkung. Titel 4–120, Text 8–4000."
    private var statusError = false
    private var caret = 0

    override fun layout(screenWidth: Int, screenHeight: Int) {
        frame = UiRect((screenWidth - 340) / 2, (screenHeight - 196) / 2, 340, 196)
        val kinds = listOf("feature" to "Feature", "issue" to "Issue", "note" to "Anmerkung")
        val chipW = (frame.w - 40 - 12) / 3
        kindHits = kinds.mapIndexed { index, (id, _) ->
            id to UiRect(frame.x + 16 + index * (chipW + 6), frame.y + 36, chipW, 18)
        }
        titleBox = UiRect(frame.x + 16, frame.y + 62, frame.w - 32, 20)
        bodyBox = UiRect(frame.x + 16, titleBox.bottom() + 8, frame.w - 32, 44)
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
        context.drawText(font, "Neue Meldung", frame.x + 16, frame.y + 14, HugoTheme.text, false)
        context.drawText(font, "×", close.x + 3, close.y + 2, HugoTheme.textMuted, false)
        kindHits.forEach { (id, rect) ->
            val label = when (id) {
                "feature" -> "Feature"
                "issue" -> "Issue"
                else -> "Anmerkung"
            }
            UiWidgets.button(
                context, font, rect, label, mouseX.toDouble(), mouseY.toDouble(),
                !working, if (kind == id) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY, key = "kind-$id"
            )
        }
        drawField(context, titleBox, titleField, "Titel (mind. 4)", focus == Focus.TITLE)
        drawField(context, bodyBox, bodyField, "Text (mind. 8)", focus == Focus.BODY)
        context.drawText(
            font,
            UiDraw.ellipsize(font, status, frame.w - 32),
            frame.x + 16,
            frame.bottom() - 54,
            if (statusError) HugoTheme.danger else HugoTheme.textDim,
            false
        )
        UiWidgets.button(context, font, cancel, "Abbrechen", mouseX.toDouble(), mouseY.toDouble(), true, ButtonStyle.GHOST)
        UiWidgets.button(context, font, confirm, if (working) "…" else "Senden", mouseX.toDouble(), mouseY.toDouble(), !working, ButtonStyle.PRIMARY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (close.contains(mouseX, mouseY) || !frame.contains(mouseX, mouseY) || cancel.contains(mouseX, mouseY)) {
            PopupManager.close()
            return true
        }
        kindHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let {
            kind = it.first
            return true
        }
        focus = when {
            titleBox.contains(mouseX, mouseY) -> Focus.TITLE
            bodyBox.contains(mouseX, mouseY) -> Focus.BODY
            else -> focus
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
        val field = activeField()
        if (input.isPaste) {
            field.setText((field.text + client.keyboard.clipboard).take(fieldMax()))
            return true
        }
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                field.backspace()
                true
            }
            GLFW.GLFW_KEY_TAB -> {
                focus = if (focus == Focus.TITLE) Focus.BODY else Focus.TITLE
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (focus == Focus.TITLE) focus = Focus.BODY else submit()
                true
            }
            else -> true
        }
    }

    override fun charTyped(input: CharInput): Boolean {
        if (working || !input.isValidChar) return true
        activeField().insert(input.asString())
        return true
    }

    private fun submit() {
        val title = titleField.text.trim()
        val body = bodyField.text.trim()
        if (title.length < 4) {
            status = "Titel braucht mindestens 4 Zeichen."
            statusError = true
            return
        }
        if (body.length < 8) {
            status = "Text braucht mindestens 8 Zeichen."
            statusError = true
            return
        }
        working = true
        status = "Wird gesendet …"
        statusError = false
        ClientJobs.submit({ message, error ->
            working = false
            status = message
            statusError = error
            if (!error) {
                onFinished(true)
                PopupManager.close()
            }
        }) {
            ClientApi.sendFeedback(kind, title, body)
            "Meldung gesendet."
        }
    }

    private fun drawField(context: DrawContext, rect: UiRect, field: TextFieldLogic, placeholder: String, focused: Boolean) {
        val font = client.textRenderer
        UiDraw.panel(context, rect, HugoTheme.inset, if (focused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = field.text.ifEmpty { placeholder }
        context.drawText(
            font,
            UiDraw.ellipsize(font, shown, rect.w - 12),
            rect.x + 6,
            rect.y + 6,
            if (field.text.isEmpty()) HugoTheme.textDim else HugoTheme.text,
            false
        )
        if (focused && field.text.isNotEmpty() && (caret / 10) % 2 == 0) {
            UiDraw.fill(context, rect.x + 6 + font.getWidth(field.text).coerceAtMost(rect.w - 14), rect.y + 4, 1, 12, HugoTheme.accent)
        }
    }

    private fun activeField(): TextFieldLogic = if (focus == Focus.TITLE) titleField else bodyField
    private fun fieldMax(): Int = if (focus == Focus.TITLE) 120 else 4000

    private enum class Focus { TITLE, BODY }
}
