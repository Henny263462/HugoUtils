package dev.henny.hugoutils.client

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.input.ModClientCommands
import dev.henny.hugoutils.client.input.ModKeyBindings
import dev.henny.hugoutils.client.ui.ConfigPages
import dev.henny.hugoutils.client.ui.PerspectiveConfigPage
import dev.henny.hugoutils.client.ui.ProfileConfigPage
import dev.henny.hugoutils.client.ui.SignInConfigPage
import dev.henny.hugoutils.client.ui.UpdateConfigPage
import dev.henny.hugoutils.update.UpdateManager
import net.fabricmc.api.ClientModInitializer

class HugoutilsClient : ClientModInitializer {
    override fun onInitializeClient() {
        ConfigManager.load()
        UpdateManager.initialize()
        ConfigPages.register(PerspectiveConfigPage())
        ConfigPages.register(SignInConfigPage())
        ConfigPages.register(ProfileConfigPage())
        ConfigPages.register(UpdateConfigPage())
        ModClientCommands.initialize()
        ModKeyBindings.initialize()
    }
}
