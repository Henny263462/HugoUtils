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
}
