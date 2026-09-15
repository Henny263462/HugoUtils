package dev.henny.hugoutils.client.input

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.PerspectiveMode
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.Perspective
import net.minecraft.entity.PlayerLikeEntity

object PerspectiveController {
    @JvmStatic
    fun handlePendingPresses(client: MinecraftClient) {
        val mode = PerspectiveMode.fromId(ConfigManager.config.perspectiveMode)
        if (mode == PerspectiveMode.VANILLA) return

        while (client.options.togglePerspectiveKey.wasPressed()) {
            val current = client.options.perspective
            val next = when {
                current != Perspective.FIRST_PERSON -> Perspective.FIRST_PERSON
                mode == PerspectiveMode.BACK_ONLY -> Perspective.THIRD_PERSON_BACK
                else -> Perspective.THIRD_PERSON_FRONT
            }
            client.options.perspective = next
        }
    }

    @JvmStatic
    fun shouldShowCrosshair(original: Boolean): Boolean {
        if (original) return true
        val client = MinecraftClient.getInstance()
        return ConfigManager.config.showCrosshairInThirdPerson &&
            !client.options.perspective.isFirstPerson
    }

    @JvmStatic
    fun shouldShowOwnName(player: PlayerLikeEntity): Boolean {
        val client = MinecraftClient.getInstance()
        return ConfigManager.config.showOwnNameInThirdPerson &&
            player === client.player &&
            !client.options.perspective.isFirstPerson
    }
}
