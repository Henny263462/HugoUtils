package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientFeedback
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.api.ClientSessionStore
import dev.henny.hugoutils.api.FeedbackReport
import dev.henny.hugoutils.ui.Button
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.Labels
import dev.henny.hugoutils.ui.LoadingDraw
import dev.henny.hugoutils.ui.SettingHero
import dev.henny.hugoutils.ui.SettingListItem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class FeedbackConfigPage : ConfigPage {
    override val category = ConfigCategory.FEEDBACK
    override val id = "page:${category.id}"
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var list = UiRect(0, 0, 0, 0)
    private var reports = emptyList<FeedbackReport>()
    private var rows = emptyList<Pair<FeedbackReport, UiRect>>()
    private var status = "Anmelden unter Account, dann ein Ticket senden."
    private var statusError = false
    private var working = false
    private var scroll = 0
    private var hoveredTip: String? = null
    private var lastLiveAt = 0L
    private val composeButton = Button("Neue Meldung", { openCompose() }, style = ButtonStyle.PRIMARY)
    private val hero = SettingHero(
        "feedback-compose",
        "Feedback & Support",
        detail = { status },
        action = composeButton
    )

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val h = height.coerceAtLeast(240)
        frame = UiRect(x, y, width, h)
        val heroH = hero.layout(x + 8, y + 8, width - 16)
        list = UiRect(x + 8, y + 8 + heroH + 8, width - 16, h - heroH - 24)
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
        hoveredTip = null
        if (ClientSessionStore.hasToken() && !working && System.currentTimeMillis() - lastLiveAt > 12_000L) {
            load(true)
        }
        composeButton.enabled = !working
        val font = client.textRenderer
        hero.render(context, font, mouseX, mouseY)
        if (statusError) {
            Labels.value(context, font, hero.bounds.x + 12, hero.bounds.y + 30, UiDraw.ellipsize(font, status, hero.bounds.w - 140), HugoTheme.danger)
        }

        val hits = ArrayList<Pair<FeedbackReport, UiRect>>()
        context.enableScissor(list.x, list.y, list.right(), list.bottom())
        if (reports.isEmpty()) {
            if (working && ClientSessionStore.hasToken()) {
                for (index in 0 until 4) {
                    val rect = UiRect(list.x, list.y + index * ROW, list.w, ROW - 8)
                    if (rect.y > list.bottom()) break
                    LoadingDraw.row(context, rect)
                }
            } else {
                Labels.dim(
                    context,
                    font,
                    list.x + 8,
                    list.y + 10,
                    if (!ClientSessionStore.hasToken()) "Bitte zuerst unter Account anmelden." else "Noch keine Meldungen."
                )
            }
        }
        reports.forEachIndexed { index, report ->
            val item = SettingListItem(
                report.id,
                overline = report.kindLabel(),
                trailing = report.statusLabel(),
                trailingColor = if (report.status == "resolved") HugoTheme.success else HugoTheme.textMuted,
                title = report.title,
                detail = report.reply?.takeIf { it.isNotBlank() }?.let { "Antwort: $it" } ?: report.body
            )
            item.layout(list.x, list.y - scroll + index * ROW, list.w)
            if (item.bounds.bottom() >= list.y && item.bounds.y <= list.bottom()) {
                item.render(context, font, mouseX, mouseY)
                if (item.bounds.contains(mouseX.toDouble(), mouseY.toDouble())) {
                    hoveredTip = listOfNotNull(
                        "${report.kindLabel()} · ${report.statusLabel()}",
                        report.title,
                        report.body.takeIf { it.isNotBlank() },
                        report.reply?.let { "Antwort\n$it" }
                    ).joinToString("\n")
                }
            }
            hits += report to item.bounds
        }
        context.disableScissor()
        rows = hits
        UiDraw.scrollbar(context, list, scroll, maxScroll())
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (hero.mouseClicked(mouseX, mouseY)) {
            if (composeButton.bounds.contains(mouseX, mouseY)) return true
            openCompose()
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

    private fun openCompose() {
        if (!ClientSessionStore.hasToken()) {
            status = "Bitte zuerst unter Account anmelden."
            statusError = true
            return
        }
        PopupManager.open(
            FeedbackComposePopup { ok ->
                if (ok) load(true)
            }
        )
    }

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

    private fun maxScroll(): Int = (reports.size * ROW - list.h).coerceAtLeast(0)

    companion object {
        private const val ROW = 62
    }
}
