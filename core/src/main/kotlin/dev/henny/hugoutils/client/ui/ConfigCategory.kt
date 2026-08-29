package dev.henny.hugoutils.client.ui

enum class ConfigCategory(
    val id: String,
    val title: String,
    val available: Boolean,
    /** Footer dropdown above the version label instead of the main sidebar nav. */
    val footer: Boolean = false
) {
    VISUALS("visuals", "Visuals", true),
    PROFILES("profiles", "Profile", true),
    UPDATES("updates", "Updates", true, footer = true),
    MARKET("market", "Market", false),
    CHAT("chat", "Chat", false);

    companion object {
        val navEntries: List<ConfigCategory> = entries.filter { !it.footer }
        val footerEntries: List<ConfigCategory> = entries.filter { it.footer }
    }
}
