package dev.henny.hugoutils.client.access

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

enum class AccessFeature(val id: String) {
    DROPPED_ITEM_GLOW("dropped_item_glow"),
    HELD_ITEM_GLOW("held_item_glow"),
    PLAYER_GLOW("player_glow"),
    HELD_GLINT("held_glint"),
    FASTITEMS("fastitems")
}

/**
 * Read-only entitlement cache. Access fails closed on startup and after a
 * bounded stale window, while short API outages keep already verified access.
 */
object FeatureAccessManager {
    private const val API_ROOT = "https://hugo.henny.dev/api/v1/access/"
    private const val REFRESH_MS = 5 * 60 * 1000L
    private const val MAX_STALE_MS = 15 * 60 * 1000L
    private val logger = LoggerFactory.getLogger("HugoUtils/Access")
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private val inFlight = AtomicBoolean(false)

    @Volatile
    private var currentPlayer: UUID? = null

    @Volatile
    private var snapshot: Snapshot? = null

    @JvmStatic
    fun initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client -> tick(client) })
    }

    @JvmStatic
    fun refreshNow() {
        val player = MinecraftClient.getInstance().player?.uuid ?: return
        currentPlayer = player
        refresh(player)
    }

    @JvmStatic
    fun has(feature: AccessFeature): Boolean {
        val player = currentPlayer ?: return false
        val state = snapshot ?: return false
        val now = System.currentTimeMillis()
        if (state.player != player || now - state.fetchedAt > MAX_STALE_MS) return false
        val entitlement = state.features[feature] ?: return false
        return entitlement.active &&
            (entitlement.unlimited || entitlement.expiresAt?.isAfter(Instant.now()) == true)
    }

    private fun tick(client: MinecraftClient) {
        val player = client.player?.uuid
        if (player == null) {
            currentPlayer = null
            snapshot = null
            return
        }
        if (currentPlayer != player) {
            currentPlayer = player
            snapshot = null
        }
        val lastFetch = snapshot?.fetchedAt ?: 0L
        if (System.currentTimeMillis() - lastFetch >= REFRESH_MS) refresh(player)
    }

    private fun refresh(player: UUID) {
        if (!inFlight.compareAndSet(false, true)) return
        val request = HttpRequest.newBuilder(URI.create(API_ROOT + player))
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "application/json")
            .header("User-Agent", "HugoUtils")
            .GET()
            .build()
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .whenComplete { response, failure ->
                try {
                    if (failure != null || response == null || response.statusCode() != 200) {
                        logger.warn(
                            "Feature access refresh failed for {}: {}",
                            player,
                            failure?.message ?: "HTTP ${response?.statusCode()}"
                        )
                        return@whenComplete
                    }
                    val parsed = parse(player, response.body()) ?: return@whenComplete
                    if (currentPlayer == player) snapshot = parsed
                } catch (error: Exception) {
                    logger.warn("Invalid feature access response for {}", player, error)
                } finally {
                    inFlight.set(false)
                }
            }
    }

    private fun parse(player: UUID, body: String): Snapshot? {
        val root = JsonParser.parseString(body).asJsonObject
        if (!root.boolean("ok")) return null
        val values = EnumMapBuilder()
        val features = root.getAsJsonObject("features")
        if (features != null) {
            for (feature in AccessFeature.entries) {
                val value = features.getAsJsonObject(feature.id)
                values[feature] = value?.toEntitlement() ?: Entitlement()
            }
        } else {
            // Rolling-deploy compatibility with the previous global API.
            val active = root.boolean("active")
            val unlimited = root.boolean("unlimited")
            val expiresAt = root.instant("expiresAt")
            for (feature in AccessFeature.entries) {
                values[feature] = Entitlement(active, unlimited, expiresAt)
            }
        }
        return Snapshot(player, System.currentTimeMillis(), values.build())
    }

    private fun JsonObject.toEntitlement() = Entitlement(
        active = boolean("active"),
        unlimited = boolean("unlimited"),
        expiresAt = instant("expiresAt")
    )

    private fun JsonObject.boolean(name: String): Boolean =
        get(name)?.takeUnless { it.isJsonNull }?.asBoolean == true

    private fun JsonObject.instant(name: String): Instant? =
        get(name)?.takeUnless { it.isJsonNull }?.asString?.let { value ->
            runCatching { Instant.parse(value) }.getOrNull()
        }

    private data class Snapshot(
        val player: UUID,
        val fetchedAt: Long,
        val features: Map<AccessFeature, Entitlement>
    )

    private data class Entitlement(
        val active: Boolean = false,
        val unlimited: Boolean = false,
        val expiresAt: Instant? = null
    )

    private class EnumMapBuilder {
        private val values = java.util.EnumMap<AccessFeature, Entitlement>(AccessFeature::class.java)
        operator fun set(feature: AccessFeature, entitlement: Entitlement) {
            values[feature] = entitlement
        }
        fun build(): Map<AccessFeature, Entitlement> = values.toMap()
    }
}
