package dev.henny.hugoutils.client.gui.capture

import dev.henny.hugoutils.HugoIds
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.input.KeyInput
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

fun interface GuiCaptureStarter {
    fun startSnapshot(screen: Screen)
}

object GuiCaptureController : GuiCaptureStarter {
    private val category = KeyBinding.Category.create(HugoIds.id("gui_capture"))
    private var initialized = false
    private var overlay: GuiCaptureOverlay? = null

    fun initialize() {
        if (initialized) return
        initialized = true
        val capture = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.hugoutils.gui_capture",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                category
            )
        )
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (capture.wasPressed()) {
                val screen = client.currentScreen
                if (screen != null && screen !is GuiCaptureEditorScreen) startSnapshot(screen)
            }
            overlay?.takeIf {
                client.currentScreen !== it.screen && client.currentScreen !is GuiCaptureEditorScreen
            }?.let { stop() }
        }
    }

    override fun startSnapshot(screen: Screen) {
        val client = MinecraftClient.getInstance()
        val snapshot = GuiCaptureSnapshotter.capture(client, screen)
        val suggestedId = suggestId(screen)
        val inventoryEnd = snapshot.inventoryStart?.minus(1)
        val draft = GuiCaptureDraft(
            screenId = suggestedId,
            titlePattern = Regex.escape(snapshot.title),
            layout = buildString {
                snapshot.rows?.let { append("${it}x9, ") }
                append("${snapshot.slotCount} Slots")
                snapshot.inventoryStart?.let { append(", Spielerinventar ab $it") }
            },
            itemSlots = inventoryEnd?.takeIf { it >= 0 }?.let { "0-$it" }.orEmpty()
        )
        snapshot.slots.forEach { slot ->
            draft.annotations[slot.slot] = SlotAnnotation(
                role = when {
                    slot.hotbar -> GuiSlotRole.HOTBAR
                    slot.playerInventory -> GuiSlotRole.PLAYER_INVENTORY
                    else -> GuiSlotRole.UNKNOWN
                }
            )
        }
        overlay = GuiCaptureOverlay(
            client,
            screen,
            snapshot,
            draft
        )
        if (screen !is HandledScreen<*>) {
            client.setScreen(GuiCaptureEditorScreen(screen))
        }
    }

    fun activeFor(screen: Screen): GuiCaptureOverlay? =
        overlay?.takeIf { it.screen === screen }

    fun statusText(): String = overlay?.let { "Aktive Session: ${it.draft.screenId}" }
        ?: "Keine aktive Capture-Session"

    fun handleCaptureKey(screen: Screen, input: KeyInput): Boolean {
        if (input.key() != GLFW.GLFW_KEY_F8 || overlay?.screen === screen) return false
        startSnapshot(screen)
        return true
    }

    fun stop() {
        overlay = null
        val client = MinecraftClient.getInstance()
        (client.currentScreen as? GuiCaptureEditorScreen)?.let { client.setScreen(it.parent) }
    }

    private fun suggestId(screen: Screen): String {
        val className = screen.javaClass.simpleName
            .removeSuffix("HandledScreen")
            .removeSuffix("Screen")
            .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
            .lowercase()
        return GuiCaptureStore.sanitizeScreenId(className).ifBlank { "gui.capture" }
    }
}
