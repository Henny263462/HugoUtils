package dev.henny.hugoutils.client.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigCategoryNavTest {
    @Test
    fun `rail only contains market mods and settings`() {
        assertEquals(
            listOf("market", "mods", "settings"),
            ConfigCategory.railEntries.map { it.id }
        )
        assertEquals(listOf("sign_in"), ConfigCategory.footerEntries.map { it.id })
        assertEquals("Account", ConfigCategory.SIGN_IN.title)
    }

    @Test
    fun `mods children stay under the mods rail`() {
        assertTrue(ConfigCategory.visualEntries.isNotEmpty())
        assertTrue(ConfigCategory.visualEntries.all { it.parentId == ConfigCategory.MODS.id })
        assertTrue(ConfigCategory.DROPPED_GLOW in ConfigCategory.visualEntries)
        assertTrue(ConfigCategory.BLOCK_OVERLAY in ConfigCategory.visualEntries)
        assertTrue(ConfigCategory.SKY in ConfigCategory.visualEntries)
    }

    @Test
    fun `market children stay under the market rail`() {
        assertEquals(
            listOf("market_home", "market_items", "market_arbitrage", "market_players", "market_shop"),
            ConfigCategory.marketEntries.map { it.id }
        )
        assertTrue(ConfigCategory.marketEntries.all { it.parentId == ConfigCategory.MARKET.id })
        assertEquals("market_home", dev.henny.hugoutils.client.config.ModConfig().lastOpenedPage)
        assertEquals(false, dev.henny.hugoutils.client.config.ModConfig().restoreLastPage)
    }

    @Test
    fun `settings children stay under the settings rail`() {
        assertEquals(
            listOf("general", "profiles", "feedback", "updates", "tools", "afk_bot"),
            ConfigCategory.settingsEntries.map { it.id }
        )
        assertTrue(ConfigCategory.settingsEntries.all { it.parentId == ConfigCategory.SETTINGS.id })
        assertEquals("general", dev.henny.hugoutils.client.config.ModConfig().lastSettingsPage)
        assertEquals(0, dev.henny.hugoutils.client.config.ModConfig().uiCornerRadius)
        assertEquals("market.afk-bot", ConfigCategory.AFK_BOT.requiredKey())
        assertEquals("debug.inv", ConfigCategory.TOOLS.requiredKey())
        assertEquals(null, ConfigCategory.PROFILES.requiredKey())
    }
}
