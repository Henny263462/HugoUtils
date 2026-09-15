package dev.henny.hugoutils.blockhighlight

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.ui.ConfigPages
import net.fabricmc.api.ClientModInitializer

class BlockHighlightMod : ClientModInitializer {
    override fun onInitializeClient() {
        ConfigManager.registerSection(BlockHighlightConfig)
        ConfigPages.register(BlockHighlightConfigPage())
        BlockHighlightRenderLayer.initialize()
    }
}
