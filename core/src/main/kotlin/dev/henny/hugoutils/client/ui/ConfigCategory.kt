package dev.henny.hugoutils.client.ui

enum class NavGroup {
    RAIL,
    MARKET,
    MODS,
    SETTINGS,
    HIDDEN
}

enum class ConfigCategory(
    val id: String,
    val title: String,
    val available: Boolean,
    val group: NavGroup = NavGroup.RAIL,
    val footer: Boolean = false,
    val visualChild: Boolean = false
) {
    MARKET("market", "Market", true, NavGroup.RAIL),
    MARKET_HOME("market_home", "Home", true, NavGroup.MARKET),
    MARKET_ITEMS("market_items", "Items", true, NavGroup.MARKET),
    MARKET_ARBITRAGE("market_arbitrage", "Arbitrage", true, NavGroup.MARKET),
    MARKET_PLAYERS("market_players", "Spieler", true, NavGroup.MARKET),
    MARKET_SHOP("market_shop", "Shop", true, NavGroup.MARKET),
    MODS("mods", "Mods", true, NavGroup.RAIL),
    SETTINGS("settings", "Settings", true, NavGroup.RAIL),
    SIGN_IN("sign_in", "Account", true, NavGroup.RAIL, footer = true),

    DROPPED_GLOW("dropped_glow", "Dropped Glow", true, NavGroup.MODS, visualChild = true),
    HAND_GLINT("hand_glint", "Hand-Glint", true, NavGroup.MODS, visualChild = true),
    HAND_GLOW("hand_glow", "Hand-Glow", true, NavGroup.MODS, visualChild = true),
    PLAYER_GLOW("player_glow", "Player Glow", true, NavGroup.MODS, visualChild = true),
    FAST_ITEMS("fast_items", "Fast Items", true, NavGroup.MODS, visualChild = true),
    BLOCKS("blocks", "Highlight", true, NavGroup.MODS, visualChild = true),
    BLOCK_OVERLAY("block_overlay", "Overlay", true, NavGroup.MODS, visualChild = true),
    SKY("sky", "Sky", true, NavGroup.MODS, visualChild = true),
    EFFECTS("effects", "Effekte", true, NavGroup.MODS, visualChild = true),
    PERSPECTIVE("perspective", "F5", true, NavGroup.MODS, visualChild = true),

    GENERAL("general", "Allgemein", true, NavGroup.SETTINGS),
    PROFILES("profiles", "Profile", true, NavGroup.SETTINGS),
    FEEDBACK("feedback", "Feedback & Support", true, NavGroup.SETTINGS),
    UPDATES("updates", "Updates", true, NavGroup.SETTINGS),
    TOOLS("tools", "GUI Capture", true, NavGroup.SETTINGS),
    AFK_BOT("afk_bot", "AFK Bot", true, NavGroup.SETTINGS),

    CHAT("chat", "Chat", false, NavGroup.HIDDEN);

    val parentId: String? get() = when (group) {
        NavGroup.MARKET -> MARKET.id
        NavGroup.MODS -> MODS.id
        NavGroup.SETTINGS -> SETTINGS.id
        else -> null
    }

    fun requiredKey(): String? = when (this) {
        AFK_BOT -> "market.afk-bot"
        TOOLS -> "debug.inv"
        else -> null
    }

    companion object {
        val railEntries: List<ConfigCategory> = entries.filter { it.group == NavGroup.RAIL && it.available && !it.footer }
        val navEntries: List<ConfigCategory> = railEntries
        val marketEntries: List<ConfigCategory> = entries.filter { it.group == NavGroup.MARKET }
        val visualEntries: List<ConfigCategory> = entries.filter { it.group == NavGroup.MODS }
        val settingsEntries: List<ConfigCategory> = entries.filter { it.group == NavGroup.SETTINGS }
        val footerEntries: List<ConfigCategory> = entries.filter { it.footer && it.available }
    }
}
