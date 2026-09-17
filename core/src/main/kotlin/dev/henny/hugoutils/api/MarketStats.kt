package dev.henny.hugoutils.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class ChartPoint(
    val timeMs: Long,
    val price: Double,
    val listingCount: Int = 0
) {
    fun timeLabel(): String {
        if (timeMs <= 0L) return ""
        val instant = Instant.ofEpochMilli(timeMs)
        return TIME_FORMAT.format(instant)
    }

    companion object {
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM HH:mm")
            .withZone(ZoneId.systemDefault())
    }
}

data class MarketQuote(
    val id: String?,
    val name: String,
    val minecraftId: String? = null,
    val last: Double? = null,
    val average: Double? = null,
    val orderAverage: Double? = null,
    val auctionAverage: Double? = null,
    val min: Double? = null,
    val max: Double? = null,
    val volume: Double? = null,
    val series: List<Double> = emptyList(),
    val changePercent: Double? = null,
    val count: Int? = null,
    val auctionCount: Int? = null,
    val orderCount: Int? = null,
    val auctionChange: Double? = null,
    val orderChange: Double? = null,
    val auctionSeries: List<Double> = emptyList(),
    val orderSeries: List<Double> = emptyList(),
    val auctionLowest: Double? = null,
    val orderLowest: Double? = null,
    val auctionPoints: List<ChartPoint> = emptyList(),
    val orderPoints: List<ChartPoint> = emptyList()
) {
    val chart: List<Float> get() = series.map { it.toFloat() }
    val auctionChart: List<Float>
        get() = auctionPoints.map { it.price.toFloat() }.ifEmpty { auctionSeries.map { it.toFloat() } }.ifEmpty { chart }
    val orderChart: List<Float>
        get() = orderPoints.map { it.price.toFloat() }.ifEmpty { orderSeries.map { it.toFloat() } }

    fun cardPrice(): String? = MarketStats.formatCardPrice(auctionAverage ?: average ?: last)
    fun cardCount(): String? = MarketStats.formatCount(count)
    fun changeLabel(value: Double? = changePercent): String? = MarketStats.formatChange(value)
    fun changeColor(value: Double? = changePercent): Int = MarketStats.changeColor(value)

    fun rowPrice(): String {
        val order = MarketStats.formatMoney(orderAverage)
        val auction = MarketStats.formatMoney(auctionAverage)
        return when {
            order != null && auction != null -> "$order / $auction"
            order != null -> order
            auction != null -> auction
            else -> MarketStats.formatMoney(average ?: last) ?: ""
        }
    }

    fun tooltip(): String = buildList {
        add(name)
        MarketStats.formatCardPrice(orderAverage)?.let { add("Ø Order  $it") }
        MarketStats.formatCardPrice(auctionAverage)?.let { add("Ø Auktion  $it") }
        MarketStats.formatCardPrice(average)?.let { add("Ø Preis  $it") }
        MarketStats.formatCardPrice(last)?.let { add("Zuletzt  $it") }
        MarketStats.formatCardPrice(orderLowest)?.let { add("Order ab  $it") }
        MarketStats.formatCardPrice(auctionLowest)?.let { add("Auktion ab  $it") }
        MarketStats.formatCardPrice(min)?.let { add("Min  $it") }
        MarketStats.formatCardPrice(max)?.let { add("Max  $it") }
        MarketStats.formatCardPrice(volume)?.let { add("Volumen  $it") }
        if (series.size >= 2) add("${series.size} Punkte im Chart")
    }.joinToString("\n")

    fun priceFor(source: String, preferHigh: Boolean): Double? {
        val values = when (source.lowercase()) {
            "order", "orders", "shop" -> listOfNotNull(orderAverage, orderLowest, last)
            "auction", "auctions", "ah" -> listOfNotNull(auctionAverage, auctionLowest, last)
            else -> listOfNotNull(orderAverage, auctionAverage, average, last, orderLowest, auctionLowest)
        }
        return if (preferHigh) values.maxOrNull() else values.minOrNull()
    }

    fun statChips(): List<Pair<String, String>> = listOfNotNull(
        MarketStats.formatMoney(orderAverage)?.let { "Order" to it },
        MarketStats.formatMoney(auctionAverage)?.let { "Auktion" to it },
        MarketStats.formatMoney(average)?.let { "Ø" to it },
        MarketStats.formatMoney(last)?.let { "Last" to it }
    )
}

