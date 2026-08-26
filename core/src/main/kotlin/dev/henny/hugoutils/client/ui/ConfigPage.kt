package dev.henny.hugoutils.client.ui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack

/**
 * Contract for a feature-specific config page rendered inside [HugoScreen].
 * Modules register their page via [ConfigPages].
 */
interface ConfigPage {
    val category: ConfigCategory

    /** Called once when the category is selected or the screen is resized. Return the content height in pixels. */
    fun layout(x: Int, y: Int, width: Int, height: Int): Int

    /** Called each frame. */
    fun render(context: DrawContext, mouseX: Int, mouseY: Int)

    /** @return true if the event was consumed. */
    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseReleased() {}
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false
    fun keyPressed(input: KeyInput): Boolean = false
    fun charTyped(input: CharInput): Boolean = false

    /** Reset transient UI state when the screen opens (e.g. collapse sections). */
    fun resetUi() {}

    /** Item currently hovered in this page, if any. */
    fun hoveredStack(): ItemStack? = null

    /** Persist changes (e.g. save JSON config). */
    fun persist()
}
