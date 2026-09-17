package dev.henny.hugoutils.api

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object MarketItemTooltips {
    fun initialize() {
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            if (!ClientSessionStore.hasToken() || stack.isEmpty) return@register
            val quote = MarketPriceCache.quoteFor(stack)
            if (quote == null) {
                lines.add(Text.literal("Ø Market  lädt …").formatted(Formatting.DARK_GRAY))
                return@register
            }
            var added = false
            MarketStats.formatCardPrice(quote.orderAverage)?.let {
                lines.add(Text.literal("Ø Order  $it").formatted(Formatting.GRAY))
                added = true
            }
            MarketStats.formatCardPrice(quote.auctionAverage)?.let {
                lines.add(Text.literal("Ø Auktion  $it").formatted(Formatting.GRAY))
                added = true
            }
            if (!added) {
                MarketStats.formatCardPrice(quote.average ?: quote.last)?.let {
                    lines.add(Text.literal("Ø Market  $it").formatted(Formatting.GRAY))
                    added = true
                }
            }
            if (!added) {
                lines.add(Text.literal("Keine Market-Preise").formatted(Formatting.DARK_GRAY))
            }
        }
    }
}
