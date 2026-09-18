package dev.henny.hugoutils.client.ui

import com.google.gson.JsonObject
import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.api.JsonView
import dev.henny.hugoutils.api.MarketPriceCache
import dev.henny.hugoutils.api.MarketQuote
import dev.henny.hugoutils.api.MarketStats
import dev.henny.hugoutils.ui.AnimatedFloat
import dev.henny.hugoutils.ui.Easing
import dev.henny.hugoutils.ui.UiFrame
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object MarketLinks {
    var openPage: (String) -> Unit = {}

    fun openItem(id: String) {
        openPage(ConfigCategory.MARKET_ITEMS.id)
        ConfigPages.forCategory(ConfigCategory.MARKET_ITEMS)
            .filterIsInstance<MarketConfigPage>()
            .firstOrNull()
            ?.openItemId(id)
    }

    fun openItemsSearch(query: String) {
        openPage(ConfigCategory.MARKET_ITEMS.id)
        ConfigPages.forCategory(ConfigCategory.MARKET_ITEMS)
            .filterIsInstance<MarketConfigPage>()
            .firstOrNull()
            ?.applySearch(query)
    }

    fun openPlayers() {
        openPage(ConfigCategory.MARKET_PLAYERS.id)
    }
}

data class MarketListing(
    val id: String,
    val itemId: String?,
    val name: String,
    val minecraftId: String?,
    val seller: String,
    val qty: String,
    val age: String,
    val price: String
)

object MarketListings {
    fun parse(body: JsonObject?, source: String, limit: Int): List<MarketListing> {
        if (body == null) return emptyList()
        val named = JsonView.objectsNamed(body, source, "${source}s", "listings", "items")
        val raw = named.ifEmpty { JsonView.objects(body) }
        val filtered = raw.filter { MarketStats.matchesSource(it, source) }.ifEmpty { raw }
        return filtered.take(limit).map(::from)
    }

    fun from(obj: JsonObject): MarketListing {
        val nested = JsonView.child(obj, "item", "product", "stack")
        val itemId = JsonView.str(obj, "itemId", "item_id")
            ?: JsonView.str(nested, "id", "itemId", "item_id", "uuid")
            ?: JsonView.str(obj, "item")
        val minecraftId = JsonView.str(obj, "minecraftId", "minecraft_id", "identityKey", "item.minecraftId", "item.identityKey")
            ?: JsonView.str(nested, "minecraftId", "minecraft_id", "identityKey")
        val cached = itemId?.let(MarketPriceCache::quoteForId)
            ?: minecraftId?.let(MarketPriceCache::quoteForId)
        val created = JsonView.str(obj, "createdAt", "updatedAt", "age", "timeAgo", "time") ?: ""
        val parsed = MarketStats.parseTime(created)
        val age = MarketStats.formatAgo(parsed).ifBlank { created }
        val qtyNumber = JsonView.number(obj, "amount", "quantity", "qty", "count")
        val name = JsonView.str(
            obj,
            "displayName", "itemName", "itemDisplayName", "title", "item.displayName"
        ) ?: JsonView.str(nested, "displayName", "itemName", "title", "name")
            ?: cached?.name?.takeUnless(MarketNames::isPlaceholder)
            ?: MarketNames.prettyId(minecraftId)
            ?: "Item"
        val seller = JsonView.str(obj, "seller", "player", "playerName", "owner") ?: "Spieler"
        val priceNumber = JsonView.number(obj, "unitPrice", "price", "buyPrice", "sellPrice", "totalPrice")
        return MarketListing(
            id = JsonView.str(obj, "id")
                ?: listOf(itemId.orEmpty(), seller, qtyNumber?.toString().orEmpty(), created, priceNumber?.toString().orEmpty())
                    .joinToString(":"),
            itemId = itemId,
            name = name,
            minecraftId = minecraftId ?: cached?.minecraftId,
            seller = seller,
            qty = qtyNumber?.let { "${it.toInt()} Stück" } ?: "1 Stück",
            age = age,
            price = MarketStats.formatCardPrice(priceNumber) ?: "—"
        )
    }
}

object MarketNames {
    fun isPlaceholder(name: String?): Boolean {
        val value = name?.trim().orEmpty()
        return value.isEmpty() || value.equals("Item", true) || value.equals("Eintrag", true)
    }

    fun prettyId(minecraftId: String?): String? {
        val raw = minecraftId
            ?.substringAfter(':')
            ?.substringBefore('[')
            ?.replace('_', ' ')
            ?.trim()
            .orEmpty()
        if (raw.isBlank() || raw.length > 40) return null
        if (raw.all { it.isDigit() || it == '-' }) return null
        return raw.split(' ').filter { it.isNotBlank() }.joinToString(" ") { part ->
            part.replaceFirstChar { ch -> ch.titlecase() }
        }
    }

