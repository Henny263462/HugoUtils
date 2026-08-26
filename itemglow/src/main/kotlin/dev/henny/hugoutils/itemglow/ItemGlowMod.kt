package dev.henny.hugoutils.itemglow

import net.fabricmc.api.ClientModInitializer

class ItemGlowMod : ClientModInitializer {
    override fun onInitializeClient() {
        ItemGlowRenderer.initialize()
    }
}
