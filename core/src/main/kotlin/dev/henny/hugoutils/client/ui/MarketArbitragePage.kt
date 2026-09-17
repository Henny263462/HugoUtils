package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.api.ArbitrageDeal
import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.api.ClientSessionStore
import dev.henny.hugoutils.api.JsonView
import dev.henny.hugoutils.api.MarketArbitrage
import dev.henny.hugoutils.api.MarketStats
import dev.henny.hugoutils.ui.ButtonStyle
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack
import java.util.concurrent.atomic.AtomicInteger

class MarketArbitragePage : ConfigPage {
    override val category = ConfigCategory.MARKET_ARBITRAGE
    override val id = "page:${category.id}"
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var refresh = UiRect(0, 0, 0, 0)
    private var list = UiRect(0, 0, 0, 0)
    private var deals = emptyList<ArbitrageDeal>()
    private var rows = emptyList<Pair<ArbitrageDeal, UiRect>>()
    private var status = "Anmelden unter Account, dann Arbitrage laden."
    private var statusError = false
    private var working = false
    private var scroll = 0
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var lastLiveAt = 0L
    private var hoveredTip: String? = null
    private var hoveredItem: ItemStack? = null
    private val generation = AtomicInteger()

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val h = height.coerceAtLeast(220)
        frame = UiRect(x, y, width, h)
        refresh = UiRect(x + width - 86, y + 8, 72, 20)
        list = UiRect(x + 8, y + 36, width - 16, h - 48)
        return frame.h
    }

    override fun resetUi() {
        scroll = 0
    }

    override fun onShown() {
        if (ClientSessionStore.hasToken() && deals.isEmpty() && !working) load(false)
    }

    override fun hoveredTooltip(): String? = hoveredTip
    override fun hoveredStack(): ItemStack? = hoveredItem

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        maybeLiveRefresh()
        hoveredTip = null
        hoveredItem = null
        val font = client.textRenderer
        context.drawText(font, "Arbitrage", frame.x + 10, frame.y + 12, HugoTheme.text, false)
        UiWidgets.button(
            context, font, refresh, if (working) "Lädt" else "Reload",
            lastMouseX, lastMouseY, !working, ButtonStyle.SECONDARY, key = "arb-reload"
        )
        context.drawText(
            font,
            "Orders kaufen, Auktionen verkaufen",
            frame.x + 10,
            frame.y + 24,
            HugoTheme.textDim,
            false
        )
        val hits = ArrayList<Pair<ArbitrageDeal, UiRect>>(deals.size)
        val startY = list.y - scroll
        context.enableScissor(list.x, list.y, list.right(), list.bottom())
        if (deals.isEmpty()) {
            context.drawText(
                font,
                emptyHint(),
                list.x + 6,
                list.y + 10,
                if (statusError) HugoTheme.danger else HugoTheme.textDim,
                false
            )
        }
        deals.forEachIndexed { index, deal ->
            val rect = UiRect(list.x, startY + index * ROW, list.w, ROW - 6)
            if (rect.bottom() >= list.y && rect.y <= list.bottom()) {
                drawDeal(context, deal, rect)
            }
            hits += deal to rect
        }
        context.disableScissor()
        rows = hits
        UiDraw.scrollbar(context, list, scroll, maxScroll())
        if (statusError) {
            context.drawText(
                font,
                UiDraw.ellipsize(font, status, frame.w - 110),
                frame.x + 110,
                frame.y + 12,
                HugoTheme.danger,
                false
            )
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (refresh.contains(mouseX, mouseY) && !working) {
            load(true)
            return true
        }
        rows.firstOrNull { it.second.contains(mouseX, mouseY) }?.first?.itemId?.let { id ->
            MarketLinks.openItem(id)
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

    private fun maybeLiveRefresh() {
        if (!ClientSessionStore.hasToken() || working) return
        if (System.currentTimeMillis() - lastLiveAt < LIVE_MS) return
        load(false)
    }

    private fun load(force: Boolean) {
        if (!ClientSessionStore.hasToken()) {
            status = "Bitte zuerst unter Account anmelden."
            statusError = true
            deals = emptyList()
            return
        }
        working = true
        statusError = false
        lastLiveAt = System.currentTimeMillis()
        val request = generation.incrementAndGet()
        var next = emptyList<ArbitrageDeal>()
        ClientJobs.submit({ message, error ->
            if (request != generation.get()) return@submit
            working = false
            status = message
            statusError = error
            if (!error) {
                deals = next
                hydrateNames(next)
            }
        }) {
            val body = ClientApi.arbitrage(force)
            next = MarketArbitrage.parse(body)
            JsonView.str(body, "message") ?: "${next.size} Chancen"
        }
    }

    private fun drawDeal(context: DrawContext, deal: ArbitrageDeal, rect: UiRect) {
        val font = client.textRenderer
        val hovered = rect.contains(lastMouseX, lastMouseY)
        UiDraw.panel(
            context,
            rect,
            if (hovered) HugoTheme.cardHover else HugoTheme.card,
            if (hovered) HugoTheme.accent else HugoTheme.cardBorder
        )
        val stack = MarketStacks.ofIds(deal.minecraftId, deal.itemId, deal.name)
        if (stack != null) {
            context.drawItem(stack, rect.x + 6, rect.y + 10)
            if (hovered) hoveredItem = stack
        } else {
            UiDraw.panel(context, UiRect(rect.x + 6, rect.y + 10, 16, 16), HugoTheme.inset, HugoTheme.cardBorder)
        }
        val textX = rect.x + 28
        context.drawText(font, UiDraw.ellipsize(font, deal.name, rect.w - 150), textX, rect.y + 5, HugoTheme.text, false)
        val buy = buildString {
            append("Kaufen · ${deal.buySource} ")
            append(MarketStats.formatCardPrice(deal.buyPrice) ?: "—")
            deal.buyAverage?.let { append("  Ø ${MarketStats.formatCardPrice(it)}") }
            deal.buyCount?.let { append("  $it ${deal.buySource}") }
            deal.buyVolume?.let { append(" · $it") }
        }
        val sell = buildString {
            append("Verkaufen · ${deal.sellSource} ")
            append(MarketStats.formatCardPrice(deal.sellPrice) ?: "—")
            deal.sellLowest?.let { append("  ab ${MarketStats.formatCardPrice(it)}") }
            deal.sellAverage?.let { append("  Ø ${MarketStats.formatCardPrice(it)}") }
            deal.sellCount?.let { append("  $it ${deal.sellSource}") }
            deal.sellVolume?.let { append(" · $it") }
        }
        context.drawText(font, UiDraw.ellipsize(font, buy, rect.w - 150), textX, rect.y + 16, HugoTheme.textMuted, false)
        context.drawText(font, UiDraw.ellipsize(font, sell, rect.w - 150), textX, rect.y + 26, HugoTheme.textMuted, false)
        val profit = MarketStats.formatCardPrice(deal.profit)
        val pct = deal.profitPct?.let { MarketStats.formatChange(it) }
        profit?.let {
            context.drawText(font, it, rect.right() - font.getWidth(it) - 8, rect.y + 8, HugoTheme.success, false)
        }
        pct?.let {
            context.drawText(font, it, rect.right() - font.getWidth(it) - 8, rect.y + 20, HugoTheme.success, false)
        }
        if (deal.age.isNotBlank()) {
            context.drawText(
                font,
                deal.age,
                rect.right() - font.getWidth(deal.age) - 8,
                rect.y + 32,
                HugoTheme.textDim,
                false
            )
        }
        if (hovered) {
            hoveredTip = listOfNotNull(deal.name, buy, sell, profit?.let { "Gewinn  $it" }, pct, deal.age.takeIf { it.isNotBlank() })
                .joinToString("\n")
        }
    }

    private fun hydrateNames(current: List<ArbitrageDeal>) {
        val listings = current.map {
            MarketListing(it.id, it.itemId, it.name, it.minecraftId, "", "", "", "")
        }
        MarketNames.resolve(listings) { resolved ->
            val byId = resolved.associateBy { it.id }
            deals = deals.map { deal ->
                val hit = byId[deal.id] ?: return@map deal
                deal.copy(name = hit.name, minecraftId = hit.minecraftId ?: deal.minecraftId)
            }
        }
    }

    private fun emptyHint(): String = when {
        !ClientSessionStore.hasToken() -> "Nicht angemeldet."
        working -> "Lädt …"
        statusError -> status
        else -> "Keine Arbitrage-Chancen."
    }

    private fun maxScroll(): Int = (deals.size * ROW - list.h).coerceAtLeast(0)

    companion object {
        private const val ROW = 52
        private const val LIVE_MS = 15_000L
    }
}
