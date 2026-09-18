package dev.henny.hugoutils.blockoverlay

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class BlockOverlayConfigTest {
    @Test
    fun `packed vertex color is mixed toward overlay`() {
        val mixed = OverlayTintConsumer.mixChannel(255, 0, 0.5f)
        assertEquals(127, mixed)
        assertEquals(191, OverlayTintConsumer.mixChannel(255, 127, 0.5f))
        assertEquals(137, OverlayTintConsumer.mixChannel(20, 255, 0.5f))
        val packed = OverlayTintConsumer.mixPacked(0xFFFFFFFF.toInt(), 0x800000FF.toInt())
        assertEquals(0xFF7F7FFF.toInt(), packed)
        val abgrWhite = 0xFFFFFFFF.toInt()
        assertNotEquals(abgrWhite, OverlayTintConsumer.mixAbgr(abgrWhite, 0xFF0000FF.toInt()))
    }
}
