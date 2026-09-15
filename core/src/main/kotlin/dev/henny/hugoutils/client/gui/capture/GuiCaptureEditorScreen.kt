package dev.henny.hugoutils.client.gui.capture

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput

class GuiCaptureEditorScreen(val parent: Screen) : Screen(parent.title) {
    override fun init() {
        super.init()
        parent.resize(width, height)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        parent.render(context, mouseX, mouseY, deltaTicks)
        GuiCaptureController.activeFor(parent)?.render(context, mouseX, mouseY)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val overlay = GuiCaptureController.activeFor(parent) ?: return true
        overlay.mouseClicked(click.x(), click.y())
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean =
        GuiCaptureController.activeFor(parent)?.keyPressed(input) ?: true

    override fun charTyped(input: CharInput): Boolean =
        GuiCaptureController.activeFor(parent)?.charTyped(input) ?: true

    override fun close() {
        GuiCaptureController.stop()
    }

    override fun shouldPause(): Boolean = parent.shouldPause()
}
