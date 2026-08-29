package dev.henny.hugoutils.client.ui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack

interface ConfigPopup {
    fun layout(screenWidth: Int, screenHeight: Int)
    fun render(context: DrawContext, mouseX: Int, mouseY: Int)
    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean
    fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseReleased() {}
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false
    fun keyPressed(input: KeyInput): Boolean = false
    fun charTyped(input: CharInput): Boolean = false
    fun hoveredStack(): ItemStack? = null
}

object PopupManager {
    var active: ConfigPopup? = null
        private set

    fun open(popup: ConfigPopup) {
        active = popup
    }

    fun close() {
        active = null
    }
}
