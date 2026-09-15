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
    private val searchField = TextField(
        TextFieldLogic(maxLength = 80),
        placeholder = "Suchen …"
    ) { query ->
        searchList.query = query
        scroll = 0
        rebuildRows()
    }
    private val entrance = AnimatedFloat(0f, .18f).apply { animateTo(1f) }
    private val rowAnimations = mutableMapOf<T, AnimatedFloat>()
    private var selectedValue = selected
    private var frame = UiRect(0, 0, 0, 0)
    private var search = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)
    private val closeButton = Button("×", { UiOverlays.host.close() }, style = ButtonStyle.GHOST)
    private var rows = emptyList<Pair<T, UiRect>>()
    private var scroll = 0

    override fun layout(screenWidth: Int, screenHeight: Int) {
        val width = (screenWidth - 32).coerceIn(280, 430)
        val height = (screenHeight - 32).coerceIn(210, 360)
        frame = UiRect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height)
        search = UiRect(frame.x + 12, frame.y + 34, frame.w - 24, 22)
        searchField.bounds = search
        searchField.focused = true
        close = UiRect(frame.right - 24, frame.y + 8, 16, 16)
        closeButton.bounds = close
        rebuildRows()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        entrance.update(UiFrame.deltaSeconds)
        val t = UiDraw.theme
        UiDraw.fill(context, 0, 0, client.window.scaledWidth, client.window.scaledHeight, UiDraw.alpha(t.overlay, entrance.value))
        UiDraw.shadow(context, frame, entrance.value)
        UiDraw.panel(context, frame, t.panel, t.panelBorder)
        context.drawText(client.textRenderer, title, frame.x + 12, frame.y + 13, t.text, false)
        closeButton.render(context, client.textRenderer, mouseX, mouseY)
        searchField.render(context, client.textRenderer, mouseX, mouseY)
        rows.forEach { (value, rect) ->
            val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
            val animation = rowAnimations.getOrPut(value) { AnimatedFloat(0f, .1f) }
            animation.animateTo(if (hovered || value == selectedValue) 1f else 0f)
            animation.update(UiFrame.deltaSeconds)
            UiDraw.fill(context, rect, Theme.lerpColor(t.inset, t.accentSoft, animation.value))
            UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, Theme.lerpColor(t.border, t.accent, animation.value))
            context.drawText(client.textRenderer, UiDraw.ellipsize(client.textRenderer, label(value), rect.w - 10), rect.x + 5, rect.y + 6, t.text, false)
        }
        context.drawText(client.textRenderer, "${searchList.filtered.size} Treffer", frame.x + 12, frame.bottom - 15, t.textDim, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (closeButton.mouseClicked(mouseX, mouseY) || !frame.contains(mouseX, mouseY)) {
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
            else -> return searchField.keyPressed(
                input,
                { client.keyboard.clipboard },
                { client.keyboard.clipboard = it }
            )
        }
        return true
    }
    override fun charTyped(input: CharInput): Boolean {
        return searchField.charTyped(input)
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
