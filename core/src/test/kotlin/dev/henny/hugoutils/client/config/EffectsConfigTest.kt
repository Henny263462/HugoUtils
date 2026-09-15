package dev.henny.hugoutils.client.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class EffectsConfigTest {
    @AfterEach
    fun reset() = EffectsConfig.reset()

    @Test
    fun globalAndPerTypeSettingsAreCombinedAndClamped() {
        EffectsConfig.particleDensity = 0.5f
        EffectsConfig.particleSize = 2f
        EffectsConfig.particleOpacity = 0.8f
        EffectsConfig.particleOverrides["minecraft:flame"] = ParticleTypeOverride(
            enabled = true,
            density = 0.4f,
            size = 0.5f,
            opacity = 0.25f
        )

        val settings = EffectsConfig.settingsFor("minecraft:flame")

        assertEquals(0.2f, settings.density, 0.0001f)
        assertEquals(1f, settings.size, 0.0001f)
        assertEquals(0.2f, settings.opacity, 0.0001f)
    }

    @Test
    fun masterToggleDisablesEveryParticleType() {
        EffectsConfig.particlesEnabled = false
        assertFalse(EffectsConfig.settingsFor("minecraft:flame").enabled)
    }
}
