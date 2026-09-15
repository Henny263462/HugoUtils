package dev.henny.hugoutils.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW

open class SearchListPopup<T>(
    values: List<T>,
    selected: T?,
    private val title: String,
    private val label: (T) -> String = { it.toString() },
    private val onSelect: (T) -> Unit
) : Popup {
    private val client = MinecraftClient.getInstance()
    private val searchList = SearchList(values, label)
    private var selectedValue = selected
    private var frame = UiRect(0, 0, 0, 0)
    private var search = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)
    private var rows = emptyList<Pair<T, UiRect>>()
    private var scroll = 0

    override fun layout(screenWidth: Int, screenHeight: Int) {
        val width = (screenWidth - 32).coerceIn(280, 430)
        val height = (screenHeight - 32).coerceIn(210, 360)
        frame = UiRect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height)
        search = UiRect(frame.x + 12, frame.y + 34, frame.w - 24, 22)
        close = UiRect(frame.right - 24, frame.y + 8, 16, 16)
        rebuildRows()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        val t = UiDraw.theme
        UiDraw.fill(context, 0, 0, client.window.scaledWidth, client.window.scaledHeight, 0xB0000000.toInt())
        UiDraw.panel(context, frame, t.panel, t.panelBorder)
        context.drawText(client.textRenderer, title, frame.x + 12, frame.y + 13, t.text, false)
        context.drawText(client.textRenderer, "×", close.x + 4, close.y + 3, t.textMuted, false)
        UiDraw.panel(context, search, t.inset, t.accent)
        val shown = searchList.query.ifEmpty { "Suchen …" }
        context.drawText(client.textRenderer, UiDraw.ellipsize(client.textRenderer, shown, search.w - 10), search.x + 5, search.y + 7, if (searchList.query.isEmpty()) t.textDim else t.text, false)
        rows.forEach { (value, rect) ->
            val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
            UiDraw.fill(context, rect, if (value == selectedValue) t.accentSoft else if (hovered) 0x18FFFFFF else t.inset)
            UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, if (value == selectedValue || hovered) t.accent else t.border)
            context.drawText(client.textRenderer, UiDraw.ellipsize(client.textRenderer, label(value), rect.w - 10), rect.x + 5, rect.y + 6, t.text, false)
        }
        context.drawText(client.textRenderer, "${searchList.filtered.size} Treffer", frame.x + 12, frame.bottom - 15, t.textDim, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (close.contains(mouseX, mouseY) || !frame.contains(mouseX, mouseY)) {
            UiOverlays.host.close()
            return true
        }
        rows.firstOrNull { it.second.contains(mouseX, mouseY) }?.let {
            selectedValue = it.first
            onSelect(it.first)
            UiOverlays.host.close()
        }
        return true
    }
    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!frame.contains(mouseX, mouseY)) return false
        scroll = (scroll - amount.toInt()).coerceIn(0, (searchList.filtered.size - visibleRows()).coerceAtLeast(0))
        rebuildRows()
        return true
    }
    override fun keyPressed(input: KeyInput): Boolean {
        when {
            input.key() == GLFW.GLFW_KEY_ESCAPE -> UiOverlays.host.close()
            input.key() == GLFW.GLFW_KEY_BACKSPACE -> searchList.query = searchList.query.dropLast(1)
            input.isPaste -> searchList.query = client.keyboard.clipboard.take(80)
            else -> return false
        }
        scroll = 0
        rebuildRows()
        return true
    }
    override fun charTyped(input: CharInput): Boolean {
        if (!input.isValidChar || searchList.query.length >= 80) return false
        searchList.query += input.asString()
        scroll = 0
        rebuildRows()
        return true
    }
    private fun visibleRows() = ((frame.h - 84) / 22).coerceAtLeast(1)
    private fun rebuildRows() {
        val values = searchList.filtered
        scroll = scroll.coerceIn(0, (values.size - visibleRows()).coerceAtLeast(0))
        rows = values.drop(scroll).take(visibleRows()).mapIndexed { index, value ->
            value to UiRect(frame.x + 12, search.bottom + 8 + index * 22, frame.w - 24, 20)
        }
    }
}

class ParticleTypePopup(
    particleIds: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) : SearchListPopup<String>(particleIds, selected, "Partikeltyp suchen", { it }, onSelect)
