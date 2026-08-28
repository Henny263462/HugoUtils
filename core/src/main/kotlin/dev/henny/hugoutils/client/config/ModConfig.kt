package dev.henny.hugoutils.client.config

class ModConfig {
    var droppedItemGlow: GlowStyle = GlowStyle.droppedDefault()
    var heldItemGlow: GlowStyle = GlowStyle.heldDefault()
    var playerGlow: GlowStyle = GlowStyle.playerDefault()
    var heldGlint: GlintStyle = GlintStyle()
    var checkUpdatesAutomatically: Boolean = true
    var includePrereleases: Boolean = false

    fun clamp() {
        droppedItemGlow.clamp()
        heldItemGlow.clamp()
        playerGlow.clamp()
        heldGlint.clamp()
    }
}
