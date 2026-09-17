package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientFeedback
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.api.ClientSessionStore
import dev.henny.hugoutils.api.FeedbackReport
import dev.henny.hugoutils.ui.ButtonStyle
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class FeedbackConfigPage : ConfigPage {
    override val category = ConfigCategory.FEEDBACK
    override val id = "page:${category.id}"
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var compose = UiRect(0, 0, 0, 0)
    private var list = UiRect(0, 0, 0, 0)
    private var reports = emptyList<FeedbackReport>()
    private var rows = emptyList<Pair<FeedbackReport, UiRect>>()
    private var status = "Anmelden unter Account, dann Feedback senden."
    private var statusError = false
    private var working = false
    private var scroll = 0
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var hoveredTip: String? = null
    private var lastLiveAt = 0L

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val h = height.coerceAtLeast(240)
        frame = UiRect(x, y, width, h)
        compose = UiRect(x + 8, y + 8, width - 16, 58)
        list = UiRect(x + 8, compose.bottom() + 8, width - 16, h - (compose.bottom() - y) - 16)
        return frame.h
    }

    override fun resetUi() {
        scroll = 0
        if (ClientSessionStore.hasToken()) load(false)
    }

    override fun onShown() {
        if (ClientSessionStore.hasToken() && reports.isEmpty() && !working) load(false)
    }

    override fun hoveredTooltip(): String? = hoveredTip

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        hoveredTip = null
        if (ClientSessionStore.hasToken() && !working && System.currentTimeMillis() - lastLiveAt > 12_000L) {
            load(true)
        }
        val font = client.textRenderer
        UiWidgets.hoverCard(context, compose, lastMouseX, lastMouseY, key = "feedback-compose")
        context.drawText(font, "Feedback", compose.x + 12, compose.y + 12, HugoTheme.text, false)
        context.drawText(
            font,
            UiDraw.ellipsize(font, status, compose.w - 140),
            compose.x + 12,
            compose.y + 30,
            if (statusError) HugoTheme.danger else HugoTheme.textMuted,
            false
        )
        val send = UiRect(compose.right() - 118, compose.y + 16, 106, 22)
        UiWidgets.button(
            context, font, send, "Neue Meldung", lastMouseX, lastMouseY,
            enabled = !working, style = ButtonStyle.PRIMARY, key = "feedback-send"
        )

        val hits = ArrayList<Pair<FeedbackReport, UiRect>>()
        context.enableScissor(list.x, list.y, list.right(), list.bottom())
        if (reports.isEmpty()) {
            context.drawText(
                font,
                if (!ClientSessionStore.hasToken()) "Bitte zuerst unter Account anmelden."
                else if (working) "Lädt …"
                else "Noch keine Meldungen.",
                list.x + 8,
                list.y + 10,
                HugoTheme.textDim,
                false
            )
        }
        reports.forEachIndexed { index, report ->
            val rect = UiRect(list.x, list.y - scroll + index * ROW, list.w, ROW - 8)
            if (rect.bottom() >= list.y && rect.y <= list.bottom()) {
                drawReport(context, report, rect)
            }
            hits += report to rect
        }
        context.disableScissor()
        rows = hits
        UiDraw.scrollbar(context, list, scroll, maxScroll())
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (compose.contains(mouseX, mouseY)) {
            if (!ClientSessionStore.hasToken()) {
                status = "Bitte zuerst unter Account anmelden."
                statusError = true
                return true
            }
            PopupManager.open(
                FeedbackComposePopup { ok ->
                    if (ok) load(true)
                }
            )
            return true
        }
        rows.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (report, _) ->
            hoveredTip = listOfNotNull(report.title, report.body, report.reply?.let { "Antwort: $it" })
                .joinToString("\n")
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!list.contains(mouseX, mouseY)) return false
        val max = maxScroll()
        if (max <= 0) return false
        scroll = (scroll - (amount * 22).toInt()).coerceIn(0, max)
        return true
    }

    override fun persist() = Unit

    private fun load(force: Boolean) {
        if (!ClientSessionStore.hasToken()) {
            status = "Bitte zuerst unter Account anmelden."
            statusError = true
            reports = emptyList()
            return
        }
        working = true
        lastLiveAt = System.currentTimeMillis()
        var next = emptyList<FeedbackReport>()
        ClientJobs.submit({ message, error ->
            working = false
            status = message
            statusError = error
            if (!error) reports = next
        }) {
            next = ClientFeedback.parseList(ClientApi.feedback(force))
            "${next.size} Meldungen"
        }
    }

    private fun drawReport(context: DrawContext, report: FeedbackReport, rect: UiRect) {
        val font = client.textRenderer
        val hovered = rect.contains(lastMouseX, lastMouseY)
        UiWidgets.hoverCard(context, rect, lastMouseX, lastMouseY, key = report.id)
        context.drawText(font, report.kindLabel(), rect.x + 10, rect.y + 8, HugoTheme.accent, false)
        context.drawText(
            font,
            report.statusLabel(),
            rect.right() - font.getWidth(report.statusLabel()) - 10,
            rect.y + 8,
            if (report.status == "resolved") HugoTheme.success else HugoTheme.textMuted,
            false
        )
        context.drawText(
            font,
            UiDraw.ellipsize(font, report.title, rect.w - 24),
            rect.x + 10,
            rect.y + 22,
            HugoTheme.text,
            false
        )
        val detail = report.reply?.takeIf { it.isNotBlank() }?.let { "Antwort: $it" } ?: report.body
        context.drawText(
            font,
            UiDraw.ellipsize(font, detail, rect.w - 24),
            rect.x + 10,
            rect.y + 36,
            HugoTheme.textMuted,
            false
        )
        if (hovered) {
            hoveredTip = listOfNotNull(
                "${report.kindLabel()} · ${report.statusLabel()}",
                report.title,
                report.body.takeIf { it.isNotBlank() },
                report.reply?.let { "Antwort\n$it" }
            ).joinToString("\n")
        }
    }

    private fun maxScroll(): Int = (reports.size * ROW - list.h).coerceAtLeast(0)

    companion object {
        private const val ROW = 62
    }
}
