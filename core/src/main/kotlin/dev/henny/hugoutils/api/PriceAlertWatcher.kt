package dev.henny.hugoutils.api

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.PriceAlert
import dev.henny.hugoutils.ui.Toast
import dev.henny.hugoutils.ui.UiOverlays
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient

object PriceAlertWatcher {
    private const val INTERVAL_TICKS = 20 * 8
    private var ticks = 0
    private var working = false

    fun initialize() {
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!ClientSessionStore.hasToken()) return@register
            if (ConfigManager.priceAlerts().isEmpty()) return@register
            if (ticks++ % INTERVAL_TICKS != 0) return@register
            poll()
        }
    }

    fun poll() {
        if (!ClientSessionStore.hasToken() || working) return
        val alerts = ConfigManager.priceAlerts()
        if (alerts.isEmpty()) return
        working = true
        ClientJobs.submit({ _, _ -> working = false }) {
            alerts.forEach(::check)
            ""
        }
    }

    internal fun triggered(alert: PriceAlert, quote: MarketQuote): Boolean {
        val price = quote.priceFor(alert.source, alert.above) ?: return false
        return if (alert.above) price >= alert.target else price <= alert.target
    }

    private fun check(alert: PriceAlert) {
        val quote = loadQuote(alert) ?: return
        val hit = triggered(alert, quote)
        if (hit && !alert.fired) {
            alert.fired = true
            ConfigManager.requestSave()
            val price = quote.priceFor(alert.source, alert.above)
            val direction = if (alert.above) "≥" else "≤"
            val label = "${alert.name.ifBlank { quote.name }}  ${MarketStats.formatCardPrice(price)}  (Ziel $direction ${MarketStats.formatCardPrice(alert.target)})"
            MinecraftClient.getInstance().execute {
                UiOverlays.host.show(Toast("Preiswecker  $label", durationSeconds = 8f, kind = Toast.Kind.SUCCESS))
            }
        } else if (!hit && alert.fired) {
            alert.fired = false
            ConfigManager.requestSave()
        }
    }

    private fun loadQuote(alert: PriceAlert): MarketQuote? {
        alert.itemId.takeIf { it.isNotBlank() }?.let { id ->
            MarketPriceCache.quoteForId(id)?.let { return it }
            runCatching { MarketStats.quote(ClientApi.item(id)) }.getOrNull()?.let { return it }
        }
        val query = alert.minecraftId.ifBlank { alert.name }
        if (query.isBlank()) return null
        MarketPriceCache.quoteForId(query)?.let { return it }
        val body = runCatching { ClientApi.items(query = query, limit = 8, force = false) }.getOrNull() ?: return null
        val items = JsonView.objectsNamed(body, "items").ifEmpty { JsonView.objects(body) }
        return items.firstOrNull()?.let(MarketStats::quote)
    }
}
