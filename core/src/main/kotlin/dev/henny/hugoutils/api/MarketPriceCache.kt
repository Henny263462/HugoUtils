package dev.henny.hugoutils.api

import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import java.util.concurrent.ConcurrentHashMap

object MarketPriceCache {
    private val byKey = ConcurrentHashMap<String, MarketQuote>()
    private val inflight = ConcurrentHashMap.newKeySet<String>()
    private val attemptedAt = ConcurrentHashMap<String, Long>()

    fun put(quote: MarketQuote) {
        keysOf(quote).forEach { byKey[it] = quote }
        trimQuotes()
    }

    fun quoteFor(stack: ItemStack): MarketQuote? {
        if (stack.isEmpty) return null
        val id = Registries.ITEM.getId(stack.item).toString()
        val cached = quoteForId(id) ?: quoteForId(stack.name.string.lowercase())
        request(id)
        return cached
    }

    fun quoteForId(id: String): MarketQuote? {
        val key = id.trim().lowercase()
        if (key.isEmpty()) return null
        return byKey[key] ?: byKey[key.removePrefix("minecraft:")]
    }

    fun request(id: String) {
        if (!ClientSessionStore.hasToken()) return
        val key = id.trim().lowercase()
        if (key.isEmpty() || quoteForId(key) != null) return
        val now = System.currentTimeMillis()
        val last = attemptedAt[key] ?: 0L
        if (now - last < RETRY_MS) return
        if (!inflight.add(key)) return
        attemptedAt[key] = now
        trimAttempted()
        ClientJobs.submit({ _, _ -> inflight.remove(key) }) {
            lookup(key)
            ""
        }
    }

    fun prefetch() {
        if (!ClientSessionStore.hasToken()) return
        ClientJobs.submit({ _, _ -> }) {
            JsonView.objects(ClientApi.items(force = false, limit = 24)).forEach { MarketStats.quote(it) }
            ""
        }
    }

    fun clear() {
        byKey.clear()
        inflight.clear()
        attemptedAt.clear()
    }

    internal fun sizeForTests(): Int = byKey.size

    private fun lookup(id: String) {
        val queries = listOf(id, id.substringAfter(':')).distinct()
        for (query in queries) {
            val body = runCatching { ClientApi.items(query = query, limit = 8, force = false) }.getOrNull() ?: continue
            val items = JsonView.objectsNamed(body, "items").ifEmpty { JsonView.objects(body) }
            val match = items.firstOrNull { obj -> matches(obj, id) } ?: items.firstOrNull()
            if (match != null) {
                MarketStats.quote(match)
                return
            }
        }
        val search = runCatching { ClientApi.search(id.substringAfter(':'), force = false) }.getOrNull() ?: return
        JsonView.objects(search).firstOrNull { matches(it, id) }?.let { MarketStats.quote(it) }
    }

    private fun matches(obj: com.google.gson.JsonObject, id: String): Boolean {
        val needle = id.lowercase().removePrefix("minecraft:")
        val hay = listOf(
            JsonView.str(obj, "minecraftId", "identityKey", "id"),
            JsonView.str(JsonView.child(obj, "item"), "minecraftId", "identityKey", "id")
        ).mapNotNull { it?.lowercase()?.removePrefix("minecraft:") }
        return needle in hay
    }

    private fun keysOf(quote: MarketQuote): List<String> {
        val id = quote.id?.trim()?.lowercase()
        val minecraftId = quote.minecraftId?.trim()?.lowercase()
        return listOfNotNull(
            id,
            id?.removePrefix("minecraft:"),
            minecraftId,
            minecraftId?.removePrefix("minecraft:"),
            quote.name.lowercase().takeIf { it.isNotBlank() && it != "item" && it != "eintrag" }
        ).distinct()
    }

    private fun trimQuotes() {
        if (byKey.size <= MAX_QUOTES) return
        val unique = LinkedHashSet(byKey.values)
        val overflow = unique.size - MAX_QUOTES
        if (overflow > 0) {
            unique.take(overflow).forEach { stale ->
                keysOf(stale).forEach { key -> byKey.remove(key, stale) }
            }
        }
        if (byKey.size > MAX_KEYS) {
            byKey.keys.take(byKey.size - MAX_KEYS).forEach { byKey.remove(it) }
        }
    }

    private fun trimAttempted() {
        if (attemptedAt.size <= MAX_KEYS) return
        attemptedAt.keys.take(attemptedAt.size - MAX_KEYS).forEach { attemptedAt.remove(it) }
    }

    private const val RETRY_MS = 20_000L
    internal const val MAX_QUOTES = 128
    internal const val MAX_KEYS = 512
}
