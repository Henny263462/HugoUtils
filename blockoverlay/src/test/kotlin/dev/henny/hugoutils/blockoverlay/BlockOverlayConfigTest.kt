package dev.henny.hugoutils.blockoverlay

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlockOverlayConfigTest {
    @Test
        fun `packed vertex color is mixed toward overlay`() {
            val mixed = OverlayTintConsumer.mixChannel(255, 0, 0.5f)
            assertEquals(127, mixed)
            assertEquals(191, OverlayTintConsumer.mixChannel(255, 127, 0.5f))
            assertEquals(137, OverlayTintConsumer.mixChannel(20, 255, 0.5f))
        }
}
