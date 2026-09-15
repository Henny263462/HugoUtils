package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.ui.UiPage
import net.minecraft.client.gui.DrawContext

/**
 * Registered page boundary for the four legacy visual editors. Their shared
 * card renderer is supplied by the screen-owned session controller so picker,
 * drag and popup state survives page switches.
 */
internal class VisualConfigPage(
    override val id: String,
    private val layoutPage: (ConfigCategory, Int, Int, Int, Int) -> Int,
    private val renderPage: (ConfigCategory, DrawContext, Int, Int) -> Unit,
    private val category: ConfigCategory
) : UiPage {
    override fun layout(x: Int, y: Int, width: Int, height: Int): Int =
        layoutPage(category, x, y, width, height)

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) =
        renderPage(category, context, mouseX, mouseY)
}
