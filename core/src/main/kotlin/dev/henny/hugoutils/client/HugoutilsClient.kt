package dev.henny.hugoutils.client

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.EffectsConfig
import dev.henny.hugoutils.client.gui.capture.GuiCaptureController
import dev.henny.hugoutils.client.input.ModClientCommands
import dev.henny.hugoutils.client.input.ModKeyBindings
import dev.henny.hugoutils.client.ui.ConfigPages
import dev.henny.hugoutils.client.ui.EffectsConfigPage
import dev.henny.hugoutils.client.ui.GuiCaptureConfigPage
import dev.henny.hugoutils.client.ui.HugoScreen
import dev.henny.hugoutils.client.ui.PerspectiveConfigPage
import dev.henny.hugoutils.client.ui.ProfileConfigPage
import dev.henny.hugoutils.client.ui.SignInConfigPage
import dev.henny.hugoutils.client.ui.UpdateConfigPage
import dev.henny.hugoutils.client.ui.UiDebugGate
import dev.henny.hugoutils.update.UpdateManager
import dev.henny.hugoutils.ui.UiDebugCommands
import dev.henny.hugoutils.ui.UiNavigation
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient

class HugoutilsClient : ClientModInitializer {
    override fun onInitializeClient() {
        ConfigManager.load()
        ConfigManager.registerSection(EffectsConfig)
        UpdateManager.initialize()
        ConfigPages.register(EffectsConfigPage())
        ConfigPages.register(GuiCaptureConfigPage())
        ConfigPages.register(PerspectiveConfigPage())
        ConfigPages.register(SignInConfigPage())
        ConfigPages.register(ProfileConfigPage())
        ConfigPages.register(UpdateConfigPage())
        ModClientCommands.initialize()
        UiDebugCommands.initialize(
            UiDebugGate.isEnabled(
                FabricLoader.getInstance().isDevelopmentEnvironment,
                ConfigManager.config.uiDebugCommandsEnabled
            )
        ) { pageId ->
            if (UiNavigation.registry.entry(pageId) == null) {
                false
            } else {
                MinecraftClient.getInstance().setScreen(HugoScreen(pageId))
                true
            }
        }
        ModKeyBindings.initialize()
        GuiCaptureController.initialize()
    }
}
