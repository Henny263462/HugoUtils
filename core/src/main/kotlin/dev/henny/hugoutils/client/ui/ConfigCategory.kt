package dev.henny.hugoutils.client.ui

enum class ConfigCategory(
    val id: String,
    val title: String,
    val available: Boolean,
    /** Footer dropdown above the version label instead of the main sidebar nav. */
    val footer: Boolean = false,
    val visualChild: Boolean = false
) {
    VISUALS("visuals", "Visuals", true),
    DROPPED_GLOW("dropped_glow", "Dropped Item Glow", true, visualChild = true),
    HAND_GLINT("hand_glint", "Hand-Glint", true, visualChild = true),
    HAND_GLOW("hand_glow", "Hand-Glow", true, visualChild = true),
    PLAYER_GLOW("player_glow", "Player Glow", true, visualChild = true),
    FAST_ITEMS("fast_items", "Fast Items", true, visualChild = true),
    BLOCKS("blocks", "Block Highlight", true, visualChild = true),
    EFFECTS("effects", "Effekte", true, visualChild = true),
    PERSPECTIVE("perspective", "F5-Perspektive", true, visualChild = true),
    TOOLS("tools", "GUI Capture", true),
    AFK_BOT("afk_bot", "AFK Bot", true),
    SIGN_IN("sign_in", "Sign-in", true),
    PROFILES("profiles", "Profile", true),
    UPDATES("updates", "Updates", true, footer = true),
    MARKET("market", "Market", false),
    CHAT("chat", "Chat", false);

    companion object {
        val navEntries: List<ConfigCategory> = entries.filter { !it.footer && !it.visualChild }
        val visualEntries: List<ConfigCategory> = entries.filter { it.visualChild }
        val footerEntries: List<ConfigCategory> = entries.filter { it.footer }
    }
}
