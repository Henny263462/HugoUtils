package dev.henny.hugoutils.sky

import dev.henny.hugoutils.client.ui.ConfigCategory
import dev.henny.hugoutils.client.ui.ConfigPage
import dev.henny.hugoutils.client.ui.HugoTheme
import dev.henny.hugoutils.client.ui.ModPageChrome
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.UiDraw
import dev.henny.hugoutils.ui.UiRect
import dev.henny.hugoutils.ui.UiWidgets
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class SkyConfigPage : ConfigPage {
    override val category = ConfigCategory.SKY
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var hero = UiRect(0, 0, 0, 0)
    private var refresh = UiRect(0, 0, 0, 0)
    private var grid = UiRect(0, 0, 0, 0)
    private var cards = emptyList<Pair<Int, UiRect>>()
    private var scroll = 0
    private var hoveredTip: String? = null
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val h = height.coerceAtLeast(280)
        frame = UiRect(x, y, width, h)
        hero = UiRect(x + 8, y + 8, width - 16, 58)
        refresh = UiRect(hero.right() - 96, hero.y + 18, 84, 20)
        grid = UiRect(x + 8, hero.bottom() + 10, width - 16, h - 84)
        updateCards()
        return frame.h
    }

    private fun updateCards() {
        val cols = if (grid.w >= 420) 3 else 2
        val cardW = ((grid.w - GAP * (cols - 1)) / cols).coerceAtLeast(110)
        val rowsVisible = ((grid.h + GAP) / (CARD_H + GAP)).coerceAtLeast(1)
        val totalRows = ((SkyManager.skies.size + cols - 1) / cols).coerceAtLeast(0)
        scroll = scroll.coerceIn(0, (totalRows - rowsVisible).coerceAtLeast(0))
        cards = SkyManager.skies.indices.map { index ->
            val col = index % cols
            val row = index / cols - scroll
            index to UiRect(grid.x + col * (cardW + GAP), grid.y + row * (CARD_H + GAP), cardW, CARD_H)
        }.filter { it.second.bottom() > grid.y && it.second.y < grid.bottom() }
    }

    override fun resetUi() {
        if (SkyManager.skies.isEmpty() && !SkyManager.scanning) SkyManager.scan()
    }

    override fun hoveredTooltip(): String? = hoveredTip

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        updateCards()
        hoveredTip = null
        val font = client.textRenderer
        val selected = SkyManager.selectedIndex()
        val current = selected.takeIf { it > 0 }?.let { SkyManager.skies.getOrNull(it - 1)?.name } ?: "Vanilla"
        ModPageChrome.hero(
            context, font, hero,
            "Sky",
            "Aktuell: $current  ·  ${SkyManager.skies.size} Himmel gefunden.",
            SkyManager.skies.isNotEmpty(), lastMouseX, lastMouseY
        )
        UiWidgets.button(
            context, font, refresh,
            if (SkyManager.scanning) "Suche…" else "Scannen",
            lastMouseX, lastMouseY,
            !SkyManager.scanning && !SkyManager.applying,
            ButtonStyle.SECONDARY
        )
        context.enableScissor(grid.x, grid.y, grid.right(), grid.bottom())
        if (cards.isEmpty()) {
            context.drawText(
                font,
                if (SkyManager.scanning) "Texturepacks werden durchsucht …"
                else "Keine Packs mit Sky-Texturen gefunden.",
                grid.x + 6,
                grid.y + 10,
                HugoTheme.textMuted,
                false
            )
        }
        cards.forEach { (index, rect) ->
            val sky = SkyManager.skies[index]
            val on = index + 1 == selected
            ModPageChrome.option(
                context, font, rect,
                "${index + 1}. ${sky.name}",
                sky.textures.joinToString { it.label }.ifBlank { "Nur Himmel" },
                selected = on, lastMouseX, lastMouseY
            )
            if (rect.contains(lastMouseX, lastMouseY)) {
                hoveredTip = "/sky ${index + 1}\n${sky.name}\nNur extrahierte Sky-Dateien werden übernommen."
            }
        }
        context.disableScissor()
        UiDraw.scrollbar(context, grid, scroll, (SkyManager.skies.size / 2).coerceAtLeast(0))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (refresh.contains(mouseX, mouseY) && !SkyManager.scanning && !SkyManager.applying) {
            SkyManager.scan()
            return true
        }
        cards.firstOrNull { it.second.contains(mouseX, mouseY) }?.let { (index, _) ->
            SkyManager.select(index + 1)
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!grid.contains(mouseX, mouseY)) return false
        val cols = if (grid.w >= 420) 3 else 2
        val visible = ((grid.h + GAP) / (CARD_H + GAP)).coerceAtLeast(1)
        val totalRows = ((SkyManager.skies.size + cols - 1) / cols).coerceAtLeast(0)
        scroll = (scroll - amount.toInt()).coerceIn(0, (totalRows - visible).coerceAtLeast(0))
        return true
    }

    override fun persist() = Unit

    private companion object {
        const val CARD_H = 64
        const val GAP = 8
    }
}
