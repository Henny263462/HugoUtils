package dev.henny.hugoutils.api

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarketPriceCacheTest {
    @AfterEach
    fun tearDown() {
        MarketPriceCache.clear()
    }

    @Test
    fun `drops quotes once the cache is full`() {
        MarketPriceCache.clear()
        repeat(MarketPriceCache.MAX_QUOTES + 80) { index ->
            MarketPriceCache.put(
                MarketQuote(
                    id = "item-$index",
                    name = "Item $index",
                    minecraftId = "minecraft:item_$index"
                )
            )
        }
        assertTrue(MarketPriceCache.sizeForTests() <= MarketPriceCache.MAX_KEYS)
        assertTrue(MarketPriceCache.sizeForTests() <= MarketPriceCache.MAX_QUOTES * 5)
    }
}
