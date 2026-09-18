package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

abstract class Control(var bounds: UiRect = UiRect(0, 0, 0, 0)) {
    var enabled: Boolean = true
    var visible: Boolean = true
    var tooltip: String? = null
    protected val interaction = InteractionState()
    open fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {}
    open fun mouseClicked(mouseX: Double, mouseY: Double): Boolean = false
    open fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    open fun mouseReleased() {}
}

enum class ButtonStyle { PRIMARY, SECONDARY, GHOST, DANGER }
