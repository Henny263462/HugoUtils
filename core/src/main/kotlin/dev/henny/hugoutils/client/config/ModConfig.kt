package dev.henny.hugoutils.client.config

class ModConfig {
    var droppedItemGlow: GlowStyle = GlowStyle.droppedDefault()
    var heldItemGlow: GlowStyle = GlowStyle.heldDefault()
    var playerGlow: GlowStyle = GlowStyle.playerDefault()
    var heldGlint: GlintStyle = GlintStyle()
    var perspectiveMode: String = PerspectiveMode.VANILLA.id
    var checkUpdatesAutomatically: Boolean = true
    var includePrereleases: Boolean = false

    fun clamp() {
        droppedItemGlow.clamp()
        heldItemGlow.clamp()
        playerGlow.clamp()
        heldGlint.clamp()
        perspectiveMode = PerspectiveMode.fromId(perspectiveMode).id
    }
}

enum class PerspectiveMode(val id: String, val label: String, val description: String) {
    VANILLA("vanilla", "Vanilla", "Normal → hinten → vorne"),
    BACK_ONLY("back_only", "Nur hinten", "Normal → hinten → Normal"),
    FRONT_ONLY("front_only", "Nur vorne", "Normal → vorne → Normal");

    companion object {
        fun fromId(id: String?): PerspectiveMode = entries.firstOrNull { it.id == id } ?: VANILLA
    }
}
