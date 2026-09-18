package dev.henny.hugoutils.client.ui

import com.google.gson.JsonObject
import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.api.ClientSessionStore
import dev.henny.hugoutils.api.JsonView
import dev.henny.hugoutils.api.MarketPriceCache
import dev.henny.hugoutils.api.MarketQuote
import dev.henny.hugoutils.api.MarketStats
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.TextFieldLogic
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack
import org.lwjgl.glfw.GLFW
import java.util.concurrent.atomic.AtomicInteger

class MarketHomePage : ConfigPage {
    override val category = ConfigCategory.MARKET_HOME
    override val id = "page:${category.id}"
    private val client = MinecraftClient.getInstance()
    private val query = TextFieldLogic(maxLength = 48)
    private val auctionFeed = SlidingFeed()
    private val orderFeed = SlidingFeed()
    private var frame = UiRect(0, 0, 0, 0)
    private var search = UiRect(0, 0, 0, 0)
    private var openHit = UiRect(0, 0, 0, 0)
    private var chipRow = UiRect(0, 0, 0, 0)
    private var auctionStat = UiRect(0, 0, 0, 0)
    private var orderStat = UiRect(0, 0, 0, 0)
    private var playerStat = UiRect(0, 0, 0, 0)
    private var auctionList = UiRect(0, 0, 0, 0)
    private var orderList = UiRect(0, 0, 0, 0)
    private var chips = emptyList<Chip>()
    private var chipHits = emptyList<Pair<Chip, UiRect>>()
    private var auctionHits = emptyList<Pair<MarketListing, UiRect>>()
    private var orderHits = emptyList<Pair<MarketListing, UiRect>>()
    private var auctionCard = StatCard("Auktionen", "—", null, emptyList())
    private var orderCard = StatCard("Orders", "—", null, emptyList())
    private var playerCard = StatCard("Spieler", "—", null, emptyList())
    private var status = "Anmelden unter Account, dann Market laden."
    private var statusError = false
    private var working = false
    private var focused = false
    private var caret = 0
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var lastLiveAt = 0L
    private var hoveredTip: String? = null
    private var hoveredItem: ItemStack? = null
    private val generation = AtomicInteger()

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val listsH = 15 * ROW + 44
        val h = (170 + listsH + 8).coerceAtLeast(height)
        frame = UiRect(x, y, width, h)
        search = UiRect(x + 8, y + 36, width - 16 - 78, 20)
        openHit = UiRect(search.right() + 6, y + 36, 72, 20)
        chipRow = UiRect(x + 8, y + 64, width - 16, 24)
        val gap = 8
        val cardW = ((width - 16 - gap * 2) / 3).coerceAtLeast(90)
        auctionStat = UiRect(x + 8, y + 96, cardW, 64)
        orderStat = UiRect(auctionStat.right() + gap, y + 96, cardW, 64)
        playerStat = UiRect(orderStat.right() + gap, y + 96, width - 16 - cardW * 2 - gap * 2, 64)
        val listsTop = auctionStat.bottom() + 10
        val split = ((width - 24) / 2).coerceAtLeast(140)
        auctionList = UiRect(x + 8, listsTop, split, listsH)
        orderList = UiRect(auctionList.right() + 8, listsTop, width - 16 - split - 8, listsH)
        return frame.h
    }

    override fun resetUi() {
        focused = false
    }

    override fun onShown() {
        if (ClientSessionStore.hasToken()) load(false)
    }

    override fun hoveredTooltip(): String? = hoveredTip
    override fun hoveredStack(): ItemStack? = hoveredItem

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        caret++
        maybeLiveRefresh()
        auctionFeed.update()
        orderFeed.update()
        hoveredTip = null
        hoveredItem = null
        val font = client.textRenderer
        context.drawText(font, "HugoMarkt", frame.x + (frame.w - font.getWidth("HugoMarkt")) / 2, frame.y + 10, HugoTheme.text, false)
        drawInput(context)
        UiWidgets.button(
            context, font, openHit, "Öffnen", lastMouseX, lastMouseY, true, ButtonStyle.PRIMARY, key = "market-home-open"
        )
        if (working) {
            UiDraw.spinner(context, openHit.x - 12, openHit.y + openHit.h / 2, 5)
        }
        drawChips(context)
        drawStat(context, auctionStat, auctionCard)
        drawStat(context, orderStat, orderCard)
        drawStat(context, playerStat, playerCard)
        drawFeed(context, auctionList, "Auktionen", "Neue Listings je Stunde", auctionFeed, true)
        drawFeed(context, orderList, "Orders", "Neue Listings je Stunde", orderFeed, false)
        if (statusError) {
            context.drawText(font, UiDraw.ellipsize(font, status, frame.w - 20), frame.x + 10, frame.y + 22, HugoTheme.danger, false)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (search.contains(mouseX, mouseY)) {
            focused = true
            return true
        }
        focused = false
        if (openHit.contains(mouseX, mouseY)) {
            submitSearch()
            return true
        }
        chipHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (chip, _) ->
            MarketLinks.openItem(chip.id)
            return true
        }
        auctionHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.first?.itemId?.let {
            MarketLinks.openItem(it)
            return true
        }
        orderHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.first?.itemId?.let {
            MarketLinks.openItem(it)
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (!focused) return false
        return when {
            input.isPaste -> {
                query.setText(client.keyboard.clipboard.take(48))
                true
            }
            input.key() == GLFW.GLFW_KEY_BACKSPACE -> {
                query.backspace()
                true
            }
            input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER -> {
                submitSearch()
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
        query.insert(input.asString())
        return true
    }

    override fun persist() = Unit

    private fun submitSearch() {
        val text = query.text.trim()
        if (text.isEmpty()) return
        if (text.startsWith("@") || text.length <= 16 && !text.contains(' ')) {
            MarketLinks.openPlayers()
            return
        }
        MarketLinks.openItemsSearch(text)
    }

    private fun maybeLiveRefresh() {
        if (!ClientSessionStore.hasToken() || working) return
        val now = System.currentTimeMillis()
        if (now - lastLiveAt < LIVE_MS) return
        load(false)
    }

    private fun load(force: Boolean) {
        if (!ClientSessionStore.hasToken()) {
            status = "Bitte zuerst unter Account anmelden."
            statusError = true
            return
        }
        working = true
        statusError = false
        lastLiveAt = System.currentTimeMillis()
        val request = generation.incrementAndGet()
        var nextAuctions = emptyList<MarketListing>()
        var nextOrders = emptyList<MarketListing>()
        var nextChips = emptyList<Chip>()
        var nextAuction = auctionCard
        var nextOrder = orderCard
        var nextPlayer = playerCard
        ClientJobs.submit({ message, error ->
            if (request != generation.get()) return@submit
            working = false
            status = message
            statusError = error
            if (!error) {
                auctionFeed.accept(nextAuctions, bounce = false)
                orderFeed.accept(nextOrders, bounce = false)
                chips = nextChips
                auctionCard = nextAuction
                orderCard = nextOrder
                playerCard = nextPlayer
                MarketNames.resolve(nextAuctions) { auctionFeed.accept(it, bounce = false) }
                MarketNames.resolve(nextOrders) { orderFeed.accept(it, bounce = false) }
            }
        }) {
            val home = runCatching { ClientApi.home(force) }.getOrNull()
            val stats = home ?: runCatching { ClientApi.stats(force) }.getOrNull()
            nextAuction = parseStat(stats, "Auktionen", "auctions", "auction", "ah")
            nextOrder = parseStat(stats, "Orders", "orders", "order", "shops")
            nextPlayer = parseStat(stats, "Spieler", "players", "player", "online")
            nextChips = parseChips(home)
            nextChips.forEach { chip ->
                MarketPriceCache.put(
                    MarketQuote(id = chip.id, name = chip.name, minecraftId = chip.minecraftId)
                )
            }
            val auctionBody = runCatching { ClientApi.listings(source = "auction", limit = 15, force = force) }.getOrNull()
            val orderBody = runCatching { ClientApi.listings(source = "order", limit = 15, force = force) }.getOrNull()
            nextAuctions = MarketListings.parse(auctionBody, "auction", 15).ifEmpty {
                JsonView.objectsNamed(home, "auctions", "recentAuctions", "latestAuctions").take(15).map(MarketListings::from)
            }
            nextOrders = MarketListings.parse(orderBody, "order", 15).ifEmpty {
                JsonView.objectsNamed(home, "orders", "recentOrders", "latestOrders").take(15).map(MarketListings::from)
            }
            JsonView.str(home, "message") ?: "Market geladen."
        }
    }

    private fun parseStat(body: JsonObject?, title: String, vararg keys: String): StatCard {
        val nested = JsonView.child(body, *keys) ?: JsonView.child(JsonView.child(body, "stats", "overview", "counts"), *keys)
        val count = JsonView.number(nested, "count", "total", "value", "listingCount")
            ?: keys.firstNotNullOfOrNull { JsonView.number(body, it, "${it}Count", "${it}Total") }
        val change = JsonView.number(nested, "change", "delta", "changePct", "changePercent")
            ?: JsonView.number(body, "${keys.first()}Change", "${keys.first()}Delta")
        val sparkSource = nested?.get("sparkline") ?: nested?.get("points") ?: nested?.get("chart")
        val spark = MarketStats.sparklineValues(sparkSource)
        return StatCard(title, MarketStats.formatGrouped(count), change, spark)
    }

    private fun parseChips(home: JsonObject?): List<Chip> {
        val raw = JsonView.objectsNamed(home, "featured", "trending", "spotlight", "chips", "items")
        return raw.take(8).mapNotNull(::chipFromItem)
    }

    private fun chipFromItem(obj: JsonObject): Chip? {
        val id = MarketStats.itemId(obj) ?: return null
        val quote = MarketStats.quote(obj)
        return Chip(id, quote.name, quote.minecraftId)
    }

    private fun drawInput(context: DrawContext) {
        val font = client.textRenderer
        UiDraw.panel(context, search, HugoTheme.inset, if (focused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = if (query.text.isEmpty()) "Spieler suchen …" else query.text
        context.drawText(
            font,
            UiDraw.ellipsize(font, shown, search.w - 12),
            search.x + 6,
            search.y + 6,
            if (query.text.isEmpty()) HugoTheme.textDim else HugoTheme.text,
            false
        )
        if (focused && (caret / 10) % 2 == 0) {
            val caretX = search.x + 6 + font.getWidth(query.text)
            UiDraw.fill(context, caretX, search.y + 4, 1, search.h - 8, HugoTheme.accent)
        }
    }

    private fun drawChips(context: DrawContext) {
        if (working && chips.isEmpty()) {
            chipHits = emptyList()
            var x = chipRow.x
            repeat(4) {
                if (x + 86 > chipRow.right()) return
                UiDraw.skeleton(context, UiRect(x, chipRow.y, 86, chipRow.h))
                x += 92
            }
            return
        }
        val font = client.textRenderer
        var x = chipRow.x
        val hits = ArrayList<Pair<Chip, UiRect>>()
        chips.forEach { chip ->
            val label = UiDraw.ellipsize(font, chip.name, 110)
            val w = 26 + font.getWidth(label)
            if (x + w > chipRow.right()) return@forEach
            val rect = UiRect(x, chipRow.y, w, chipRow.h)
            val hovered = rect.contains(lastMouseX, lastMouseY)
            UiDraw.panel(
                context,
                rect,
                if (hovered) HugoTheme.accentSoft else HugoTheme.inset,
                if (hovered) HugoTheme.accent else HugoTheme.cardBorder
            )
            val stack = MarketStacks.ofIds(chip.minecraftId, chip.id, chip.name)
            if (stack != null) context.drawItem(stack, rect.x + 4, rect.y + 4)
            context.drawText(font, label, rect.x + 22, rect.y + 8, HugoTheme.text, false)
            if (hovered) hoveredItem = stack
            hits += chip to rect
            x += w + 6
        }
        chipHits = hits
    }

    private fun drawStat(context: DrawContext, rect: UiRect, card: StatCard) {
        val font = client.textRenderer
        if (working && card.value == "—") {
            UiDraw.skeleton(context, rect)
            UiDraw.skeletonBone(context, rect.x + 8, rect.y + 10, 54, 6)
            UiDraw.skeletonBone(context, rect.x + 8, rect.y + 24, 72, 10)
            return
        }
        UiDraw.panel(context, rect, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, card.title.uppercase(), rect.x + 8, rect.y + 8, HugoTheme.textDim, false)
        context.drawText(font, card.value, rect.x + 8, rect.y + 22, HugoTheme.text, false)
        card.change?.let { change ->
            val label = MarketStats.formatDelta(change) ?: return@let
            context.drawText(font, label, rect.x + 8, rect.y + 38, MarketStats.changeColor(change), false)
        }
        if (card.spark.size >= 2) {
            val spark = UiRect(rect.x + rect.w / 2, rect.y + 10, rect.w / 2 - 8, rect.h - 18)
            UiDraw.sparkline(context, spark, card.spark, HugoTheme.success, fill = false)
        }
    }

    private fun drawFeed(
        context: DrawContext,
        rect: UiRect,
        title: String,
        subtitle: String,
        feed: SlidingFeed,
        auctions: Boolean
    ) {
        val font = client.textRenderer
        UiDraw.panel(context, rect, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, title, rect.x + 8, rect.y + 8, HugoTheme.text, false)
        context.drawText(font, subtitle, rect.x + 8, rect.y + 20, HugoTheme.textMuted, false)
        val body = UiRect(rect.x + 4, rect.y + 34, rect.w - 8, rect.h - 38)
        context.enableScissor(body.x, body.y, body.right(), body.bottom())
        val hits = ArrayList<Pair<MarketListing, UiRect>>()
        val rows = feed.visible()
        if (working && rows.isEmpty()) {
            for (index in 0 until (body.h / ROW).coerceAtLeast(1).coerceAtMost(8)) {
                val row = UiRect(body.x, body.y + index * ROW, body.w, ROW - 4)
                if (row.y > body.bottom()) break
                UiDraw.skeletonRow(context, row)
            }
        } else if (rows.isEmpty()) {
            context.drawText(font, "Keine Einträge", body.x + 6, body.y + 8, HugoTheme.textDim, false)
        } else {
            rows.forEachIndexed { index, row ->
                val listing = row.listing
                val y = body.y + index * ROW + feed.offset(row, ROW)
                if (y + ROW < body.y || y > body.bottom()) return@forEachIndexed
                val hit = UiRect(body.x, y, body.w, ROW)
                val hovered = hit.contains(lastMouseX, lastMouseY)
                if (hovered) UiDraw.fill(context, hit, HugoTheme.accentSoft)
                val stack = MarketStacks.of(listing)
                if (stack != null) {
                    context.drawItem(stack, hit.x + 4, y + 6)
                    if (hovered) hoveredItem = stack
                }
                val nameW = body.w - 90
                context.drawText(font, UiDraw.ellipsize(font, listing.name, nameW), hit.x + 24, y + 4, HugoTheme.text, false)
                val meta = "${listing.qty} · ${listing.seller}"
                context.drawText(font, UiDraw.ellipsize(font, meta, nameW), hit.x + 24, y + 14, HugoTheme.textDim, false)
                context.drawText(
                    font,
                    listing.price,
                    hit.right() - font.getWidth(listing.price) - 6,
                    y + 4,
                    HugoTheme.success,
                    false
                )
                context.drawText(
                    font,
                    listing.age,
                    hit.right() - font.getWidth(listing.age) - 6,
                    y + 14,
                    HugoTheme.textMuted,
                    false
                )
                hits += listing to hit
            }
        }
        context.disableScissor()
        if (auctions) auctionHits = hits else orderHits = hits
    }

    private data class Chip(val id: String, val name: String, val minecraftId: String?)
    private data class StatCard(val title: String, val value: String, val change: Double?, val spark: List<Float>)

    companion object {
        private const val ROW = 28
        private const val LIVE_MS = 8_000L
    }
}
