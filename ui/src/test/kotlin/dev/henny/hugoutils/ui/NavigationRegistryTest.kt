package dev.henny.hugoutils.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NavigationRegistryTest {
    @Test
    fun `orders roots and resolves parent children`() {
        val registry = NavigationRegistry()
        registry.register(NavigationEntry("tools", "Tools", order = 20))
        registry.register(NavigationEntry("visuals", "Visuals", order = 10))
        registry.register(NavigationEntry("glow", "Glow", parentId = "visuals", order = 2))
        registry.register(NavigationEntry("blocks", "Blocks", parentId = "visuals", order = 1))

        assertEquals(listOf("visuals", "tools"), registry.roots().map { it.id })
        assertEquals(listOf("blocks", "glow"), registry.children("visuals").map { it.id })
    }

    @Test
    fun `rejects missing page entry`() {
        val registry = NavigationRegistry()
        assertThrows(IllegalArgumentException::class.java) {
            registry.registerPage("missing", FakePage)
        }
    }

    private object FakePage : UiPage {
        override val id = "fake"
        override fun layout(x: Int, y: Int, width: Int, height: Int) = 0
        override fun render(context: net.minecraft.client.gui.DrawContext, mouseX: Int, mouseY: Int) = Unit
    }
}
