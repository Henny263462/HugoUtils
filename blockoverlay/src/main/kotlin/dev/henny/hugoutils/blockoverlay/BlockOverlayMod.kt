package dev.henny.hugoutils.blockoverlay

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.ui.ConfigPages
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

class BlockOverlayMod : ClientModInitializer {
    override fun onInitializeClient() {
        ConfigManager.registerSection(BlockOverlayConfig)
        ConfigPages.register(BlockOverlayConfigPage())
        ClientTickEvents.END_CLIENT_TICK.register(BlockOverlayConfig::flushWorldReload)
    }
}
