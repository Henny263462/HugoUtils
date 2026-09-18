package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.gui.capture.GuiCaptureController
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.ui.SettingHero
import dev.henny.hugoutils.ui.SettingNote
import dev.henny.hugoutils.ui.SettingToggle
import dev.henny.hugoutils.ui.SettingsColumn
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class GuiCaptureConfigPage : ConfigPage {
    override val category = ConfigCategory.TOOLS
    private val client = MinecraftClient.getInstance()
    private val column = SettingsColumn()
    private val hero = SettingHero(
        "capture-hero",
        "GUI Capture",
        detail = { GuiCaptureController.statusText().ifBlank { "Container mit F8 aufnehmen und lokal exportieren." } },
        enabled = { ConfigManager.config.uiDebugCommandsEnabled }
    )
    private val steps = SettingNote(
        "capture-steps",
        listOf(
            "1. Container öffnen und F8 drücken",
            "2. screenId und Metadaten eintragen",
            "3. Slots anklicken und Rollen beschreiben",
            "4. Export schreibt capture.json lokal",
            "Pfad: config/hugoutils/gui-knowledge/"
        )
    )
    private val debug = SettingToggle(
        "capture-debug",
        "UI-Debug-Commands",
        "/hugoutils-ui  ·  Änderung nach Neustart.",
        read = { ConfigManager.config.uiDebugCommandsEnabled },
        write = { value -> ConfigManager.update { it.uiDebugCommandsEnabled = value } }
    )

    init {
        column.add(hero).add(steps).add(debug)
    }

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        return column.layout(x + 8, y + 8, width - 16) + 16
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        column.render(context, client.textRenderer, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean =
        column.mouseClicked(mouseX, mouseY)

    override fun persist() = ConfigManager.save()
}
