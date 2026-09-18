package dev.henny.hugoutils.client.rtp

import dev.henny.hugoutils.api.ClientApi
import dev.henny.hugoutils.api.ClientApiException
import dev.henny.hugoutils.api.ClientJobs
import dev.henny.hugoutils.api.ClientSessionStore
import dev.henny.hugoutils.api.JsonView
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.gui.capture.GuiCaptureScreenGeometry
import dev.henny.hugoutils.client.ui.PopupManager
import dev.henny.hugoutils.client.ui.RtpPrivacyScreen
import dev.henny.hugoutils.ui.Dialog
import dev.henny.hugoutils.ui.Toast
import dev.henny.hugoutils.ui.UiOverlays
import com.google.gson.JsonObject
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.world.ClientWorld
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKey
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.slf4j.LoggerFactory

object RtpShare {
    private val logger = LoggerFactory.getLogger("HugoUtils-RTP")
    private const val ARM_MS = 15_000L
    private const val JUMP_BLOCKS = 40.0
    private const val CHUNK_WAIT_MS = 3_000L

    @Volatile private var armedUntil = 0L
    @Volatile private var startX = 0.0
    @Volatile private var startY = 0.0
    @Volatile private var startZ = 0.0
    @Volatile private var startDimension: String? = null
    @Volatile private var pendingCaptureUntil = 0L
    @Volatile private var sending = false
    @Volatile private var pendingChoice: Boolean? = null

    private const val CONFIRM_REQUIRED = "Bitte bestätigen."

