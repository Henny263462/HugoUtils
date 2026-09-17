package dev.henny.hugoutils.client.ui

import com.google.gson.JsonObject
import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.api.ClientSessionStore
import dev.henny.hugoutils.api.JsonView
import dev.henny.hugoutils.api.MarketPriceCache
import dev.henny.hugoutils.api.MarketQuote
import dev.henny.hugoutils.api.MarketStats
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.ui.AnimatedFloat
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.Easing
import dev.henny.hugoutils.ui.TextFieldLogic
import dev.henny.hugoutils.ui.UiFrame
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack
import org.lwjgl.glfw.GLFW
import java.util.concurrent.atomic.AtomicInteger

class MarketConfigPage : ConfigPage {
    override val category = ConfigCategory.MARKET_ITEMS
    override val id = "page:${category.id}"
    private val client = MinecraftClient.getInstance()
    private val query = TextFieldLogic(maxLength = 48)
    private val detailReveal = AnimatedFloat(0f, .28f, Easing.EASE_IN_OUT)
    private var frame = UiRect(0, 0, 0, 0)
    private var search = UiRect(0, 0, 0, 0)
    private var viewHit = UiRect(0, 0, 0, 0)
    private var refresh = UiRect(0, 0, 0, 0)
    private val auctionFeed = SlidingFeed()
    private val orderFeed = SlidingFeed()
    private var gridFrame = UiRect(0, 0, 0, 0)
    private var detailFrame = UiRect(0, 0, 0, 0)
    private var backHit = UiRect(0, 0, 0, 0)
    private var stackOne = UiRect(0, 0, 0, 0)
    private var stack64 = UiRect(0, 0, 0, 0)
    private var alertHit = UiRect(0, 0, 0, 0)
    private var auctionCard = UiRect(0, 0, 0, 0)
    private var orderCard = UiRect(0, 0, 0, 0)
    private var chartFrame = UiRect(0, 0, 0, 0)
    private var auctionList = UiRect(0, 0, 0, 0)
    private var orderList = UiRect(0, 0, 0, 0)
    private var cards = emptyList<Card>()
    private var items = emptyList<JsonObject>()
    private var quotes = emptyMap<String, MarketQuote>()
    private var selected: JsonObject? = null
    private var selectedQuote: MarketQuote? = null
    private var status = "Anmelden unter Account, dann Items laden."
    private var statusError = false
    private var working = false
    private var focused = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var caret = 0
    private var gridScroll = 0
    private var auctionScroll = 0
    private var orderScroll = 0
    private var stackSize = 1
    private var hoveredTip: String? = null
    private var hoveredItem: ItemStack? = null
    private var lastLiveAt = 0L
    private var lastDetailAt = 0L
    private var page = 1
    private var hasMore = false
    private var loadingMore = false
    private var menu: ItemMenu? = null
    private var hoverX = 0f
    private var hoverY = 0f
    private val generation = AtomicInteger()
    private val detailGeneration = AtomicInteger()

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val h = height.coerceAtLeast(300)
        frame = UiRect(x, y, width, h)
        search = UiRect(x + 8, y + 8, width - 16 - 78 - 50, 20)
        viewHit = UiRect(search.right() + 6, y + 8, 44, 20)
        refresh = UiRect(viewHit.right() + 6, y + 8, 72, 20)
        gridFrame = UiRect(x + 8, y + 34, width - 16, h - 42)
        detailFrame = UiRect(x, y, width, h)
        backHit = UiRect(x + width - 86, y + 8, 78, 20)
        stack64 = UiRect(backHit.x - 40, y + 8, 34, 20)
        stackOne = UiRect(stack64.x - 34, y + 8, 28, 20)
        alertHit = UiRect(stackOne.x - 62, y + 8, 56, 20)
        val split = ((width - 24) / 2).coerceAtLeast(120)
        auctionCard = UiRect(x + 8, y + 36, split, 72)
        orderCard = UiRect(auctionCard.right() + 8, y + 36, width - 16 - split - 8, 72)
        chartFrame = UiRect(x + 8, auctionCard.bottom() + 8, width - 16, 110)
        val listsTop = chartFrame.bottom() + 8
        val listsH = (frame.bottom() - listsTop - 8).coerceAtLeast(72)
        auctionList = UiRect(x + 8, listsTop, split, listsH)
        orderList = UiRect(auctionCard.right() + 8, listsTop, orderCard.w, listsH)
        return frame.h
    }

    override fun resetUi() {
        focused = false
        gridScroll = 0
        menu = null
        closeDetail(immediate = true)
    }

    override fun onShown() {
        closeDetail(immediate = true)
        if (ClientSessionStore.hasToken() && items.isEmpty() && !working) {
            load(false)
        }
    }

    fun applySearch(text: String) {
        query.setText(text.take(48))
        closeDetail(immediate = true)
        load(true)
    }

    override fun hoveredTooltip(): String? = hoveredTip
    override fun hoveredStack(): ItemStack? = hoveredItem

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        caret++
        maybeLiveRefresh()
        detailReveal.update(UiFrame.deltaSeconds)
        auctionFeed.update()
        orderFeed.update()
        if (detailReveal.target == 0f && !detailReveal.running) {
            selected = null
            selectedQuote = null
            auctionFeed.clear()
            orderFeed.clear()
        }
        hoveredTip = null
        hoveredItem = null
        val reveal = detailReveal.value
        if (reveal < 0.999f) {
            context.matrices.pushMatrix()
            context.matrices.translate(-reveal * frame.w * 0.22f, 0f)
            drawGrid(context)
            context.matrices.popMatrix()
            menu?.let { drawItemMenu(context, it) }
        }
        if (reveal > 0.001f) {
            context.matrices.pushMatrix()
            context.matrices.translate((1f - reveal) * frame.w, 0f)
            drawDetail(context)
            context.matrices.popMatrix()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (detailReveal.running) return frame.contains(mouseX, mouseY)
        if (showingDetail()) {
            if (backHit.contains(mouseX, mouseY)) {
                closeDetail()
                return true
            }
            if (stackOne.contains(mouseX, mouseY)) {
                stackSize = 1
                return true
            }
            if (stack64.contains(mouseX, mouseY)) {
                stackSize = 64
                return true
            }
            if (alertHit.contains(mouseX, mouseY)) {
                openAlert(selected, selectedQuote)
                return true
            }
            return frame.contains(mouseX, mouseY)
        }
        if (search.contains(mouseX, mouseY)) {
            focused = true
            return true
        }
        focused = false
        if (viewHit.contains(mouseX, mouseY)) {
            ConfigManager.update { it.marketListView = !it.marketListView }
            return true
        }
        if (refresh.contains(mouseX, mouseY) && !working) {
            load(true)
            return true
        }
        cards.firstOrNull { it.rect.contains(mouseX, mouseY) }?.let { card ->
            openItem(card.obj, card.quote)
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !showingDetail()) {
            menu?.items?.firstOrNull { it.rect.contains(mouseX, mouseY) }?.let { item ->
                menu = null
                item.action()
                return true
            }
            cards.firstOrNull { it.rect.contains(mouseX, mouseY) }?.let { card ->
                openPinMenu(card, mouseX.toInt(), mouseY.toInt())
                return true
            }
            menu = null
            return frame.contains(mouseX, mouseY)
        }
        val current = menu
        if (current != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            current.items.firstOrNull { it.rect.contains(mouseX, mouseY) }?.let { item ->
                menu = null
                item.action()
                return true
            }
            menu = null
        }
        return mouseClicked(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (showingDetail()) {
            val delta = -(amount * 18).toInt()
            if (auctionList.contains(mouseX, mouseY)) {
                val max = (auctionFeed.visible().size * LIST_ROW - auctionList.h + 28).coerceAtLeast(0)
                auctionScroll = (auctionScroll + delta).coerceIn(0, max)
                return true
            }
            if (orderList.contains(mouseX, mouseY)) {
                val max = (orderFeed.visible().size * LIST_ROW - orderList.h + 28).coerceAtLeast(0)
                orderScroll = (orderScroll + delta).coerceIn(0, max)
                return true
            }
            return false
        }
        if (!gridFrame.contains(mouseX, mouseY)) return false
        val max = maxGridScroll()
        gridScroll = (gridScroll - (amount * 22).toInt()).coerceIn(0, max)
        if (hasMore && gridScroll >= (max * 0.78f).toInt()) {
            load(false, append = true)
        }
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (showingDetail() && input.key() == GLFW.GLFW_KEY_ESCAPE) {
            closeDetail()
            return true
        }
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
                load(true)
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

    private fun showingDetail(): Boolean = selected != null && detailReveal.target > 0.5f

    private fun closeDetail(immediate: Boolean = false) {
        if (immediate) {
            detailReveal.snapTo(0f)
            selected = null
            selectedQuote = null
            auctionFeed.clear()
            orderFeed.clear()
        } else {
            detailReveal.animateTo(0f)
        }
        stackSize = 1
        auctionScroll = 0
        orderScroll = 0
    }

    fun openItemId(id: String) {
        val existing = items.firstOrNull { MarketStats.itemId(it) == id }
        if (existing != null) {
            openItem(existing, quoteOf(existing))
            return
        }
        if (!ClientSessionStore.hasToken()) return
        val request = detailGeneration.incrementAndGet()
        var itemBody: JsonObject? = null
        var quote: MarketQuote? = null
        ClientJobs.submit({ message, error ->
            if (request != detailGeneration.get()) return@submit
            status = message
            statusError = error
            if (!error) {
                val item = itemBody ?: return@submit
                val next = quote ?: MarketStats.quote(item)
                val key = MarketStats.itemId(item) ?: id
                quotes = quotes + (key to next)
                items = (listOf(item) + items).distinctBy { MarketStats.itemId(it) }
                openItem(item, next)
            }
        }) {
            itemBody = ClientApi.item(id, true)
            quote = MarketStats.quote(itemBody as JsonObject)
            "Details geladen."
        }
    }

    private fun openItem(obj: JsonObject, quote: MarketQuote) {
        menu = null
        selected = obj
        selectedQuote = quote
        auctionFeed.clear()
        orderFeed.clear()
        auctionScroll = 0
        orderScroll = 0
        stackSize = 1
        detailReveal.animateTo(1f)
        loadDetails(obj, quote)
    }

    private fun maybeLiveRefresh() {
        if (!ClientSessionStore.hasToken()) return
        val now = System.currentTimeMillis()
        if (showingDetail()) {
            if (now - lastDetailAt < DETAIL_LIVE_MS) return
            val obj = selected ?: return
            loadDetails(obj, selectedQuote ?: quoteOf(obj), silent = true)
            return
        }
        if (working || loadingMore || page > 1 || gridScroll > 24) return
        if (now - lastLiveAt < LIVE_MS) return
        load(false)
    }

    private fun load(force: Boolean, append: Boolean = false) {
        if (!ClientSessionStore.hasToken()) {
            status = "Bitte zuerst unter Account anmelden."
            statusError = true
            items = emptyList()
            quotes = emptyMap()
            return
        }
        if (append) {
            if (!hasMore || loadingMore || working) return
            loadingMore = true
        } else {
            working = true
            status = "Lade Items …"
            statusError = false
            page = 1
        }
        lastLiveAt = System.currentTimeMillis()
        val request = generation.incrementAndGet()
        val q = query.text.trim()
        val requestPage = if (append) page + 1 else 1
        val currentItems = items
        var nextItems = emptyList<JsonObject>()
        var nextQuotes = emptyMap<String, MarketQuote>()
        var nextHasMore = false
        var nextPage = requestPage
        ClientJobs.submit({ message, error ->
            if (request != generation.get()) return@submit
            working = false
            loadingMore = false
            status = message
            statusError = error
            if (!error) {
                items = nextItems
                quotes = if (append) quotes + nextQuotes else nextQuotes
                hasMore = nextHasMore
                page = nextPage
                selected?.let { current ->
                    val id = MarketStats.itemId(current)
                    selected = nextItems.firstOrNull { MarketStats.itemId(it) == id } ?: current
                    selectedQuote = id?.let { quotes[it] } ?: selectedQuote
                }
                if (!append) hydratePinned()
                if (hasMore && !append && maxGridScroll() <= 0) {
                    load(false, append = true)
                }
            }
        }) {
            val result = ClientApi.items(query = q, page = requestPage, limit = PAGE_SIZE, force = force)
            val batch = JsonView.objects(result)
            nextItems = if (append) {
                (currentItems + batch).distinctBy { MarketStats.itemId(it) }
            } else {
                batch
            }
            val total = JsonView.number(result, "total")?.toInt()
            nextHasMore = if (total != null) nextItems.size < total else batch.size >= PAGE_SIZE
            nextPage = requestPage
            nextQuotes = nextItems.mapNotNull { item ->
                val id = MarketStats.itemId(item) ?: return@mapNotNull null
                id to MarketStats.quote(item)
            }.toMap()
            nextQuotes.values.forEach(MarketPriceCache::put)
            JsonView.str(result, "message") ?: "${nextItems.size} Items"
        }
    }

    private fun loadDetails(obj: JsonObject, current: MarketQuote, silent: Boolean = false) {
        val id = MarketStats.itemId(obj) ?: return
        lastDetailAt = System.currentTimeMillis()
        if (!silent) working = true
        val request = detailGeneration.incrementAndGet()
        var quote = current
        var auctions = emptyList<MarketListing>()
        var orders = emptyList<MarketListing>()
        var itemBody = obj
        ClientJobs.submit({ message, error ->
            if (request != detailGeneration.get()) return@submit
            if (!silent) working = false
            if (!silent) {
                status = message
                statusError = error
            }
            if (!error) {
                selected = itemBody
                selectedQuote = quote
                quotes = quotes + (id to quote)
                auctionFeed.accept(auctions, bounce = false)
                orderFeed.accept(orders, bounce = false)
                MarketNames.resolve(auctions) { auctionFeed.accept(it, bounce = false) }
                MarketNames.resolve(orders) { orderFeed.accept(it, bounce = false) }
            }
        }) {
            itemBody = runCatching { ClientApi.item(id, silent) }.getOrDefault(obj)
            val history = runCatching { ClientApi.history(id, range = "24h", force = silent) }.getOrNull()
            quote = MarketStats.quote(itemBody, history)
            val auctionBody = runCatching { ClientApi.itemAuctions(id, limit = 5, force = silent) }.getOrNull()
                ?: runCatching { ClientApi.listings(itemId = id, source = "auction", limit = 5, force = silent) }.getOrNull()
            val orderBody = runCatching { ClientApi.itemOrders(id, limit = 5, force = silent) }.getOrNull()
                ?: runCatching { ClientApi.listings(itemId = id, source = "order", limit = 5, force = silent) }.getOrNull()
            auctions = MarketListings.parse(auctionBody, "auction", 5)
            orders = MarketListings.parse(orderBody, "order", 5)
            "Details geladen."
        }
    }

    private fun hydratePinned() {
        val known = items.mapNotNull { MarketStats.itemId(it) }.toHashSet()
        val missing = ConfigManager.pinnedMarketItems().filter { it !in known }.take(8)
        if (missing.isEmpty()) return
        var extra = emptyList<JsonObject>()
        var extraQuotes = emptyMap<String, MarketQuote>()
        ClientJobs.submit({ _, error ->
            if (error || extra.isEmpty()) return@submit
            items = (extra + items).distinctBy { MarketStats.itemId(it) }
            quotes = extraQuotes + quotes
        }) {
            extra = missing.mapNotNull { id -> runCatching { ClientApi.item(id) }.getOrNull() }
            extraQuotes = extra.mapNotNull { item ->
                val id = MarketStats.itemId(item) ?: return@mapNotNull null
                id to MarketStats.quote(item)
            }.toMap()
            extraQuotes.values.forEach(MarketPriceCache::put)
            ""
        }
    }

    private fun drawGrid(context: DrawContext) {
        val font = client.textRenderer
        drawInput(context)
        UiWidgets.button(
            context, font, refresh, if (working) "Lädt" else "Reload",
            lastMouseX, lastMouseY, !working, ButtonStyle.SECONDARY, key = "market-reload"
        )
        val listView = ConfigManager.config.marketListView
        UiWidgets.button(
            context, font, viewHit, if (listView) "Liste" else "Grid",
            lastMouseX, lastMouseY, true, ButtonStyle.GHOST, key = "market-view"
        )
        if (listView) {
            drawItemList(context)
            return
        }
        val visible = displayedItems()
        val cols = columns(gridFrame.w)
        val gap = 6
        val cardW = ((gridFrame.w - gap * (cols - 1)) / cols).coerceAtLeast(110)
        val cardH = 78
        val startY = gridFrame.y - gridScroll
        cards = visible.mapIndexed { index, obj ->
            val col = index % cols
            val row = index / cols
            val rect = UiRect(
                gridFrame.x + col * (cardW + gap),
                startY + row * (cardH + gap),
                cardW,
                cardH
            )
            Card(obj, quoteOf(obj), rect)
        }
        context.enableScissor(gridFrame.x, gridFrame.y, gridFrame.right(), gridFrame.bottom())
        cards.forEach { card ->
            if (card.rect.bottom() < gridFrame.y || card.rect.y > gridFrame.bottom()) return@forEach
            drawItemCard(context, card)
        }
        if (cards.isEmpty()) {
            context.drawText(
                font,
                emptyHint(),
                gridFrame.x + 6,
                gridFrame.y + 10,
                if (statusError) HugoTheme.danger else HugoTheme.textDim,
                false
            )
        }
        context.disableScissor()
        UiDraw.scrollbar(context, gridFrame, gridScroll, maxGridScroll())
    }

    private fun drawItemCard(context: DrawContext, card: Card) {
        val font = client.textRenderer
        val quote = card.quote
        val hovered = card.rect.contains(lastMouseX, lastMouseY)
        UiDraw.panel(
            context,
            card.rect,
            if (hovered) HugoTheme.cardHover else HugoTheme.card,
            if (hovered) HugoTheme.accent else HugoTheme.cardBorder
        )
        val stack = MarketStacks.of(quote)
        if (stack != null) {
            context.drawItem(stack, card.rect.x + 6, card.rect.y + 6)
        } else {
            UiDraw.panel(context, UiRect(card.rect.x + 6, card.rect.y + 6, 16, 16), HugoTheme.inset, HugoTheme.cardBorder)
        }
        val pinned = MarketStats.itemId(card.obj)?.let(ConfigManager::isPinnedMarketItem) == true
        context.drawText(
            font,
            UiDraw.ellipsize(font, quote.name, card.rect.w - if (pinned) 42 else 30),
            card.rect.x + 26,
            card.rect.y + 6,
            HugoTheme.text,
            false
        )
        if (pinned) {
            context.drawText(font, "▲", card.rect.right() - 12, card.rect.y + 6, HugoTheme.accent, false)
        }
        context.drawText(
            font,
            quote.cardCount() ?: "—",
            card.rect.x + 26,
            card.rect.y + 16,
            HugoTheme.textDim,
            false
        )
        val price = scaledPrice(quote.auctionAverage ?: quote.average ?: quote.last) ?: "—"
        context.drawText(font, price, card.rect.x + 8, card.rect.y + 28, HugoTheme.text, false)
        quote.changeLabel()?.let { label ->
            context.drawText(
                font,
                label,
                card.rect.right() - font.getWidth(label) - 8,
                card.rect.y + 28,
                quote.changeColor(),
                false
            )
        }
        val spark = UiRect(card.rect.x + 8, card.rect.y + 42, card.rect.w - 16, 28)
        val series = quote.auctionChart.ifEmpty { quote.chart }
        if (series.size >= 2) {
            UiDraw.sparkline(context, spark, series, HugoTheme.success, fill = false)
        }
        if (hovered) {
            hoveredTip = quote.tooltip()
            hoveredItem = stack
        }
    }

    private fun drawDetail(context: DrawContext) {
        val font = client.textRenderer
        val quote = selectedQuote ?: selected?.let(::quoteOf) ?: return
        val stack = MarketStacks.of(quote)
        val icon = UiRect(detailFrame.x + 8, detailFrame.y + 6, 16, 16)
        if (stack != null) {
            context.drawItem(stack, icon.x, icon.y)
            if (icon.contains(lastMouseX, lastMouseY)) hoveredItem = stack
        }
        context.drawText(
            font,
            UiDraw.ellipsize(font, quote.name, alertHit.x - detailFrame.x - 32),
            detailFrame.x + 28,
            detailFrame.y + 10,
            HugoTheme.text,
            false
        )
        val watching = selected?.let(MarketStats::itemId)?.let { id ->
            ConfigManager.priceAlerts().any { it.itemId == id }
        } == true
        drawChip(context, alertHit, "Wecker", watching)
        drawChip(context, stackOne, "1×", stackSize == 1)
        drawChip(context, stack64, "64×", stackSize == 64)
        UiWidgets.button(
            context, font, backHit, "Alle Items", lastMouseX, lastMouseY, true, ButtonStyle.GHOST, key = "market-back"
        )
        drawStatCard(
            context,
            auctionCard,
            "AUKTIONEN",
            scaledPrice(quote.auctionAverage ?: quote.average),
            quote.changeLabel(quote.auctionChange ?: quote.changePercent),
            quote.changeColor(quote.auctionChange ?: quote.changePercent),
            quote.auctionCount ?: quote.count
        )
        drawStatCard(
            context,
            orderCard,
            "ORDERS",
            scaledPrice(quote.orderAverage),
            quote.changeLabel(quote.orderChange),
            quote.changeColor(quote.orderChange),
            quote.orderCount
        )
        UiDraw.panel(context, chartFrame, HugoTheme.inset, HugoTheme.cardBorder)
        context.drawText(font, "STATISTIK", chartFrame.x + 8, chartFrame.y + 6, HugoTheme.textDim, false)
        context.drawText(font, "Auktionen", chartFrame.x + 78, chartFrame.y + 6, HugoTheme.success, false)
        context.drawText(font, "Orders", chartFrame.x + 140, chartFrame.y + 6, HugoTheme.accent, false)
        val plot = UiRect(chartFrame.x + 8, chartFrame.y + 20, chartFrame.w - 16, chartFrame.h - 28)
        val auctions = quote.auctionChart
        val orders = quote.orderChart
        when {
            auctions.size >= 2 && orders.size >= 2 ->
                UiDraw.dualChart(context, plot, auctions, HugoTheme.success, orders, HugoTheme.accent)
            auctions.size >= 2 -> UiDraw.chart(context, plot, auctions, HugoTheme.success)
            orders.size >= 2 -> UiDraw.chart(context, plot, orders, HugoTheme.accent)
            quote.chart.size >= 2 -> UiDraw.chart(context, plot, quote.chart, HugoTheme.success)
            else -> context.drawText(font, "Kein Chart", plot.x + 6, plot.y + 20, HugoTheme.textDim, false)
        }
        drawChartHover(context, plot, quote, auctions, orders)
        drawListingPanel(context, auctionList, "AUKTIONEN", auctionFeed, auctionScroll)
        drawListingPanel(context, orderList, "ORDERS", orderFeed, orderScroll)
    }

    private fun drawChartHover(
        context: DrawContext,
        plot: UiRect,
        quote: MarketQuote,
        auctions: List<Float>,
        orders: List<Float>
    ) {
        if (!plot.contains(lastMouseX, lastMouseY)) return
        val font = client.textRenderer
        val all = (if (auctions.size >= 2) auctions else emptyList()) + (if (orders.size >= 2) orders else emptyList())
        if (all.size < 2) return
        val min = all.min()
        val max = all.max().coerceAtLeast(min + 0.0001f)
        val auctionLine = if (auctions.size >= 2) UiDraw.downsample(auctions, UiDraw.sampleCount(plot.w, auctions.size)) else auctions
        val orderLine = if (orders.size >= 2) UiDraw.downsample(orders, UiDraw.sampleCount(plot.w, orders.size)) else orders
        val t = ((lastMouseX - plot.x) / plot.w.toDouble()).toFloat().coerceIn(0f, 1f)
        val targetX = lastMouseX.toFloat()
        val auctionY = if (auctionLine.size >= 2) UiDraw.yOnChart(plot, UiDraw.valueAt(auctionLine, t), min, max).toFloat() else null
        val orderY = if (orderLine.size >= 2) UiDraw.yOnChart(plot, UiDraw.valueAt(orderLine, t), min, max).toFloat() else null
        val followY = auctionY ?: orderY ?: return
        if (hoverX == 0f && hoverY == 0f) {
            hoverX = targetX
            hoverY = followY
        } else {
            hoverX = UiDraw.lerp(hoverX, targetX, 0.38f)
            hoverY = UiDraw.lerp(hoverY, followY, 0.38f)
        }
        val x = hoverX.toInt().coerceIn(plot.x, plot.right() - 1)
        UiDraw.fill(context, x, plot.y, 1, plot.h, HugoTheme.accentMuted)
        auctionY?.let { UiDraw.fill(context, x - 2, it.toInt() - 2, 5, 5, HugoTheme.success) }
        orderY?.let { UiDraw.fill(context, x - 2, it.toInt() - 2, 5, 5, HugoTheme.accent) }
        val auctionIndex = ((t * (auctions.size - 1)).toInt()).coerceIn(0, (auctions.size - 1).coerceAtLeast(0))
        val orderIndex = ((t * (orders.size - 1)).toInt()).coerceIn(0, (orders.size - 1).coerceAtLeast(0))
        val auctionPoint = quote.auctionPoints.getOrNull(auctionIndex)
        val orderPoint = quote.orderPoints.getOrNull(orderIndex)
        val time = auctionPoint?.timeLabel()?.ifBlank { null } ?: orderPoint?.timeLabel()?.ifBlank { null }
        val auctionPrice = if (auctionLine.size >= 2) UiDraw.valueAt(auctionLine, t).toDouble() else null
        val orderPrice = if (orderLine.size >= 2) UiDraw.valueAt(orderLine, t).toDouble() else null
        val lines = buildList {
            time?.let(::add)
            MarketStats.formatCardPrice(auctionPrice)?.let { add("Auktion  $it") }
            MarketStats.formatCardPrice(orderPrice)?.let { add("Order  $it") }
        }
        if (lines.isEmpty()) return
        val boxW = lines.maxOf { font.getWidth(it) } + 10
        val boxH = lines.size * 10 + 8
        var boxX = x + 8
        var boxY = hoverY.toInt() - boxH / 2
        if (boxX + boxW > plot.right()) boxX = x - boxW - 8
        boxY = boxY.coerceIn(plot.y, plot.bottom() - boxH)
        UiDraw.panel(context, UiRect(boxX, boxY, boxW, boxH), HugoTheme.tooltipBg, HugoTheme.accent)
        lines.forEachIndexed { index, line ->
            context.drawText(font, line, boxX + 5, boxY + 5 + index * 10, HugoTheme.text, false)
        }
    }

    private fun drawStatCard(
        context: DrawContext,
        rect: UiRect,
        title: String,
        price: String?,
        change: String?,
        changeColor: Int,
        count: Int?
    ) {
        val font = client.textRenderer
        UiDraw.panel(context, rect, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, title, rect.x + 8, rect.y + 8, HugoTheme.textDim, false)
        context.drawText(font, price ?: "—", rect.x + 8, rect.y + 22, HugoTheme.success, false)
        context.drawText(font, "pro Stück", rect.x + 8, rect.y + 36, HugoTheme.textDim, false)
        change?.let {
            context.drawText(font, it, rect.right() - font.getWidth(it) - 8, rect.y + 22, changeColor, false)
        }
        context.drawText(
            font,
            MarketStats.formatWindow(count),
            rect.x + 8,
            rect.y + 52,
            HugoTheme.textMuted,
            false
        )
    }

    private fun drawListingPanel(
        context: DrawContext,
        rect: UiRect,
        title: String,
        feed: SlidingFeed,
        scroll: Int
    ) {
        val font = client.textRenderer
        val rows = feed.visible()
        UiDraw.panel(context, rect, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, title, rect.x + 8, rect.y + 8, HugoTheme.textDim, false)
        context.drawText(font, rows.size.toString(), rect.right() - font.getWidth(rows.size.toString()) - 8, rect.y + 8, HugoTheme.textMuted, false)
        val body = UiRect(rect.x + 4, rect.y + 22, rect.w - 8, rect.h - 26)
        context.enableScissor(body.x, body.y, body.right(), body.bottom())
        if (rows.isEmpty()) {
            context.drawText(font, "Keine Einträge", body.x + 6, body.y + 8, HugoTheme.textDim, false)
        } else {
            rows.forEachIndexed { index, row ->
                val listing = row.listing
                val y = body.y + index * LIST_ROW - scroll + feed.offset(row, LIST_ROW)
                if (y + LIST_ROW < body.y || y > body.bottom()) return@forEachIndexed
                context.drawText(font, UiDraw.ellipsize(font, listing.seller, body.w - 12), body.x + 6, y + 3, HugoTheme.text, false)
                val meta = listOf(listing.qty, listing.age).filter { it.isNotBlank() }.joinToString(" · ")
                context.drawText(font, UiDraw.ellipsize(font, meta, body.w / 2), body.x + 6, y + 13, HugoTheme.textDim, false)
                context.drawText(
                    font,
                    listing.price,
                    body.right() - font.getWidth(listing.price) - 6,
                    y + 3,
                    HugoTheme.success,
                    false
                )
                context.drawText(font, "/ Stück", body.right() - font.getWidth("/ Stück") - 6, y + 13, HugoTheme.textDim, false)
            }
        }
        context.disableScissor()
    }

    private fun drawItemList(context: DrawContext) {
        val font = client.textRenderer
        val startY = gridFrame.y - gridScroll
        cards = displayedItems().mapIndexed { index, obj ->
            Card(
                obj,
                quoteOf(obj),
                UiRect(gridFrame.x, startY + index * ITEM_ROW, gridFrame.w, ITEM_ROW - 4)
            )
        }
        context.enableScissor(gridFrame.x, gridFrame.y, gridFrame.right(), gridFrame.bottom())
        cards.forEach { card ->
            if (card.rect.bottom() < gridFrame.y || card.rect.y > gridFrame.bottom()) return@forEach
            drawItemRow(context, card)
        }
        if (cards.isEmpty()) {
            context.drawText(
                font,
                emptyHint(),
                gridFrame.x + 6,
                gridFrame.y + 10,
                if (statusError) HugoTheme.danger else HugoTheme.textDim,
                false
            )
        }
        context.disableScissor()
        UiDraw.scrollbar(context, gridFrame, gridScroll, maxGridScroll())
    }

    private fun drawItemRow(context: DrawContext, card: Card) {
        val font = client.textRenderer
        val quote = card.quote
        val hovered = card.rect.contains(lastMouseX, lastMouseY)
        UiDraw.panel(
            context,
            card.rect,
            if (hovered) HugoTheme.cardHover else HugoTheme.card,
            if (hovered) HugoTheme.accent else HugoTheme.cardBorder
        )
        val stack = MarketStacks.of(quote)
        if (stack != null) {
            context.drawItem(stack, card.rect.x + 6, card.rect.y + 6)
        } else {
            UiDraw.panel(context, UiRect(card.rect.x + 6, card.rect.y + 6, 16, 16), HugoTheme.inset, HugoTheme.cardBorder)
        }
        val pinned = MarketStats.itemId(card.obj)?.let(ConfigManager::isPinnedMarketItem) == true
        val name = UiDraw.ellipsize(font, quote.name, card.rect.w / 2)
        context.drawText(font, name, card.rect.x + 28, card.rect.y + 4, HugoTheme.text, false)
        if (pinned) {
            context.drawText(font, "▲", card.rect.x + 28 + font.getWidth(name) + 4, card.rect.y + 4, HugoTheme.accent, false)
        }
        context.drawText(
            font,
            quote.cardCount() ?: "—",
            card.rect.x + 28,
            card.rect.y + 14,
            HugoTheme.textDim,
            false
        )
        val spark = UiRect(card.rect.x + card.rect.w / 2 - 20, card.rect.y + 6, 72, 18)
        val series = quote.auctionChart.ifEmpty { quote.chart }
        if (series.size >= 2) {
            UiDraw.sparkline(context, spark, series, HugoTheme.success, fill = false)
        }
        val price = scaledPrice(quote.auctionAverage ?: quote.average ?: quote.last) ?: "—"
        context.drawText(
            font,
            price,
            card.rect.right() - font.getWidth(price) - 8,
            card.rect.y + 4,
            HugoTheme.text,
            false
        )
        quote.changeLabel()?.let { label ->
            context.drawText(
                font,
                label,
                card.rect.right() - font.getWidth(label) - 8,
                card.rect.y + 14,
                quote.changeColor(),
                false
            )
        }
        if (hovered) {
            hoveredTip = quote.tooltip()
            hoveredItem = stack
        }
    }

    private fun drawChip(context: DrawContext, rect: UiRect, label: String, selected: Boolean) {
        val font = client.textRenderer
        UiDraw.panel(
            context,
            rect,
            if (selected) HugoTheme.accentSoft else HugoTheme.inset,
            if (selected) HugoTheme.accent else HugoTheme.cardBorder
        )
        context.drawText(
            font,
            label,
            rect.x + (rect.w - font.getWidth(label)) / 2,
            rect.y + 6,
            if (selected) HugoTheme.text else HugoTheme.textMuted,
            false
        )
    }

    private fun drawInput(context: DrawContext) {
        val font = client.textRenderer
        UiDraw.panel(context, search, HugoTheme.inset, if (focused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = if (query.text.isEmpty()) "Items suchen …" else query.text
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

    private fun scaledPrice(value: Double?): String? =
        MarketStats.formatCardPrice(value?.times(stackSize.toDouble()))

    private fun columns(width: Int): Int = when {
        width >= 640 -> 4
        width >= 430 -> 3
        else -> 2
    }

    private fun maxGridScroll(): Int {
        val count = displayedItems().size
        if (ConfigManager.config.marketListView) {
            return (count * ITEM_ROW - gridFrame.h).coerceAtLeast(0)
        }
        val cols = columns(gridFrame.w)
        val rows = if (count == 0) 0 else (count + cols - 1) / cols
        val contentH = rows * 84
        return (contentH - gridFrame.h).coerceAtLeast(0)
    }

    private fun displayedItems(): List<JsonObject> {
        val pinned = ConfigManager.pinnedMarketItems()
        if (pinned.isEmpty()) return items
        val byId = items.associateBy { MarketStats.itemId(it) }
        val pinnedItems = pinned.mapNotNull { byId[it] }
        val rest = items.filter { id -> MarketStats.itemId(id)?.let { it !in pinned } ?: true }
        return pinnedItems + rest
    }

    private fun openPinMenu(card: Card, x: Int, y: Int) {
        val id = MarketStats.itemId(card.obj) ?: return
        val pinned = ConfigManager.isPinnedMarketItem(id)
        val width = 132
        val height = 48
        val left = x.coerceIn(frame.x, (frame.right() - width).coerceAtLeast(frame.x))
        val top = y.coerceIn(frame.y, (frame.bottom() - height).coerceAtLeast(frame.y))
        val pin = UiRect(left + 4, top + 4, width - 8, 18)
        val alert = UiRect(left + 4, pin.bottom() + 2, width - 8, 18)
        menu = ItemMenu(
            UiRect(left, top, width, height),
            listOf(
                MenuItem(if (pinned) "Pin lösen" else "Oben pinnen", pin) {
                    ConfigManager.togglePinnedMarketItem(id)
                },
                MenuItem("Preiswecker", alert) {
                    openAlert(card.obj, card.quote)
                }
            )
        )
    }

    private fun openAlert(obj: JsonObject?, quote: MarketQuote?) {
        val id = obj?.let(MarketStats::itemId) ?: quote?.id ?: return
        PopupManager.open(
            PriceAlertPopup(
                itemId = id,
                itemName = quote?.name ?: JsonView.str(obj, "displayName", "name") ?: "Item",
                minecraftId = quote?.minecraftId ?: JsonView.str(obj, "minecraftId"),
                suggested = quote?.priceFor("any", false) ?: quote?.auctionAverage ?: quote?.last
            )
        )
    }

    private fun drawItemMenu(context: DrawContext, current: ItemMenu) {
        val font = client.textRenderer
        UiDraw.shadow(context, current.frame)
        UiDraw.panel(context, current.frame, HugoTheme.panel, HugoTheme.cardBorder)
        current.items.forEach { item ->
            val hovered = item.rect.contains(lastMouseX, lastMouseY)
            if (hovered) UiDraw.fill(context, item.rect, HugoTheme.accentSoft)
            context.drawText(font, item.label, item.rect.x + 6, item.rect.y + 5, HugoTheme.text, false)
        }
    }

    private fun emptyHint(): String = when {
        !ClientSessionStore.hasToken() -> "Nicht angemeldet."
        working -> "Lädt …"
        statusError -> status
        else -> "Keine Items."
    }

    private fun quoteOf(obj: JsonObject): MarketQuote {
        val id = MarketStats.itemId(obj)
        return id?.let { quotes[it] } ?: MarketStats.quote(obj)
    }

    private data class Card(val obj: JsonObject, val quote: MarketQuote, val rect: UiRect)
    private data class MenuItem(val label: String, val rect: UiRect, val action: () -> Unit)
    private data class ItemMenu(val frame: UiRect, val items: List<MenuItem>)

    companion object {
        private const val LIST_ROW = 28
        private const val ITEM_ROW = 32
        private const val PAGE_SIZE = 24
        private const val LIVE_MS = 15_000L
        private const val DETAIL_LIVE_MS = 4_000L
    }
}
