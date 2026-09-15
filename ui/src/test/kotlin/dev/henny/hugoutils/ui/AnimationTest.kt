package dev.henny.hugoutils.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimationTest {
    @Test
    fun `easing clamps and reaches endpoints`() {
        assertEquals(0f, Easing.EASE_IN_OUT.transform(-1f))
        assertEquals(1f, Easing.EASE_IN_OUT.transform(2f))
        assertEquals(.5f, Easing.EASE_IN_OUT.transform(.5f), .0001f)
    }

    @Test
    fun `animation uses frame time`() {
        val animation = Animation(0f, 1f, Easing.LINEAR)
        animation.animateTo(10f)
        assertEquals(2.5f, animation.update(.25f), .0001f)
        assertTrue(animation.running)
        assertEquals(10f, animation.update(.75f), .0001f)
        assertFalse(animation.running)
    }

    @Test
    fun `shared frame clock clamps long stalls`() {
        UiFrame.reset()
        UiFrame.beginFrame(1_000_000_000L)
        val delta = UiFrame.beginFrame(2_000_000_000L)
        assertEquals(UiFrame.MAX_DELTA_SECONDS, delta)
    }

    @Test
    fun `animated float retargets without jumping`() {
        val value = AnimatedFloat(0f, 1f, Easing.LINEAR)
        value.animateTo(1f)
        value.update(.4f)
        value.animateTo(0f)
        assertEquals(.4f, value.value, .0001f)
        assertEquals(.2f, value.update(.5f), .0001f)
    }
}