object MarketStats {
    fun quote(obj: JsonObject, history: JsonElement? = null): MarketQuote {
        val historyObj = history as? JsonObject
        val auctionSide = side(obj, "auction", "auctions", "ah")
        val orderSide = side(obj, "order", "orders", "shop")
        val auctionPoints = pointsAt(historyObj, "auction", "auctions")
            .ifEmpty { auctionSide.points }
        val orderPoints = pointsAt(historyObj, "order", "orders")
            .ifEmpty { orderSide.points }
        val auctionSeries = auctionPoints.map { it.price }
            .ifEmpty { seriesAt(obj, "auctions.history", "auctionHistory", "auction.history", "ah.history", "sparkAuction") }
            .ifEmpty { seriesAt(historyObj, "auctions", "auction", "ah", "auctions.history") }
        val orderSeries = orderPoints.map { it.price }
            .ifEmpty { seriesAt(obj, "orders.history", "orderHistory", "order.history", "shop.history", "sparkOrder") }
            .ifEmpty { seriesAt(historyObj, "orders", "order", "shop", "orders.history") }
        val series = pointsAt(historyObj, "points").map { it.price }
            .ifEmpty { series(history) }
            .ifEmpty { series(obj.get("history")) }
            .ifEmpty { series(obj.get("prices")) }
            .ifEmpty { series(obj.get("points")) }
            .ifEmpty { series(obj.get("spark")) }
            .ifEmpty { auctionSeries }
            .ifEmpty { orderSeries }
        val last = JsonView.number(
            obj, "last", "lastPrice", "price", "close", "unitPrice", "buyPrice", "sellPrice", "value"
        ) ?: series.lastOrNull()
        val average = auctionSide.average ?: JsonView.number(
            obj, "avg", "average", "avgPrice", "averagePrice", "mean", "median", "medianPrice"
        ) ?: series.takeIf { it.isNotEmpty() }?.average()
        val orderAverage = orderSide.average ?: nestedAverage(
            obj,
            "avgOrder", "averageOrder", "orderAvg", "orderAverage", "avgOrderPrice", "orderPrice",
            "orders.avg", "orders.average", "order.avg", "order.average", "shop.avg", "shop.average"
        ) ?: orderSide.lowest
        val auctionAverage = auctionSide.average ?: nestedAverage(
            obj,
            "avgAuction", "averageAuction", "auctionAvg", "auctionAverage", "avgAuctionPrice", "ahAvg",
            "auctions.avg", "auctions.average", "auction.avg", "auction.average", "ah.avg", "ah.average"
        ) ?: auctionSide.lowest
        val min = auctionSide.lowest ?: JsonView.number(obj, "min", "minPrice", "low") ?: series.minOrNull()
        val max = JsonView.number(obj, "max", "maxPrice", "high") ?: series.maxOrNull()
        val volume = JsonView.number(obj, "volume", "vol", "quantity", "count", "amount", "qty")
        val auctionCount = auctionSide.listingCount
            ?: JsonView.number(obj, "auctionCount", "auctions.count", "ah.count", "auctions")?.toInt()
        val orderCount = orderSide.listingCount
            ?: JsonView.number(obj, "orderCount", "orders.count", "shop.count", "orders")?.toInt()
        val count = auctionCount?.takeIf { it > 0 }
            ?: orderCount?.takeIf { it > 0 }
            ?: JsonView.number(obj, "count", "listings", "stock", "amount", "qty", "quantity")?.toInt()
            ?: volume?.toInt()
        val changePercent = auctionSide.changePct
            ?: JsonView.number(obj, "changePercent", "change_percent", "percent", "pct", "deltaPercent")
                ?.let(::asPercent)
            ?: inferredChange(series)
        val auctionChange = auctionSide.changePct
            ?: JsonView.number(obj, "auctionChange", "auctions.changePercent", "auctions.change", "ah.change")
                ?.let(::asPercent)
            ?: inferredChange(auctionSeries)
        val orderChange = orderSide.changePct
            ?: JsonView.number(obj, "orderChange", "orders.changePercent", "orders.change", "shop.change")
                ?.let(::asPercent)
            ?: inferredChange(orderSeries)
        val name = JsonView.str(
            obj,
            "displayName", "display_name", "itemName", "title",
            "item.displayName", "item.name", "label"
        ) ?: JsonView.str(JsonView.child(obj, "item", "product"), "displayName", "itemName", "title", "name")
            ?: "Eintrag"
        val minecraftId = JsonView.str(obj, "minecraftId", "minecraft_id", "item.minecraftId")
            ?: JsonView.str(JsonView.child(obj, "item", "product"), "minecraftId", "minecraft_id")
        val id = JsonView.str(obj, "id", "itemId", "item_id", "slug", "uuid") ?: minecraftId
        return MarketQuote(
            id = id,
            name = name,
            minecraftId = minecraftId,
            last = last,
            average = average,
            orderAverage = orderAverage,
            auctionAverage = auctionAverage,
            min = min,
            max = max,
            volume = volume,
            series = series,
            changePercent = changePercent,
            count = count,
            auctionCount = auctionCount,
            orderCount = orderCount,
            auctionChange = auctionChange,
            orderChange = orderChange,
            auctionSeries = auctionSeries,
            orderSeries = orderSeries,
            auctionLowest = auctionSide.lowest,
            orderLowest = orderSide.lowest,
            auctionPoints = auctionPoints,
            orderPoints = orderPoints
        ).also(MarketPriceCache::put)
    }

