package dev.henny.hugoutils.client.ui

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.gui.screen.GameMenuScreen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

object MenuButtons {
    private val LABEL = Text.literal("HugoUtils")

    fun initialize() {
        ScreenEvents.AFTER_INIT.register { client, screen, _, _ ->
            if (screen !is TitleScreen && screen !is GameMenuScreen) return@register
            if (Screens.getButtons(screen).any { it.message.string == LABEL.string }) return@register
            Screens.getButtons(screen).add(
                ButtonWidget.builder(LABEL) {
                    HugoScreen.open(client, parent = screen)
                }.dimensions(4, 4, 100, 20).build()
            )
        }
    }
}