    fun initialize() {
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> disarm() }
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
    }

    fun sharing(): Boolean = ConfigManager.config.rtpShareEnabled == true

    fun choose(share: Boolean) = setFromUi(share)

    fun setFromUi(share: Boolean, confirm: Boolean = false) {
        val previous = ConfigManager.config.rtpShareEnabled
        pendingChoice = share
        ConfigManager.update { it.rtpShareEnabled = share }
        if (!ClientSessionStore.hasToken()) {
            pendingChoice = null
            closePrivacyPrompt()
            return
        }
        ClientJobs.submit({ message, error ->
            pendingChoice = null
            if (error) {
                ConfigManager.update { it.rtpShareEnabled = previous }
                UiOverlays.host.show(Toast(message, kind = Toast.Kind.ERROR))
                return@submit
            }
            if (message == CONFIRM_REQUIRED) return@submit
            closePrivacyPrompt()
            UiOverlays.host.show(Toast(message, kind = Toast.Kind.SUCCESS))
        }) {
            try {
                ClientApi.setRtpShare(share, confirm)
            } catch (error: ClientApiException) {
                if (error.error == "rtp_refund_required" && !confirm) {
                    ConfigManager.update { it.rtpShareEnabled = previous }
                    pendingChoice = null
                    MinecraftClient.getInstance().execute { confirmPrivacyRefund(error) }
                    return@submit CONFIRM_REQUIRED
                }
                throw error
            }
            if (share) "RTP-Punkte werden geteilt." else "Privacy-Modus aktiv."
        }
    }

    private fun closePrivacyPrompt() {
        val client = MinecraftClient.getInstance()
        client.execute {
            if (client.currentScreen is RtpPrivacyScreen) client.setScreen(null)
        }
    }

    private fun confirmPrivacyRefund(error: ClientApiException) {
        val days = JsonView.number(error.body, "refund.remainingDays")?.toInt() ?: 0
        PopupManager.open(
            Dialog(
                title = "Privacy-Modus",
                message = "Du verlierst den 10%-Rabatt. Restliche $days Tage werden als Guthaben zurückgezahlt (10% Gebühr). Premium endet sofort.",
                confirmLabel = "Zurückzahlen",
                cancelLabel = "Abbrechen",
                onConfirm = { setFromUi(false, confirm = true) }
            )
        )
    }

    fun syncFromMe(me: JsonObject) {
        if (pendingChoice != null) return
        val remote = remoteShare(me)
        val local = ConfigManager.config.rtpShareEnabled
        when {
            remote != null -> {
                if (local != remote) ConfigManager.update { it.rtpShareEnabled = remote }
            }
            local != null && ClientSessionStore.hasToken() -> runCatching { ClientApi.setRtpShare(local) }
        }
        if (ConfigManager.config.rtpShareEnabled != null) {
            closePrivacyPrompt()
        }
    }

    @JvmStatic
    fun observeHandledClick(screen: HandledScreen<*>, click: Click) {
        val title = screen.title.string
        val stackName = hoveredStackName(screen, click.x(), click.y())
        if (!RtpMenus.isRtpTitle(title) && (stackName == null || !RtpMenus.isRtpItem(stackName))) return
        arm(MinecraftClient.getInstance())
    }

    private fun tick(client: MinecraftClient) {
        maybePrompt(client)
        val player = client.player ?: return
        val world = client.world ?: return
        val now = System.currentTimeMillis()
        if (pendingCaptureUntil > now) {
            tryCapture(client, world)
            return
        }
        if (now > armedUntil) return
        val dimension = rtpDimension(world) ?: return
        val moved = player.squaredDistanceTo(startX, startY, startZ) >= JUMP_BLOCKS * JUMP_BLOCKS
        val changedWorld = dimension != startDimension
        if (!moved && !changedWorld) return
        pendingCaptureUntil = now + CHUNK_WAIT_MS
        tryCapture(client, world)
    }

    private fun maybePrompt(client: MinecraftClient) {
        if (ConfigManager.config.rtpShareEnabled != null) return
        if (client.player == null || client.world == null) return
        if (client.currentScreen is RtpPrivacyScreen) return
        if (client.currentScreen != null) return
        client.setScreen(RtpPrivacyScreen())
    }

    private fun arm(client: MinecraftClient) {
        val player = client.player ?: return
        val world = client.world ?: return
        startX = player.x
        startY = player.y
        startZ = player.z
        startDimension = rtpDimension(world)
        armedUntil = System.currentTimeMillis() + ARM_MS
        pendingCaptureUntil = 0L
    }

    private fun disarm() {
        armedUntil = 0L
        pendingCaptureUntil = 0L
    }

    private fun tryCapture(client: MinecraftClient, world: ClientWorld) {
        if (!sharing()) {
            disarm()
            return
        }
        if (!ClientSessionStore.hasToken()) {
            disarm()
            return
        }
        val player = client.player ?: return
        val dimension = rtpDimension(world) ?: return
        val pos = player.blockPos
        if (world.chunkManager.getWorldChunk(pos.x shr 4, pos.z shr 4) == null) return
        val biome = biomeId(world, pos)
        if (biome.isBlank() || biome == "unknown") {
            if (System.currentTimeMillis() < pendingCaptureUntil) return
        }
        if (sending) return
        sending = true
        armedUntil = 0L
        pendingCaptureUntil = 0L
        val x = pos.x
        val y = pos.y
        val z = pos.z
        ClientJobs.submit({ message, error ->
            sending = false
            if (error) {
                logger.debug("RTP-Punkt nicht gespeichert: {}", message)
                return@submit
            }
            val dimLabel = when (dimension) {
                "nether" -> "Nether"
                "end" -> "End"
                else -> "Overworld"
            }
            UiOverlays.host.show(
                Toast("Standort geteilt  $dimLabel $x $y $z", durationSeconds = 4f, kind = Toast.Kind.SUCCESS)
            )
        }) {
            ClientApi.recordRtpPoint(dimension, biome.ifBlank { "unknown" }, x, y, z)
            "ok"
        }
    }

    private fun hoveredStackName(screen: HandledScreen<*>, mouseX: Double, mouseY: Double): String? {
        val originX = GuiCaptureScreenGeometry.x(screen)
        val originY = GuiCaptureScreenGeometry.y(screen)
        for (slot in screen.screenHandler.slots) {
            val x = originX + slot.x
            val y = originY + slot.y
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                val stack = slot.stack
                if (stack.isEmpty) return null
                return stackName(stack)
            }
        }
        return null
    }

    private fun stackName(stack: ItemStack): String = stack.name.string

    private fun rtpDimension(world: ClientWorld): String? {
        val key = world.registryKey
        return when {
            key === World.OVERWORLD || matches(key, "overworld") -> "overworld"
            key === World.NETHER || matches(key, "the_nether", "nether") -> "nether"
            key === World.END || matches(key, "the_end", "end") -> "end"
            else -> null
        }
    }

    private fun matches(key: RegistryKey<World>, vararg names: String): Boolean {
        val path = key.value.path
        val id = key.value.toString()
        return names.any { path == it || id.endsWith(":$it") }
    }

    private fun biomeId(world: ClientWorld, pos: BlockPos): String {
        val entry = world.getBiome(pos)
        val fromKey = entry.key.map { it.value.toString() }.orElse(null)
        if (!fromKey.isNullOrBlank()) return fromKey
        val id = runCatching { entry.idAsString }.getOrNull()
        return id?.takeIf { it.isNotBlank() } ?: "unknown"
    }

    private fun remoteShare(me: JsonObject): Boolean? {
        val rtp = me.getAsJsonObject("rtp")
        val value = rtp?.get("share") ?: return null
        if (value.isJsonNull) return null
        return if (value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) value.asBoolean else null
    }
}