    fun enrich(listing: MarketListing): MarketListing {
        val cached = listing.itemId?.let(MarketPriceCache::quoteForId)
            ?: listing.minecraftId?.let(MarketPriceCache::quoteForId)
        val name = listing.name.takeUnless(::isPlaceholder)
            ?: cached?.name?.takeUnless(::isPlaceholder)
            ?: prettyId(listing.minecraftId ?: cached?.minecraftId)
            ?: listing.name
        return listing.copy(
            name = name,
            minecraftId = listing.minecraftId ?: cached?.minecraftId
        )
    }

    fun resolve(listings: List<MarketListing>, onDone: (List<MarketListing>) -> Unit) {
        val first = listings.map(::enrich)
        val missing = first.mapNotNull { it.itemId }
            .filter { id -> isPlaceholder(MarketPriceCache.quoteForId(id)?.name) }
            .distinct()
            .take(20)
        if (missing.isEmpty()) {
            onDone(first)
            return
        }
        ClientJobs.submit({ _, _ -> onDone(listings.map(::enrich)) }) {
            missing.forEach { id ->
                runCatching { MarketStats.quote(ClientApi.item(id)) }
            }
            ""
        }
    }
}

object MarketStacks {
    private val cache = object : LinkedHashMap<String, ItemStack?>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ItemStack?>): Boolean =
            size > MAX_STACKS
    }

    fun of(quote: MarketQuote): ItemStack? = ofIds(quote.minecraftId, quote.id, quote.name)

    fun of(listing: MarketListing): ItemStack? = ofIds(listing.minecraftId, listing.itemId, listing.name)

    @Synchronized
    fun ofIds(minecraftId: String?, id: String?, name: String): ItemStack? {
        val key = (minecraftId ?: id ?: name).lowercase()
        if (key.isBlank() || key == "item" || key == "eintrag") return null
        return cache.getOrPut(key) { resolve(minecraftId, id) }
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }

    fun label(minecraftId: String?): String? {
        val stack = ofIds(minecraftId, null, "")
        val fromItem = stack?.name?.string?.takeIf { it.isNotBlank() }
        if (fromItem != null) return fromItem
        val raw = minecraftId?.substringAfter(':')?.replace('_', ' ')?.trim().orEmpty()
        if (raw.isBlank()) return null
        return raw.split(' ').filter { it.isNotBlank() }.joinToString(" ") { part ->
            part.replaceFirstChar { ch -> ch.titlecase() }
        }
    }

    private fun resolve(minecraftId: String?, id: String?): ItemStack? {
        fromId(minecraftId)?.let { return it }
        fromId(id)?.let { return it }
        return null
    }

    private fun fromId(id: String?): ItemStack? {
        if (id.isNullOrBlank()) return null
        val raw = id.removePrefix("minecraft:").substringBefore('[').substringBefore(' ')
        val parsed = Identifier.tryParse(id) ?: Identifier.tryParse("minecraft:$raw") ?: return null
        if (!Registries.ITEM.containsId(parsed)) return null
        return ItemStack(Registries.ITEM.get(parsed))
    }

    private const val MAX_STACKS = 256
}

class SlidingFeed {
    private val rows = ArrayList<Row>()

    fun accept(next: List<MarketListing>, bounce: Boolean) {
        val previous = rows.associateBy { it.listing.id }
        val firstFill = rows.isEmpty()
        val rebuilt = ArrayList<Row>(next.size)
        next.forEachIndexed { index, listing ->
            val existing = previous[listing.id]
            if (existing != null) {
                existing.listing = listing
                rebuilt += existing
            } else {
                val bouncing = if (firstFill) bounce else true
                val enter = AnimatedFloat(
                    0f,
                    if (bouncing) .48f else .22f,
                    if (bouncing) Easing.EASE_OUT_BOUNCE else Easing.EASE_OUT
                )
                enter.snapTo(0f)
                enter.animateTo(1f, delaySeconds = if (firstFill) index * 0.035f else 0f)
                rebuilt += Row(listing, enter)
            }
        }
        rows.clear()
        rows.addAll(rebuilt)
    }

    fun clear() {
        rows.clear()
    }

    fun update() {
        rows.forEach { it.enter.update(UiFrame.deltaSeconds) }
    }

    fun visible(): List<Row> = rows

    fun offset(row: Row, height: Int): Int = ((1f - row.enter.value) * -height).toInt()

    class Row(var listing: MarketListing, val enter: AnimatedFloat)
}
