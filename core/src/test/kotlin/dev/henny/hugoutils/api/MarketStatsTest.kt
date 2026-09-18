package dev.henny.hugoutils.api

import com.google.gson.JsonParser
import dev.henny.hugoutils.client.config.PriceAlert
import dev.henny.hugoutils.client.ui.MarketNames
import dev.henny.hugoutils.client.ui.PriceAlertPopup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarketStatsTest {
    @Test
    fun `client item schema fills both markets and sparklines`() {
        val item = JsonParser.parseString(
            """
            {
              "id":"11111111-1111-1111-1111-111111111111",
              "displayName":"Observer",
              "minecraftId":"minecraft:observer",
              "identityKey":"minecraft:observer",
              "auction":{
                "average":50.0,
                "changePct":12.5,
                "listingCount":8,
                "lowest":40.0,
                "sparkline":[
                  {"t":"2026-09-17T15:00:00.000Z","price":48.0},
                  {"t":"2026-09-17T15:30:00.000Z","price":50.0}
                ]
              },
              "order":{
                "average":12.0,
                "changePct":-3.0,
                "listingCount":2,
                "lowest":10.0,
                "sparkline":[
                  {"t":"2026-09-17T15:00:00.000Z","price":11.0},
                  {"t":"2026-09-17T15:30:00.000Z","price":12.0}
                ]
              }
            }
            """.trimIndent()
        ).asJsonObject
        val quote = MarketStats.quote(item)
        assertEquals("Observer", quote.name)
        assertEquals("minecraft:observer", quote.minecraftId)
        assertEquals(50.0, quote.auctionAverage)
        assertEquals(12.0, quote.orderAverage)
        assertEquals(8, quote.auctionCount)
        assertEquals(12.5, quote.auctionChange)
        assertEquals(listOf(48.0, 50.0), quote.auctionPoints.map { it.price })
        assertEquals("$50", quote.cardPrice())
        assertEquals("8 Stück", quote.cardCount())
        assertEquals("+12.5%", quote.changeLabel())
        assertTrue(quote.tooltip().contains("Ø Order"))
        assertTrue(quote.tooltip().contains("Ø Auktion"))
    }

    @Test
    fun `history object supplies timestamped dual series`() {
        val item = JsonParser.parseString(
            """{"id":"obs","displayName":"Observer","minecraftId":"minecraft:observer","auction":{"average":50,"changePct":0,"listingCount":1,"lowest":50,"sparkline":[]},"order":{"average":null,"changePct":null,"listingCount":0,"lowest":null,"sparkline":[]}}"""
        ).asJsonObject
        val history = JsonParser.parseString(
            """
            {
              "itemId":"obs",
              "range":"24h",
              "source":"auction",
              "points":[{"t":"2026-09-17T15:00:00.000Z","price":48.0,"listingCount":3}],
              "auction":[
                {"t":"2026-09-17T15:00:00.000Z","price":48.0,"listingCount":3},
                {"t":"2026-09-17T16:00:00.000Z","price":52.0,"listingCount":2}
              ],
              "order":[
                {"t":"2026-09-17T15:00:00.000Z","price":10.0,"listingCount":1},
                {"t":"2026-09-17T16:00:00.000Z","price":12.0,"listingCount":1}
              ]
            }
            """.trimIndent()
        ).asJsonObject
        val quote = MarketStats.quote(item, history)
        assertEquals(listOf(48.0, 52.0), quote.auctionChart.map { it.toDouble() })
        assertEquals(listOf(10.0, 12.0), quote.orderChart.map { it.toDouble() })
        assertTrue(quote.auctionPoints.first().timeMs > 0)
        assertEquals(1, MarketStats.hoverIndex(2, 0, 100, 80.0))
    }

    @Test
    fun `quote uses average and history series`() {
        val item = JsonParser.parseString(
            """{"id":"diamond","displayName":"Diamond","avgPrice":120,"min":90,"max":150,"last":110,"avgOrder":90,"avgAuction":150}"""
        ).asJsonObject
        val history = JsonParser.parseString("""{"points":[100,110,120,130]}""")
        val quote = MarketStats.quote(item, history)
        assertEquals("Diamond", quote.name)
        assertEquals(120.0, quote.average)
        assertEquals(90.0, quote.min)
        assertEquals(150.0, quote.max)
        assertEquals(listOf(100.0, 110.0, 120.0, 130.0), quote.series)
        assertTrue(quote.tooltip().contains("Ø Preis"))
        assertTrue(quote.tooltip().contains("Ø Order"))
        assertTrue(quote.tooltip().contains("Ø Auktion"))
        assertEquals("90.0 $ / 150 $", quote.rowPrice())
    }

    @Test
    fun `missing average falls back to history mean`() {
        val item = JsonParser.parseString("""{"name":"Gold","history":[{"price":10},{"price":30}]}""").asJsonObject
        val quote = MarketStats.quote(item)
        assertEquals(20.0, quote.average)
        assertEquals(10.0, quote.min)
        assertEquals(30.0, quote.max)
    }

    @Test
    fun `nested order and auction averages are preferred`() {
        val item = JsonParser.parseString(
            """{"name":"Iron","orders":{"avg":12},"auctions":{"average":40}}"""
        ).asJsonObject
        val quote = MarketStats.quote(item)
        assertEquals(12.0, quote.orderAverage)
        assertEquals(40.0, quote.auctionAverage)
        assertTrue(quote.tooltip().contains("Ø Order"))
        assertTrue(MarketStats.matchesSource(
            JsonParser.parseString("""{"source":"auction"}""").asJsonObject,
            "auction"
        ))
        assertTrue(MarketStats.matchesSource(
            JsonParser.parseString("""{"type":"shop_order"}""").asJsonObject,
            "order"
        ))
    }

    @Test
    fun `card prices and change labels match the market grid`() {
        assertEquals("$12.500.000", MarketStats.formatCardPrice(12_500_000.0))
        assertEquals("$480,28", MarketStats.formatCardPrice(480.28))
        assertEquals("61 Stück", MarketStats.formatCount(61))
        assertEquals("+1.558", MarketStats.formatDelta(1558.0))
        assertEquals("8 im Fenster", MarketStats.formatWindow(8))
        assertEquals("+33.3%", MarketStats.formatChange(33.3))
        assertEquals("-3.8%", MarketStats.formatChange(-3.8))
        assertEquals(33.3, MarketStats.asPercent(0.333)!!, 0.001)
        val quote = MarketStats.quote(
            JsonParser.parseString("""{"name":"Glass","avgAuction":480.28,"count":690,"changePercent":0.921}""").asJsonObject
        )
        assertEquals("$480,28", quote.cardPrice())
        assertEquals("690 Stück", quote.cardCount())
        assertEquals("+92.1%", quote.changeLabel())
    }

    @Test
    fun `search hits unwrap from the documented envelope`() {
        val json = JsonParser.parseString(
            """{"q":"Observer","hits":[{"type":"item","id":"uuid-1","title":"Observer","minecraftId":"minecraft:observer"}]}"""
        )
        val hits = JsonView.objects(json)
        assertEquals(1, hits.size)
        assertEquals("Observer", JsonView.str(hits.first(), "title"))
        assertEquals("uuid-1", JsonView.str(hits.first(), "id"))
    }

    @Test
    fun `nested listing item names are read`() {
        val listing = JsonParser.parseString(
            """
            {
              "id":"l1",
              "item":{"displayName":"TNT","minecraftId":"minecraft:tnt"},
              "seller":"Cretorius",
              "amount":64,
              "unitPrice":781.25
            }
            """.trimIndent()
        ).asJsonObject
        val parsed = dev.henny.hugoutils.client.ui.MarketListings.from(listing)
        assertEquals("TNT", parsed.name)
        assertEquals("minecraft:tnt", parsed.minecraftId)
        assertEquals("Cretorius", parsed.seller)
    }

    @Test
    fun `listing item id string still hydrates from cache`() {
        MarketPriceCache.put(
            MarketQuote(
                id = "uuid-tnt",
                name = "TNT",
                minecraftId = "minecraft:tnt"
            )
        )
        val listing = JsonParser.parseString(
            """{"id":"l2","item":"uuid-tnt","seller":"Icedout","amount":1,"unitPrice":7000000}"""
        ).asJsonObject
        val parsed = MarketNames.enrich(dev.henny.hugoutils.client.ui.MarketListings.from(listing))
        assertEquals("TNT", parsed.name)
        assertEquals("uuid-tnt", parsed.itemId)
    }

    @Test
    fun `arbitrage rows expose buy sell and profit`() {
        val body = JsonParser.parseString(
            """
            {
              "opportunities":[{
                "id":"a1",
                "item":{"id":"elytra","displayName":"Elytra","minecraftId":"minecraft:elytra"},
                "buy":{"source":"auction","price":1,"average":25.0,"listingCount":8,"volume":1133},
                "sell":{"source":"order","price":100,"lowest":80,"average":90,"listingCount":4,"volume":4},
                "profit":99,
                "profitPct":9900
              }]
            }
            """.trimIndent()
        ).asJsonObject
        val deals = MarketArbitrage.parse(body)
        assertEquals(1, deals.size)
        assertEquals("Elytra", deals.first().name)
        assertEquals("Auktion", deals.first().buySource)
        assertEquals("Order", deals.first().sellSource)
        assertEquals(1.0, deals.first().buyPrice)
        assertEquals(100.0, deals.first().sellPrice)
        assertEquals(99.0, deals.first().profit)
        assertEquals(9900.0, deals.first().profitPct)
    }

    @Test
    fun `arbitrage rows map auction buy and order sell`() {
        val body = JsonParser.parseString(
            """
            {
              "rows":[{
                "id":"elytra-id",
                "registry_id":"minecraft:elytra",
                "identity_key":"minecraft:elytra",
                "display_name":"Elytra",
                "ah_price":"10",
                "ah_min":"8",
                "ah_avg":"11",
                "order_price":"40",
                "order_min":"35",
                "order_avg":"42",
                "profit":"30",
                "profit_pct":"300",
                "ah_count":4,
                "order_count":8,
                "ah_volume":4,
                "order_volume":1133,
                "last_seen_at":"2026-09-18T12:00:00.000Z"
              }]
            }
            """.trimIndent()
        ).asJsonObject
        val deals = MarketArbitrage.parse(body)
        assertEquals(1, deals.size)
        val deal = deals.first()
        assertEquals("Elytra", deal.name)
        assertEquals("elytra-id", deal.itemId)
        assertEquals("Auktion", deal.buySource)
        assertEquals("Order", deal.sellSource)
        assertEquals(10.0, deal.buyPrice)
        assertEquals(40.0, deal.sellPrice)
        assertEquals(8.0, deal.buyLowest)
        assertEquals(35.0, deal.sellLowest)
        assertEquals(11.0, deal.buyAverage)
        assertEquals(42.0, deal.sellAverage)
        assertEquals(4, deal.buyCount)
        assertEquals(8, deal.sellCount)
        assertEquals("4×", deal.buyVolume)
        assertEquals("1.133×", deal.sellVolume)
        assertEquals(30.0, deal.profit)
        assertEquals(300.0, deal.profitPct)
    }

    @Test
    fun `quote falls back to lowest when average is missing`() {
        val item = JsonParser.parseString(
            """{"id":"obs","displayName":"Observer","minecraftId":"minecraft:observer","auction":{"lowest":50},"order":{"lowest":12}}"""
        ).asJsonObject
        val quote = MarketStats.quote(item)
        assertEquals(50.0, quote.auctionAverage)
        assertEquals(12.0, quote.orderAverage)
        assertTrue(quote.tooltip().contains("Ø Auktion"))
        assertTrue(quote.tooltip().contains("Ø Order"))
        assertEquals(12.0, quote.priceFor("any", false))
        assertEquals(50.0, quote.priceFor("any", true))
    }

    @Test
    fun `price alert fires at or below target`() {
        val alert = PriceAlert().apply {
            itemId = "obs"
            target = 60.0
            above = false
            source = "auction"
        }
        val quote = MarketQuote(id = "obs", name = "Observer", auctionAverage = 50.0)
        assertTrue(PriceAlertWatcher.triggered(alert, quote))
        alert.above = true
        assertFalse(PriceAlertWatcher.triggered(alert, quote))
        alert.target = 40.0
        assertTrue(PriceAlertWatcher.triggered(alert, quote))
    }

    @Test
    fun `price alert popup parses german amounts`() {
        assertEquals(1_500_000.0, PriceAlertPopup.parsePrice("1.500.000"))
        assertEquals(1_500.0, PriceAlertPopup.parsePrice("1,5k"))
        assertEquals(2_000_000.0, PriceAlertPopup.parsePrice("2m"))
    }
}
