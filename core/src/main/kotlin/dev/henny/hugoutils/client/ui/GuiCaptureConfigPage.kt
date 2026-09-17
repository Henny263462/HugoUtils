package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.gui.capture.GuiCaptureController
import dev.henny.hugoutils.client.config.ConfigManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class GuiCaptureConfigPage : ConfigPage {
    override val category = ConfigCategory.TOOLS
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var hero = UiRect(0, 0, 0, 0)
    private var steps = UiRect(0, 0, 0, 0)
    private var debugCard = UiRect(0, 0, 0, 0)
    private var debugToggle = UiRect(0, 0, 0, 0)
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, 250)
        hero = UiRect(x + 8, y + 8, width - 16, 58)
        steps = UiRect(x + 8, hero.bottom() + 8, width - 16, 102)
        debugCard = UiRect(x + 8, steps.bottom() + 8, width - 16, 64)
        debugToggle = UiRect(debugCard.right() - 42, debugCard.y + 14, 32, 14)
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        val font = client.textRenderer
        ModPageChrome.hero(
            context, font, hero,
            "GUI Capture",
            GuiCaptureController.statusText().ifBlank { "Container mit F8 aufnehmen und lokal exportieren." },
            ConfigManager.config.uiDebugCommandsEnabled, lastMouseX, lastMouseY
        )
        UiWidgets.hoverCard(context, steps, lastMouseX, lastMouseY, key = "capture-steps")
        listOf(
            "1. Container öffnen und F8 drücken",
            "2. screenId und Metadaten eintragen",
            "3. Slots anklicken und Rollen beschreiben",
            "4. Export schreibt capture.json lokal",
            "Pfad: config/hugoutils/gui-knowledge/"
        ).forEachIndexed { index, line ->
            context.drawText(font, line, steps.x + 12, steps.y + 10 + index * 16, HugoTheme.textMuted, false)
        }
        ModPageChrome.option(
            context, font, debugCard,
            "UI-Debug-Commands",
            "/hugoutils-ui  ·  Änderung nach Neustart.",
            ConfigManager.config.uiDebugCommandsEnabled,
            lastMouseX, lastMouseY, debugToggle, ConfigManager.config.uiDebugCommandsEnabled
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (debugToggle.contains(mouseX, mouseY) || debugCard.contains(mouseX, mouseY)) {
            ConfigManager.update { it.uiDebugCommandsEnabled = !it.uiDebugCommandsEnabled }
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun persist() = ConfigManager.save()
}
