package dev.henny.hugoutils.api

import com.google.gson.JsonObject

data class ArbitrageDeal(
    val id: String,
    val itemId: String?,
    val name: String,
    val minecraftId: String?,
    val buySource: String,
    val sellSource: String,
    val buyPrice: Double?,
    val sellPrice: Double?,
    val buyAverage: Double?,
    val sellAverage: Double?,
    val buyLowest: Double?,
    val sellLowest: Double?,
    val buyCount: Int?,
    val sellCount: Int?,
    val buyVolume: String?,
    val sellVolume: String?,
    val profit: Double?,
    val profitPct: Double?,
    val age: String
)

object MarketArbitrage {
    fun parse(body: JsonObject?): List<ArbitrageDeal> {
        if (body == null) return emptyList()
        val rows = JsonView.objectsNamed(body, "opportunities", "arbitrage", "deals", "items")
            .ifEmpty { JsonView.objects(body) }
        return rows.map(::from).filter { it.name.isNotBlank() }
    }

    fun from(obj: JsonObject): ArbitrageDeal {
        val item = JsonView.child(obj, "item", "product", "stack")
        val buy = JsonView.child(obj, "buy", "from", "auction", "purchase", "ah") ?: obj
        val sell = JsonView.child(obj, "sell", "to", "order", "sale") ?: obj
        val itemId = JsonView.str(obj, "itemId", "item_id")
            ?: JsonView.str(item, "id", "itemId", "item_id")
            ?: JsonView.str(obj, "item", "id")
        val name = JsonView.str(obj, "displayName", "display_name", "itemName", "title", "name", "item.displayName")
            ?: JsonView.str(item, "displayName", "display_name", "itemName", "title", "name")
            ?: itemId?.let(MarketPriceCache::quoteForId)?.name?.takeUnless { it.isBlank() || it == "Item" || it == "Eintrag" }
            ?: "Item"
        val minecraftId = JsonView.str(obj, "minecraftId", "minecraft_id", "identityKey", "identity_key", "registry_id", "item.minecraftId")
            ?: JsonView.str(item, "minecraftId", "minecraft_id", "identityKey", "identity_key")
        val buyPrice = JsonView.number(buy, "price", "unitPrice", "buyPrice", "median", "value", "ah_price", "ahPrice")
            ?: JsonView.number(obj, "buyPrice", "buy.price", "ah_price", "ahPrice", "auctionPrice")
        val sellPrice = JsonView.number(sell, "price", "unitPrice", "sellPrice", "median", "value", "order_price", "orderPrice")
            ?: JsonView.number(obj, "sellPrice", "sell.price", "order_price", "orderPrice")
        val profit = JsonView.number(obj, "profit", "spread", "delta", "profitPerUnit", "profit.unit")
            ?: if (buyPrice != null && sellPrice != null) sellPrice - buyPrice else null
        val profitPct = JsonView.number(obj, "profitPct", "profit_pct", "profitPercent", "spreadPct", "changePct", "percent")
            ?: if (buyPrice != null && buyPrice != 0.0 && profit != null) (profit / buyPrice) * 100.0 else null
        val created = JsonView.str(obj, "updatedAt", "createdAt", "last_seen_at", "lastSeenAt", "age", "timeAgo", "time") ?: ""
        val parsed = MarketStats.parseTime(created)
        return ArbitrageDeal(
            id = JsonView.str(obj, "id") ?: listOf(itemId.orEmpty(), name, buyPrice, sellPrice).joinToString(":"),
            itemId = itemId,
            name = name,
            minecraftId = minecraftId,
            buySource = sourceLabel(JsonView.str(buy, "source", "type", "market") ?: JsonView.str(obj, "buySource", "fromSource") ?: "auction"),
            sellSource = sourceLabel(JsonView.str(sell, "source", "type", "market") ?: JsonView.str(obj, "sellSource", "toSource") ?: "order"),
            buyPrice = buyPrice,
            sellPrice = sellPrice,
            buyAverage = JsonView.number(buy, "average", "avg", "mean")
                ?: JsonView.number(obj, "ah_avg", "ahAvg", "buyAverage"),
            sellAverage = JsonView.number(sell, "average", "avg", "mean")
                ?: JsonView.number(obj, "order_avg", "orderAvg", "sellAverage"),
            buyLowest = JsonView.number(buy, "lowest", "min", "low")
                ?: JsonView.number(obj, "ah_min", "ahMin", "buyLowest"),
            sellLowest = JsonView.number(sell, "lowest", "min", "low")
                ?: JsonView.number(obj, "order_min", "orderMin", "sellLowest"),
            buyCount = JsonView.number(buy, "listingCount", "count", "listings")?.toInt()
                ?: JsonView.number(obj, "buyCount", "ah_count", "ahCount")?.toInt(),
            sellCount = JsonView.number(sell, "listingCount", "count", "listings")?.toInt()
                ?: JsonView.number(obj, "sellCount", "order_count", "orderCount")?.toInt(),
            buyVolume = volume(buy) ?: volume(obj, "buyVolume", "ah_volume", "ahVolume"),
            sellVolume = volume(sell) ?: volume(obj, "sellVolume", "order_volume", "orderVolume"),
            profit = profit,
            profitPct = MarketStats.asPercent(profitPct) ?: profitPct,
            age = MarketStats.formatAgo(parsed).ifBlank { created }
        )
    }

    private fun volume(side: JsonObject, vararg keys: String): String? {
        val amount = JsonView.number(side, *keys.ifEmpty { arrayOf("volume", "amount", "qty", "quantity", "times") })
        return amount?.let { "${MarketStats.formatGrouped(it)}×" }
    }

    private fun sourceLabel(raw: String): String {
        val key = raw.lowercase()
        return when {
            "auction" in key || key == "ah" -> "Auktion"
            "order" in key || "shop" in key -> "Order"
            else -> raw.replaceFirstChar { it.uppercase() }
        }
    }
}
