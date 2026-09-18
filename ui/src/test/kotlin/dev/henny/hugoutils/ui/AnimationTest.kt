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

    @Test
    fun `bounce easing overshoots then settles`() {
        assertEquals(0f, Easing.EASE_OUT_BOUNCE.transform(0f), .0001f)
        assertEquals(1f, Easing.EASE_OUT_BOUNCE.transform(1f), .0001f)
        assertTrue(Easing.EASE_OUT_BACK.transform(.7f) > 1f)
    }

    @Test
    fun `animated float respects delay`() {
        val value = AnimatedFloat(0f, .2f, Easing.LINEAR)
        value.animateTo(1f, delaySeconds = .3f)
        assertEquals(0f, value.update(.2f), .0001f)
        assertTrue(value.running)
        assertEquals(.5f, value.update(.2f), .0001f)
    }

    @Test
    fun `sparkline downsample keeps endpoints`() {
        val sampled = UiDraw.downsample((0..99).map { it.toFloat() }, 5)
        assertEquals(5, sampled.size)
        assertEquals(0f, sampled.first())
        assertEquals(99f, sampled.last())
    }

    @Test
    fun `chart valueAt interpolates along the line`() {
        val values = listOf(0f, 10f, 20f)
        assertEquals(0f, UiDraw.valueAt(values, 0f), .0001f)
        assertEquals(10f, UiDraw.valueAt(values, 0.5f), .0001f)
        assertEquals(20f, UiDraw.valueAt(values, 1f), .0001f)
        assertEquals(5f, UiDraw.valueAt(values, 0.25f), .0001f)
    }

    @Test
    fun `skeleton pulse stays between dim and bright`() {
        val pulse = UiDraw.loadingPulse(0L)
        assertTrue(pulse in 0.4f..0.85f)
        assertTrue(UiDraw.loadingPulse(140L) in 0.4f..0.85f)
    }
}
