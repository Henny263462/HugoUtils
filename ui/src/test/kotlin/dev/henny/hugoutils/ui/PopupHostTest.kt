package dev.henny.hugoutils.ui

import net.minecraft.client.gui.DrawContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class PopupHostTest {
    private class TestPopup : Popup {
        override fun layout(screenWidth: Int, screenHeight: Int) = Unit
        override fun render(context: DrawContext, mouseX: Int, mouseY: Int) = Unit
    }

    @Test
    fun `popups form a last-in-first-out stack`() {
        val host = PopupHost()
        val first = TestPopup()
        val second = TestPopup()

        host.open(first)
        host.open(second)
        assertEquals(2, host.popupCount)
        assertSame(second, host.active)

        host.close()
        assertSame(first, host.active)
        host.close()
        assertNull(host.active)
    }
}
