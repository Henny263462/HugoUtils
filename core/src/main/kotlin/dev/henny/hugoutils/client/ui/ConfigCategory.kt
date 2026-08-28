package dev.henny.hugoutils.client.ui

enum class ConfigCategory(
    val id: String,
    val title: String,
    val available: Boolean
) {
    VISUALS("visuals", "Visuals", true),
    PROFILES("profiles", "Profile", true),
    UPDATES("updates", "Updates", true),
    MARKET("market", "Market", false),
    CHAT("chat", "Chat", false);
}
