package dev.henny.hugoutils.client.ui

import com.google.gson.JsonObject
import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.api.ClientSessionStore
import dev.henny.hugoutils.api.JsonView
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.TextFieldLogic
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW

class AfkBotConfigPage : ConfigPage {
    override val category = ConfigCategory.AFK_BOT
    private val client = MinecraftClient.getInstance()
    private val command = TextFieldLogic(maxLength = 80)
    private var frame = UiRect(0, 0, 0, 0)
    private var start = UiRect(0, 0, 0, 0)
    private var stop = UiRect(0, 0, 0, 0)
    private var addAccount = UiRect(0, 0, 0, 0)
    private var link = UiRect(0, 0, 0, 0)
    private var send = UiRect(0, 0, 0, 0)
    private var input = UiRect(0, 0, 0, 0)
    private var listFrame = UiRect(0, 0, 0, 0)
    private var logFrame = UiRect(0, 0, 0, 0)
    private var accounts = emptyList<JsonObject>()
    private var selected: JsonObject? = null
    private var logs = emptyList<String>()
    private var liveSeries = emptyList<Float>()
    private var liveLabel = "Offline"
    private var status = "AFK-Bots steuern, sobald du angemeldet bist."
    private var statusError = false
    private var working = false
    private var focused = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var caret = 0
    private var lastLiveAt = 0L
    private var hoveredTip: String? = null

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val h = height.coerceAtLeast(300)
        frame = UiRect(x, y, width, h)
        val buttonW = ((width - 20 - 18) / 4).coerceAtLeast(54)
        start = UiRect(x + 10, y + 48, buttonW, 20)
        stop = UiRect(start.right() + 6, y + 48, buttonW, 20)
        addAccount = UiRect(stop.right() + 6, y + 48, buttonW, 20)
        link = UiRect(addAccount.right() + 6, y + 48, width - 20 - 3 * (buttonW + 6), 20)
        listFrame = UiRect(x + 10, y + 76, width - 20, 8 * 16)
        logFrame = UiRect(x + 10, listFrame.bottom() + 8, width - 20, 10 * 11 + 8)
        input = UiRect(x + 10, logFrame.bottom() + 8, width - 20 - 78, 20)
        send = UiRect(input.right() + 6, input.y, 72, 20)
        return send.bottom() + 8 - y
    }

    override fun resetUi() {
        focused = false
        if (ClientSessionStore.hasToken()) refresh(true)
    }

    override fun hoveredTooltip(): String? = hoveredTip

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        caret++
        hoveredTip = null
        if (ClientSessionStore.hasToken() && !working && System.currentTimeMillis() - lastLiveAt > 5_000L) {
            refresh(true)
        }
        val font = client.textRenderer
        UiDraw.panel(context, frame, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, "AFK Bot", frame.x + 10, frame.y + 10, HugoTheme.text, false)
        context.drawText(
            font,
            liveLabel,
            frame.right() - font.getWidth(liveLabel) - 10,
            frame.y + 10,
            if (liveLabel.equals("Live", true) || liveLabel.equals("Online", true)) HugoTheme.success else HugoTheme.textDim,
            false
        )
        context.drawText(
            font,
            UiDraw.ellipsize(font, status, frame.w - 20),
            frame.x + 10,
            frame.y + 24,
            if (statusError) HugoTheme.danger else HugoTheme.textDim,
            false
        )
        drawButton(context, start, "Start", !working)
        drawButton(context, stop, "Stop", !working, ButtonStyle.DANGER)
        drawButton(context, addAccount, "Slot +", !working)
        drawButton(context, link, "Link", !working)
        val accountRows = accounts.take(8).mapIndexed { index, obj ->
            obj to UiRect(listFrame.x, listFrame.y + index * 16, listFrame.w, 15)
        }
        accountRows.forEach { (obj, rect) ->
            val label = JsonView.str(obj, "name", "playerName", "label", "id") ?: JsonView.label(obj)
            val detail = JsonView.str(obj, "status", "state", "online")
            UiWidgets.listRow(context, font, rect, label, obj === selected, mouseX.toDouble(), mouseY.toDouble(), detail)
            if (rect.contains(lastMouseX, lastMouseY)) {
                hoveredTip = JsonView.lines(obj, 8).joinToString("\n").ifBlank { label }
            }
        }
        if (accountRows.isEmpty()) {
            context.drawText(font, "Keine AFK-Slots geladen.", listFrame.x + 4, listFrame.y + 6, HugoTheme.textDim, false)
        }
        UiDraw.panel(context, logFrame, HugoTheme.inset, HugoTheme.cardBorder)
        if (liveSeries.size >= 2) {
            UiDraw.chart(context, UiRect(logFrame.x + 4, logFrame.y + 4, logFrame.w - 8, 28), liveSeries, HugoTheme.accent)
        }
        val logTop = if (liveSeries.size >= 2) logFrame.y + 36 else logFrame.y + 4
        logs.takeLast(8).forEachIndexed { index, line ->
            context.drawText(
                font,
                UiDraw.ellipsize(font, line, logFrame.w - 10),
                logFrame.x + 5,
                logTop + index * 11,
                HugoTheme.text,
                false
            )
        }
        UiDraw.panel(context, input, HugoTheme.inset, if (focused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = command.text.ifEmpty { "Live-Befehl …" }
        context.drawText(
            font,
            UiDraw.ellipsize(font, shown, input.w - 12),
            input.x + 6,
            input.y + 6,
            if (command.text.isEmpty()) HugoTheme.textDim else HugoTheme.text,
            false
        )
        if (focused && (caret / 10) % 2 == 0) {
            val caretX = input.x + 6 + font.getWidth(command.text)
            UiDraw.fill(context, caretX, input.y + 4, 1, input.h - 8, HugoTheme.accent)
        }
        drawButton(context, send, "Senden", !working && command.text.isNotBlank())
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        val accountRows = accounts.take(8).mapIndexed { index, obj ->
            obj to UiRect(listFrame.x, listFrame.y + index * 16, listFrame.w, 15)
        }
        accountRows.firstOrNull { it.second.contains(mouseX, mouseY) }?.let {
            selected = it.first
            return true
        }
        when {
            start.contains(mouseX, mouseY) -> act("AFK gestartet.") { ClientApi.afkStart(selectedId()) }
            stop.contains(mouseX, mouseY) -> act("AFK gestoppt.") { ClientApi.afkStop(selectedId()) }
            addAccount.contains(mouseX, mouseY) -> act("Slot angelegt.") { ClientApi.afkAddAccount() }
            link.contains(mouseX, mouseY) -> startLink()
            send.contains(mouseX, mouseY) -> sendCommand()
            input.contains(mouseX, mouseY) -> {
                focused = true
                return true
            }
            else -> focused = false
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (!focused) return false
        return when {
            input.isPaste -> {
                command.setText(client.keyboard.clipboard.take(80))
                true
            }
            input.key() == GLFW.GLFW_KEY_BACKSPACE -> {
                command.backspace()
                true
            }
            input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER -> {
                sendCommand()
                true
            }
            input.key() == GLFW.GLFW_KEY_ESCAPE -> {
                focused = false
                true
            }
            else -> false
        }
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!focused || !input.isValidChar) return false
        command.insert(input.asString())
        return true
    }

    override fun persist() = Unit

    private fun refresh(force: Boolean) {
        if (!ClientSessionStore.hasToken()) {
            status = "Bitte zuerst unter Account anmelden."
            statusError = true
            return
        }
        working = true
        lastLiveAt = System.currentTimeMillis()
        var nextAccounts = emptyList<JsonObject>()
        var nextLogs = emptyList<String>()
        var nextSeries = emptyList<Float>()
        var nextLive = "Offline"
        ClientJobs.submit({ message, error ->
            working = false
            status = message
            statusError = error
            if (!error) {
                accounts = nextAccounts
                logs = nextLogs
                liveSeries = nextSeries
                liveLabel = nextLive
            }
        }) {
            val state = ClientApi.afk(force)
            val live = runCatching { ClientApi.afkLive(force) }.getOrNull()
            nextAccounts = JsonView.objects(state).ifEmpty { JsonView.objects(live) }
            nextLogs = JsonView.objects(live ?: state).map { JsonView.label(it) }
                .ifEmpty { JsonView.lines(live ?: state, 10) }
                .takeLast(10)
            nextSeries = JsonView.numbers(live ?: state).map { it.toFloat() }
            nextLive = JsonView.str(live ?: state, "status", "state", "live") ?: if (nextAccounts.isNotEmpty()) "Live" else "Offline"
            JsonView.str(state, "message", "status") ?: "AFK-Status geladen."
        }
    }

    private fun act(ok: String, call: () -> JsonObject) {
        if (working) return
        if (!ClientSessionStore.hasToken()) {
            status = "Bitte zuerst unter Account anmelden."
            statusError = true
            return
        }
        working = true
        ClientJobs.submit({ message, error ->
            working = false
            status = message
            statusError = error
            if (!error) refresh(true)
        }) {
            val result = call()
            JsonView.str(result, "message", "status") ?: ok
        }
    }

    private fun startLink() {
        act("Link gestartet.") {
            val result = ClientApi.afkLinkStart()
            JsonView.str(result, "url", "link", "href")?.let { url ->
                MinecraftClient.getInstance().execute { Util.getOperatingSystem().open(url) }
            }
            result
        }
    }

    private fun sendCommand() {
        val text = command.text.trim()
        if (text.isEmpty()) return
        act("Befehl gesendet.") {
            ClientApi.afkCommand(text, selectedId())
        }
        command.setText("")
    }

    private fun selectedId(): String? = JsonView.str(selected, "id", "accountId", "uuid", "name")

    private fun drawButton(
        context: DrawContext,
        rect: UiRect,
        label: String,
        enabled: Boolean,
        style: ButtonStyle = ButtonStyle.SECONDARY
    ) {
        UiWidgets.button(context, client.textRenderer, rect, label, lastMouseX, lastMouseY, enabled, style)
    }
}
