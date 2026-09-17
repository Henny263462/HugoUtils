package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.PerspectiveMode
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class PerspectiveConfigPage : ConfigPage {
    override val category = ConfigCategory.PERSPECTIVE
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var hero = UiRect(0, 0, 0, 0)
    private var choices = emptyList<Pair<PerspectiveMode, UiRect>>()
    private var crosshairCard = UiRect(0, 0, 0, 0)
    private var nameCard = UiRect(0, 0, 0, 0)
    private var crosshairToggle = UiRect(0, 0, 0, 0)
    private var playerNameToggle = UiRect(0, 0, 0, 0)
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, 268)
        hero = UiRect(x + 8, y + 8, width - 16, 52)
        val gap = 8
        val cardW = ((width - 16 - gap * 2) / 3).coerceAtLeast(96)
        choices = PerspectiveMode.entries.mapIndexed { index, mode ->
            mode to UiRect(x + 8 + index * (cardW + gap), hero.bottom() + 10, cardW, 78)
        }
        val extraY = (choices.lastOrNull()?.second?.bottom() ?: hero.bottom()) + 10
        val extraW = ((width - 16 - gap) / 2).coerceAtLeast(120)
        crosshairCard = UiRect(x + 8, extraY, extraW, 64)
        nameCard = UiRect(crosshairCard.right() + gap, extraY, width - 16 - extraW - gap, 64)
        crosshairToggle = UiRect(crosshairCard.right() - 42, crosshairCard.y + 12, 32, 14)
        playerNameToggle = UiRect(nameCard.right() - 42, nameCard.y + 12, 32, 14)
        return nameCard.bottom() + 8 - y
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        val font = client.textRenderer
        val selected = PerspectiveMode.fromId(ConfigManager.config.perspectiveMode)
        ModPageChrome.hero(
            context, font, hero,
            "F5-Perspektive",
            "Reihenfolge der F5-Taste und HUD in der Third-Person-Ansicht.",
            null, lastMouseX, lastMouseY
        )
        for ((mode, rect) in choices) {
            ModPageChrome.option(
                context, font, rect, mode.label, mode.description,
                selected = mode == selected, lastMouseX, lastMouseY
            )
        }
        ModPageChrome.option(
            context, font, crosshairCard,
            "Crosshair",
            "Fadenkreuz in F5 anzeigen.",
            ConfigManager.config.showCrosshairInThirdPerson,
            lastMouseX, lastMouseY, crosshairToggle, ConfigManager.config.showCrosshairInThirdPerson
        )
        ModPageChrome.option(
            context, font, nameCard,
            "Name",
            "Eigenen Spielernamen in F5 zeigen.",
            ConfigManager.config.showOwnNameInThirdPerson,
            lastMouseX, lastMouseY, playerNameToggle, ConfigManager.config.showOwnNameInThirdPerson
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        choices.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (mode, _) ->
            ConfigManager.update { it.perspectiveMode = mode.id }
            return true
        }
        if (crosshairToggle.contains(mouseX, mouseY) || crosshairCard.contains(mouseX, mouseY)) {
            ConfigManager.update { it.showCrosshairInThirdPerson = !it.showCrosshairInThirdPerson }
            return true
        }
        if (playerNameToggle.contains(mouseX, mouseY) || nameCard.contains(mouseX, mouseY)) {
            ConfigManager.update { it.showOwnNameInThirdPerson = !it.showOwnNameInThirdPerson }
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun persist() = ConfigManager.requestSave()
}
