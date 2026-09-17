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
        val buy = JsonView.child(obj, "buy", "from", "order", "purchase") ?: obj
        val sell = JsonView.child(obj, "sell", "to", "auction", "sale") ?: obj
        val itemId = JsonView.str(obj, "itemId", "item_id")
            ?: JsonView.str(item, "id", "itemId", "item_id")
            ?: JsonView.str(obj, "item")
        val name = JsonView.str(obj, "displayName", "itemName", "title", "name", "item.displayName")
            ?: JsonView.str(item, "displayName", "itemName", "title", "name")
            ?: itemId?.let(MarketPriceCache::quoteForId)?.name?.takeUnless { it.isBlank() || it == "Item" || it == "Eintrag" }
            ?: "Item"
        val minecraftId = JsonView.str(obj, "minecraftId", "minecraft_id", "identityKey", "item.minecraftId")
            ?: JsonView.str(item, "minecraftId", "minecraft_id", "identityKey")
        val buyPrice = JsonView.number(buy, "price", "unitPrice", "buyPrice", "median", "value")
            ?: JsonView.number(obj, "buyPrice", "buy.price", "orderPrice")
        val sellPrice = JsonView.number(sell, "price", "unitPrice", "sellPrice", "median", "value")
            ?: JsonView.number(obj, "sellPrice", "sell.price", "auctionPrice")
        val profit = JsonView.number(obj, "profit", "spread", "delta", "profitPerUnit", "profit.unit")
            ?: if (buyPrice != null && sellPrice != null) sellPrice - buyPrice else null
        val profitPct = JsonView.number(obj, "profitPct", "profitPercent", "spreadPct", "changePct", "percent")
            ?: if (buyPrice != null && buyPrice != 0.0 && profit != null) (profit / buyPrice) * 100.0 else null
        val created = JsonView.str(obj, "updatedAt", "createdAt", "age", "timeAgo", "time") ?: ""
        val parsed = MarketStats.parseTime(created)
        return ArbitrageDeal(
            id = JsonView.str(obj, "id") ?: listOf(itemId.orEmpty(), name, buyPrice, sellPrice).joinToString(":"),
            itemId = itemId,
            name = name,
            minecraftId = minecraftId,
            buySource = sourceLabel(JsonView.str(buy, "source", "type", "market") ?: JsonView.str(obj, "buySource", "fromSource") ?: "order"),
            sellSource = sourceLabel(JsonView.str(sell, "source", "type", "market") ?: JsonView.str(obj, "sellSource", "toSource") ?: "auction"),
            buyPrice = buyPrice,
            sellPrice = sellPrice,
            buyAverage = JsonView.number(buy, "average", "avg", "mean"),
            sellAverage = JsonView.number(sell, "average", "avg", "mean"),
            buyLowest = JsonView.number(buy, "lowest", "min", "low"),
            sellLowest = JsonView.number(sell, "lowest", "min", "low"),
            buyCount = JsonView.number(buy, "listingCount", "count", "listings")?.toInt()
                ?: JsonView.number(obj, "buyCount", "orderCount")?.toInt(),
            sellCount = JsonView.number(sell, "listingCount", "count", "listings")?.toInt()
                ?: JsonView.number(obj, "sellCount", "auctionCount")?.toInt(),
            buyVolume = volume(buy) ?: JsonView.str(obj, "buyVolume"),
            sellVolume = volume(sell) ?: JsonView.str(obj, "sellVolume"),
            profit = profit,
            profitPct = MarketStats.asPercent(profitPct) ?: profitPct,
            age = MarketStats.formatAgo(parsed).ifBlank { created }
        )
    }

    private fun volume(side: JsonObject): String? {
        val amount = JsonView.number(side, "volume", "amount", "qty", "quantity", "times")
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