    fun series(element: JsonElement?): List<Double> {
        val values = JsonView.numbers(element)
        return if (values.size >= 2) values else emptyList()
    }

    fun seriesAt(obj: JsonObject?, vararg keys: String): List<Double> {
        if (obj == null) return emptyList()
        for (key in keys) {
            val found = series(JsonView.lookup(obj, key))
            if (found.isNotEmpty()) return found
        }
        return emptyList()
    }

    fun points(element: JsonElement?): List<ChartPoint> {
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> (element.get("points") as? JsonArray) ?: return emptyList()
            else -> return emptyList()
        }
        return array.mapNotNull { child ->
            val obj = child as? JsonObject ?: return@mapNotNull JsonView.parseNumber(child)?.let { ChartPoint(0L, it) }
            val price = JsonView.number(obj, "price", "value", "y", "close") ?: return@mapNotNull null
            ChartPoint(
                timeMs = parseTime(JsonView.str(obj, "t", "time", "timestamp", "at")),
                price = price,
                listingCount = JsonView.number(obj, "listingCount", "count")?.toInt() ?: 0
            )
        }
    }

    fun pointsAt(obj: JsonObject?, vararg keys: String): List<ChartPoint> {
        if (obj == null) return emptyList()
        for (key in keys) {
            val found = points(JsonView.lookup(obj, key))
            if (found.isNotEmpty()) return found
        }
        return emptyList()
    }

    fun sparklineValues(element: JsonElement?): List<Float> {
        if (element is JsonArray && element.size() > 0) {
            val first = element.first()
            if (first is JsonObject) {
                return element.mapNotNull { child ->
                    val obj = child as? JsonObject ?: return@mapNotNull null
                    JsonView.number(obj, "price", "v", "value", "y", "count")
                }.map { it.toFloat() }
            }
        }
        return JsonView.numbers(element).map { it.toFloat() }
    }

    fun hoverIndex(count: Int, x: Int, width: Int, mouseX: Double): Int? {
        if (count < 2 || mouseX < x || mouseX >= x + width) return null
        val t = ((mouseX - x) / width.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        return (t * (count - 1)).roundToInt().coerceIn(0, count - 1)
    }

    fun formatCardPrice(value: Double?): String? {
        if (value == null || !value.isFinite()) return null
        val abs = abs(value)
        val decimals = if (kotlin.math.abs(abs - kotlin.math.floor(abs)) < 0.0005) 0 else 2
        val formatted = java.text.NumberFormat.getNumberInstance(Locale.GERMANY).apply {
            maximumFractionDigits = decimals
            minimumFractionDigits = decimals
        }.format(abs)
        val sign = if (value < 0) "-" else ""
        return "$sign$$formatted"
    }

    fun formatCount(count: Int?): String? {
        if (count == null || count <= 0) return null
        return "$count Stück"
    }

    fun formatWindow(count: Int?): String = when {
        count == null -> "— im Fenster"
        count <= 0 -> "0 im Fenster"
        else -> "$count im Fenster"
    }

    fun formatGrouped(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return java.text.NumberFormat.getIntegerInstance(Locale.GERMANY).format(kotlin.math.round(value))
    }

    fun formatAgo(timeMs: Long): String {
        if (timeMs <= 0L) return ""
        val ago = (System.currentTimeMillis() - timeMs).coerceAtLeast(0L)
        return when {
            ago < 8_000L -> "gerade eben"
            ago < 60_000L -> "vor ${ago / 1000}s"
            ago < 3_600_000L -> "vor ${ago / 60_000}m"
            ago < 86_400_000L -> "vor ${ago / 3_600_000}h"
            else -> "vor ${ago / 86_400_000}d"
        }
    }

    fun formatChange(value: Double?): String? {
        if (value == null || !value.isFinite()) return null
        val sign = if (value > 0) "+" else ""
        return "$sign%.1f%%".format(Locale.US, value)
    }

    fun formatDelta(value: Double?): String? {
        if (value == null || !value.isFinite()) return null
        val sign = if (value > 0) "+" else ""
        return sign + formatGrouped(value)
    }

    fun changeColor(value: Double?): Int = when {
        value == null -> 0xFF7A7A7A.toInt()
        value > 0.05 -> 0xFF4CAF7A.toInt()
        value < -0.05 -> 0xFFC84A4A.toInt()
        else -> 0xFF7A7A7A.toInt()
    }

    fun asPercent(value: Double?): Double? {
        if (value == null || !value.isFinite()) return null
        return if (abs(value) <= 1.0) value * 100.0 else value
    }

    private fun inferredChange(series: List<Double>): Double? {
        if (series.size < 2) return null
        val first = series.first()
        val last = series.last()
        if (first == 0.0) return null
        return (last - first) / first * 100.0
    }

    fun formatMoney(value: Double?): String? {
        if (value == null || value.isNaN() || value.isInfinite()) return null
        val sign = if (value < 0) "-" else ""
        val abs = abs(value)
        val body = when {
            abs >= 1_000_000 -> "%.1f Mio".format(Locale.US, abs / 1_000_000.0)
            abs >= 10_000 -> "%.1fk".format(Locale.US, abs / 1000.0)
            abs >= 100 -> "%.0f".format(Locale.US, abs)
            abs >= 10 -> "%.1f".format(Locale.US, abs)
            else -> "%.2f".format(Locale.US, abs)
        }
        return "$sign$body $"
    }

    fun matchesSource(obj: JsonObject, source: String): Boolean {
        val haystack = listOf("source", "type", "kind", "market", "channel", "origin")
            .mapNotNull { JsonView.str(obj, it)?.lowercase() }
            .joinToString(" ")
        if (haystack.isBlank()) return false
        return when (source.lowercase()) {
            "auction", "auctions", "ah" ->
                listOf("auction", "auktion", "ah", "bid", "bidding").any { it in haystack }
            "order", "orders", "shop" ->
                listOf("order", "shop", "store", "buyorder", "sellorder", "listing").any { it in haystack }
            else -> source.lowercase() in haystack
        }
    }

    fun itemId(obj: JsonObject): String? =
        JsonView.str(obj, "id", "itemId", "item_id", "uuid") ?: JsonView.str(obj, "minecraftId")

    fun parseTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrElse {
            raw.toLongOrNull() ?: 0L
        }
    }

    private fun side(obj: JsonObject, vararg keys: String): QuoteSide {
        for (key in keys) {
            val child = obj.get(key) as? JsonObject ?: continue
            val average = JsonView.number(child, "average", "avg", "avgPrice", "mean", "median")
            val changePct = JsonView.number(child, "changePct", "changePercent", "change")
            return QuoteSide(
                average = average,
                changePct = changePct,
                listingCount = JsonView.number(child, "listingCount", "count")?.toInt(),
                lowest = JsonView.number(child, "lowest", "min", "low"),
                points = points(child.get("sparkline")).ifEmpty { points(child.get("points")) }
            )
        }
        return QuoteSide()
    }

    private fun nestedAverage(obj: JsonObject, vararg keys: String): Double? =
        JsonView.number(obj, *keys)

    private data class QuoteSide(
        val average: Double? = null,
        val changePct: Double? = null,
        val listingCount: Int? = null,
        val lowest: Double? = null,
        val points: List<ChartPoint> = emptyList()
    )
}
