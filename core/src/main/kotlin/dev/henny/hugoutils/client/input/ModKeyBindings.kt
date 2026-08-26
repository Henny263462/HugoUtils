package dev.henny.hugoutils.client.input

import dev.henny.hugoutils.HugoIds
import dev.henny.hugoutils.client.ui.HugoScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

object ModKeyBindings {
    private val category = KeyBinding.Category.create(HugoIds.id("main"))

    private lateinit var openConfig: KeyBinding

    fun initialize() {
        openConfig = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.hugoutils.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
            )
        )
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (openConfig.wasPressed()) {
                toggle(client)
            }
        }
    }

    private fun toggle(client: MinecraftClient) {
        if (client.currentScreen is HugoScreen) {
            client.setScreen(null)
        } else if (client.currentScreen == null) {
            client.setScreen(HugoScreen())
        }
    }
}
