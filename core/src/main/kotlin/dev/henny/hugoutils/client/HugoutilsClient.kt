package dev.henny.hugoutils.client

import dev.henny.hugoutils.client.access.FeatureAccessManager
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.input.ModClientCommands
import dev.henny.hugoutils.client.input.ModKeyBindings
import dev.henny.hugoutils.client.ui.ConfigPages
import dev.henny.hugoutils.client.ui.ProfileConfigPage
import net.fabricmc.api.ClientModInitializer

class HugoutilsClient : ClientModInitializer {
    override fun onInitializeClient() {
        FeatureAccessManager.initialize()
        ConfigManager.load()
        ConfigPages.register(ProfileConfigPage())
        ModClientCommands.initialize()
        ModKeyBindings.initialize()
    }
}
