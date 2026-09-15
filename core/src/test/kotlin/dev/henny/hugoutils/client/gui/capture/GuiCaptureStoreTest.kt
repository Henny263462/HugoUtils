package dev.henny.hugoutils.client.gui.capture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GuiCaptureStoreTest {
    @Test
    fun screenIdsAreStableAndFilesystemSafe() {
        assertEquals("ah.main-page", GuiCaptureStore.sanitizeScreenId(" AH.Main Page "))
    }

    @Test
    fun itemSlotRangesAreValidatedAgainstSnapshot() {
        assertTrue(GuiCaptureStore.validItemSlots("", 54))
        assertTrue(GuiCaptureStore.validItemSlots("0-44, 46, 50-53", 54))
        assertFalse(GuiCaptureStore.validItemSlots("0-54", 54))
        assertFalse(GuiCaptureStore.validItemSlots("9-2", 54))
        assertFalse(GuiCaptureStore.validItemSlots("listing", 54))
    }

    @Test
    fun roleIdsMatchThePublishedSchema() {
        assertEquals(
            setOf(
                "listing", "filler", "nav_next", "nav_prev", "page_indicator", "refresh",
                "tab", "filter", "sort", "confirm", "cancel", "close",
                "player_inventory", "hotbar", "unknown"
            ),
            GuiSlotRole.entries.map { it.id }.toSet()
        )
    }
}
