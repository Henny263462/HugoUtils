package dev.henny.hugoutils.client.ui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack

interface ConfigPage {
    val category: ConfigCategory

    fun layout(x: Int, y: Int, width: Int, height: Int): Int

    fun render(context: DrawContext, mouseX: Int, mouseY: Int)

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseReleased() {}
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false
    fun keyPressed(input: KeyInput): Boolean = false
    fun charTyped(input: CharInput): Boolean = false

    fun resetUi() {}

    fun hoveredStack(): ItemStack? = null

    fun persist()
}
