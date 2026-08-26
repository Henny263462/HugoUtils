package dev.henny.hugoutils.playerglow

import net.fabricmc.api.ClientModInitializer

class PlayerGlowMod : ClientModInitializer {
    override fun onInitializeClient() {
        PlayerGlowRenderer.initialize()
    }
}
