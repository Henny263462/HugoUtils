package dev.henny.hugoutils.client.gui.capture

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput

object GuiCaptureHooks {
    @JvmStatic
    fun render(screen: HandledScreen<*>, context: DrawContext, mouseX: Int, mouseY: Int) {
        GuiCaptureController.activeFor(screen)?.render(context, mouseX, mouseY)
    }

    @JvmStatic
    fun mouseClicked(screen: HandledScreen<*>, click: Click): Boolean {
        val overlay = GuiCaptureController.activeFor(screen) ?: return false
        overlay.mouseClicked(click.x(), click.y())
        return true
    }

    @JvmStatic
    fun blocksSlotClicks(screen: HandledScreen<*>): Boolean =
        GuiCaptureController.activeFor(screen) != null

    @JvmStatic
    fun keyPressed(screen: HandledScreen<*>, input: KeyInput): Boolean {
        if (GuiCaptureController.handleCaptureKey(screen, input)) return true
        return GuiCaptureController.activeFor(screen)?.keyPressed(input) ?: false
    }

    @JvmStatic
    fun charTyped(screen: HandledScreen<*>, input: CharInput): Boolean =
        GuiCaptureController.activeFor(screen)?.charTyped(input) ?: false
}
