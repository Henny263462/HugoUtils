package dev.henny.hugoutils.api

import com.google.gson.JsonObject
import dev.henny.hugoutils.ui.Toast
import dev.henny.hugoutils.ui.UiOverlays
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object MarketFeedWatcher {
    private const val INTERVAL_TICKS = 20 * 12
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "HugoUtils-Feed").apply { isDaemon = true }
    }
    private val busy = AtomicBoolean(false)
    private val seen = LinkedHashSet<String>()
    private var ticks = 0
    private var primed = false

    fun initialize() {
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!ClientSessionStore.hasToken()) {
                primed = false
                seen.clear()
                return@register
            }
            if (ticks++ % INTERVAL_TICKS != 0) return@register
            refresh()
        }
    }

    fun refresh() {
        if (!ClientSessionStore.hasToken() || !busy.compareAndSet(false, true)) return
        executor.execute {
            try {
                val events = JsonView.objects(ClientApi.feed(force = true))
                val incoming = events.map { eventKey(it) to it }
                MinecraftClient.getInstance().execute {
                    if (!primed) {
                        incoming.forEach { seen += it.first }
                        while (seen.size > 40) seen.remove(seen.first())
                        primed = true
                        return@execute
                    }
                    val fresh = incoming.filter { it.first !in seen }
                    fresh.forEach { seen += it.first }
                    fresh.take(MAX_TOASTS_PER_POLL).forEach { (_, obj) ->
                        UiOverlays.host.show(Toast(JsonView.label(obj), durationSeconds = 4f, kind = Toast.Kind.INFO))
                    }
                    if (fresh.size > MAX_TOASTS_PER_POLL) {
                        UiOverlays.host.show(
                            Toast(
                                "${fresh.size - MAX_TOASTS_PER_POLL} weitere Market-Ereignisse",
                                durationSeconds = 4f,
                                kind = Toast.Kind.INFO
                            )
                        )
                    }
                    while (seen.size > 40) seen.remove(seen.first())
                }
            } catch (_: Exception) {
            } finally {
                busy.set(false)
            }
        }
    }

    internal fun eventKey(obj: JsonObject): String {
        JsonView.str(obj, "id", "eventId", "uuid")?.let { return it }
        val stable = listOf(
            "createdAt", "time", "timestamp", "type", "source", "itemId",
            "itemName", "name", "seller", "buyer", "player", "playerName"
        ).mapNotNull { key -> JsonView.str(obj, key)?.let { "$key=$it" } }
        return stable.joinToString("|").ifBlank { "unknown-event" }
    }

    private const val MAX_TOASTS_PER_POLL = 5
}
