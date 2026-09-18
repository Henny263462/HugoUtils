package dev.henny.hugoutils.client.config

import dev.henny.hugoutils.ui.UiMetrics

class ModConfig {
    var droppedItemGlow: GlowStyle = GlowStyle.droppedDefault()
    var heldItemGlow: GlowStyle = GlowStyle.heldDefault()
    var playerGlow: GlowStyle = GlowStyle.playerDefault()
    var heldGlint: GlintStyle = GlintStyle()
    var perspectiveMode: String = PerspectiveMode.VANILLA.id
    var showCrosshairInThirdPerson: Boolean = false
    var showOwnNameInThirdPerson: Boolean = false
    var checkUpdatesAutomatically: Boolean = true
    var includePrereleases: Boolean = false
    var uiDebugCommandsEnabled: Boolean = false
    var lastSettingsPage: String = "general"
    var lastOpenedPage: String = "market_home"
    var restoreLastPage: Boolean = false
    var outlierProtection: Boolean = true
    var rtpShareEnabled: Boolean? = null
    var uiCornerRadius: Int = 0
    var marketListView: Boolean = false
    var pinnedMarketItems: MutableList<String> = ArrayList()
    var priceAlerts: MutableList<PriceAlert> = ArrayList()

    fun clamp() {
        droppedItemGlow.clamp()
        heldItemGlow.clamp(GlowStyle.MAX_HELD_THICKNESS)
        playerGlow.clamp()
        heldGlint.clamp()
        perspectiveMode = PerspectiveMode.fromId(perspectiveMode).id
        uiCornerRadius = uiCornerRadius.coerceIn(0, UiMetrics.CORNER_MAX)
    }
}

class PriceAlert {
    var itemId: String = ""
    var name: String = ""
    var minecraftId: String = ""
    var target: Double = 0.0
    var above: Boolean = false
    var source: String = "any"
    var fired: Boolean = false
}

enum class PerspectiveMode(val id: String, val label: String, val description: String) {
    VANILLA("vanilla", "Vanilla", "Normal → hinten → vorne"),
    BACK_ONLY("back_only", "Nur hinten", "Normal → hinten → Normal"),
    FRONT_ONLY("front_only", "Nur vorne", "Normal → vorne → Normal");

    companion object {
        fun fromId(id: String?): PerspectiveMode = entries.firstOrNull { it.id == id } ?: VANILLA
    }
}
