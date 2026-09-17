package dev.henny.hugoutils.client

import dev.henny.hugoutils.api.AuthApiClient
import dev.henny.hugoutils.api.ClientFlags
import dev.henny.hugoutils.api.ClientSessionStore
import dev.henny.hugoutils.api.MarketItemTooltips
import dev.henny.hugoutils.api.PriceAlertWatcher
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.EffectsConfig
import dev.henny.hugoutils.client.gui.capture.GuiCaptureController
import dev.henny.hugoutils.client.input.ModClientCommands
import dev.henny.hugoutils.client.input.ModKeyBindings
import dev.henny.hugoutils.client.ui.AfkBotConfigPage
import dev.henny.hugoutils.client.ui.ComingSoonPage
import dev.henny.hugoutils.client.ui.ConfigCategory
import dev.henny.hugoutils.client.ui.ConfigPages
import dev.henny.hugoutils.client.ui.EffectsConfigPage
import dev.henny.hugoutils.client.ui.FeedbackConfigPage
import dev.henny.hugoutils.client.ui.GeneralConfigPage
import dev.henny.hugoutils.client.ui.GuiCaptureConfigPage
import dev.henny.hugoutils.client.ui.HugoScreen
import dev.henny.hugoutils.client.ui.MarketConfigPage
import dev.henny.hugoutils.client.ui.MarketHomePage
import dev.henny.hugoutils.client.ui.MenuButtons
import dev.henny.hugoutils.client.ui.PerspectiveConfigPage
import dev.henny.hugoutils.client.ui.ProfileConfigPage
import dev.henny.hugoutils.client.ui.SignInConfigPage
import dev.henny.hugoutils.client.ui.UpdateConfigPage
import dev.henny.hugoutils.client.ui.UiDebugGate
import dev.henny.hugoutils.update.UpdateManager
import dev.henny.hugoutils.ui.UiDebugCommands
import dev.henny.hugoutils.ui.UiNavigation
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient

class HugoutilsClient : ClientModInitializer {
    override fun onInitializeClient() {
        ConfigManager.load()
        ClientSessionStore.load()
        ClientFlags.listener = {
            ConfigPages.refreshAvailability()
            MinecraftClient.getInstance().execute {
                (MinecraftClient.getInstance().currentScreen as? HugoScreen)?.onFlagsChanged()
            }
        }
        ClientLifecycleEvents.CLIENT_STARTED.register {
            AuthApiClient.ensureLoggedIn()
        }
        ConfigManager.registerSection(EffectsConfig)
        UpdateManager.initialize()
        MarketItemTooltips.initialize()
        PriceAlertWatcher.initialize()
        ConfigPages.register(EffectsConfigPage())
        ConfigPages.register(GuiCaptureConfigPage())
        ConfigPages.register(PerspectiveConfigPage())
        ConfigPages.register(SignInConfigPage())
        ConfigPages.register(GeneralConfigPage())
        ConfigPages.register(MarketHomePage())
        ConfigPages.register(MarketConfigPage())
        ConfigPages.register(ComingSoonPage(
            ConfigCategory.MARKET_ARBITRAGE,
            message = "Coming soon",
            detail = "Orders kaufen, Auktionen verkaufen — folgt."
        ))
        ConfigPages.register(ComingSoonPage(ConfigCategory.MARKET_PLAYERS))
        ConfigPages.register(ComingSoonPage(ConfigCategory.MARKET_SHOP))
        ConfigPages.register(AfkBotConfigPage())
        ConfigPages.register(ProfileConfigPage())
        ConfigPages.register(FeedbackConfigPage())
        ConfigPages.register(UpdateConfigPage())
        ConfigPages.refreshAvailability()
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
                HugoScreen.open(MinecraftClient.getInstance(), pageId)
                true
            }
        }
        ModKeyBindings.initialize()
        MenuButtons.initialize()
        GuiCaptureController.initialize()
    }
}
