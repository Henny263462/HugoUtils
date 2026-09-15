package dev.henny.hugoutils.client.gui.capture

import dev.henny.hugoutils.mixin.GuiCaptureHandledScreenAccessor
import net.minecraft.client.gui.screen.ingame.HandledScreen

object GuiCaptureScreenGeometry {
    fun x(screen: HandledScreen<*>): Int =
        (screen as GuiCaptureHandledScreenAccessor).`hugoutils$guiCaptureX`()

    fun y(screen: HandledScreen<*>): Int =
        (screen as GuiCaptureHandledScreenAccessor).`hugoutils$guiCaptureY`()

    fun backgroundWidth(screen: HandledScreen<*>): Int =
        (screen as GuiCaptureHandledScreenAccessor).`hugoutils$guiCaptureBackgroundWidth`()

    fun backgroundHeight(screen: HandledScreen<*>): Int =
        (screen as GuiCaptureHandledScreenAccessor).`hugoutils$guiCaptureBackgroundHeight`()
}
