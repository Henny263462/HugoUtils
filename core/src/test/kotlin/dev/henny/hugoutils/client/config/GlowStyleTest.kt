package dev.henny.hugoutils.client.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GlowStyleTest {
    @Test
    fun heldThicknessSliderReachesSixteenPixels() {
        assertEquals(1, GlowStyle.thicknessFromSlider(0f, GlowStyle.MAX_HELD_THICKNESS))
        assertEquals(16, GlowStyle.thicknessFromSlider(1f, GlowStyle.MAX_HELD_THICKNESS))
        val style = GlowStyle.heldDefault()
        style.thicknessPixels = 16
        style.clamp(GlowStyle.MAX_HELD_THICKNESS)
        assertEquals(16, style.thicknessPixels)
        style.clamp()
        assertEquals(4, style.thicknessPixels)
    }

    @Test
    fun defaultClampKeepsDroppedAndPlayerInFourPixelRange() {
        val dropped = GlowStyle.droppedDefault()
        dropped.thicknessPixels = 12
        dropped.clamp()
        assertEquals(4, dropped.thicknessPixels)
    }
}

class PerspectiveHudSettingsTest {
    @Test
    fun thirdPersonHudFlagsRoundtripThroughGson() {
        val gson = com.google.gson.Gson()
        val config = ModConfig().apply {
            showCrosshairInThirdPerson = true
            showOwnNameInThirdPerson = true
            perspectiveMode = PerspectiveMode.BACK_ONLY.id
        }
        val json = gson.toJsonTree(config).asJsonObject
        assertTrue(json.get("showCrosshairInThirdPerson").asBoolean)
        assertTrue(json.get("showOwnNameInThirdPerson").asBoolean)
        val restored = gson.fromJson(json, ModConfig::class.java)
        assertTrue(restored.showCrosshairInThirdPerson)
        assertTrue(restored.showOwnNameInThirdPerson)
        assertEquals(PerspectiveMode.BACK_ONLY.id, restored.perspectiveMode)
    }
}
