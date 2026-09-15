package dev.henny.hugoutils.ui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack

/**
 * Reusable page adapter for incrementally extracting existing screens without
 * coupling the navigation shell to their implementation.
 */
class HostedPage(
    override val id: String,
    private val layoutHandler: (Int, Int, Int, Int) -> Int,
    private val renderHandler: (DrawContext, Int, Int) -> Unit,
    private val clickHandler: (Double, Double) -> Boolean = { _, _ -> false },
    private val dragHandler: (Double, Double) -> Boolean = { _, _ -> false },
    private val releaseHandler: () -> Unit = {},
    private val scrollHandler: (Double, Double, Double) -> Boolean = { _, _, _ -> false },
    private val keyHandler: (KeyInput) -> Boolean = { false },
    private val charHandler: (CharInput) -> Boolean = { false },
    private val resetHandler: () -> Unit = {},
    private val stackHandler: () -> ItemStack? = { null },
    private val persistHandler: () -> Unit = {}
) : UiPage {
    override fun layout(x: Int, y: Int, width: Int, height: Int) = layoutHandler(x, y, width, height)
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) = renderHandler(context, mouseX, mouseY)
    override fun mouseClicked(mouseX: Double, mouseY: Double) = clickHandler(mouseX, mouseY)
    override fun mouseDragged(mouseX: Double, mouseY: Double) = dragHandler(mouseX, mouseY)
    override fun mouseReleased() = releaseHandler()
    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double) = scrollHandler(mouseX, mouseY, amount)
    override fun keyPressed(input: KeyInput) = keyHandler(input)
    override fun charTyped(input: CharInput) = charHandler(input)
    override fun resetUi() = resetHandler()
    override fun hoveredStack() = stackHandler()
    override fun persist() = persistHandler()
}
