package dev.henny.hugoutils.client.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW

class ParticleTypePopup(
    private val particleIds: List<String>,
    private val selected: String,
    private val onSelect: (String) -> Unit
) : ConfigPopup {
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var search = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)
    private var rows = emptyList<Pair<String, UiRect>>()
    private var query = ""
    private var scroll = 0

    override fun layout(screenWidth: Int, screenHeight: Int) {
        val width = (screenWidth - 32).coerceIn(280, 430)
        val height = (screenHeight - 32).coerceIn(210, 360)
        frame = UiRect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height)
        search = UiRect(frame.x + 12, frame.y + 34, frame.w - 24, 22)
        close = UiRect(frame.right() - 24, frame.y + 8, 16, 16)
        rebuildRows()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        UiDraw.fill(context, 0, 0, client.window.scaledWidth, client.window.scaledHeight, 0xB0000000.toInt())
        UiDraw.panel(context, frame, HugoTheme.panel, HugoTheme.panelBorder)
        context.drawText(client.textRenderer, "Partikeltyp suchen", frame.x + 12, frame.y + 13, HugoTheme.text, false)
        context.drawText(client.textRenderer, "×", close.x + 4, close.y + 3, HugoTheme.textMuted, false)
        UiDraw.panel(context, search, HugoTheme.inset, HugoTheme.accent)
        val shown = query.ifEmpty { "z. B. flame, portal, smoke …" }
        context.drawText(
            client.textRenderer,
            UiDraw.ellipsize(client.textRenderer, shown, search.w - 10),
            search.x + 5,
            search.y + 7,
            if (query.isEmpty()) HugoTheme.textDim else HugoTheme.text,
            false
        )
        rows.forEach { (id, rect) ->
            val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
            UiDraw.fill(context, rect, if (id == selected) HugoTheme.accentSoft else if (hovered) 0x18FFFFFF else HugoTheme.inset)
            UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, if (id == selected || hovered) HugoTheme.accent else HugoTheme.cardBorder)
            context.drawText(client.textRenderer, UiDraw.ellipsize(client.textRenderer, id, rect.w - 10), rect.x + 5, rect.y + 6, HugoTheme.text, false)
        }
        val count = filtered().size
        context.drawText(client.textRenderer, "$count Treffer", frame.x + 12, frame.bottom() - 15, HugoTheme.textDim, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (close.contains(mouseX, mouseY) || !frame.contains(mouseX, mouseY)) {
            PopupManager.close()
            return true
        }
        rows.firstOrNull { it.second.contains(mouseX, mouseY) }?.let {
            onSelect(it.first)
            PopupManager.close()
        }
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!frame.contains(mouseX, mouseY)) return false
        val visible = visibleRows()
        scroll = (scroll - amount.toInt()).coerceIn(0, (filtered().size - visible).coerceAtLeast(0))
        rebuildRows()
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        when {
            input.key() == GLFW.GLFW_KEY_ESCAPE -> PopupManager.close()
            input.key() == GLFW.GLFW_KEY_BACKSPACE -> {
                if (query.isNotEmpty()) query = query.dropLast(1)
                scroll = 0
                rebuildRows()
            }
            input.isPaste -> {
                query = client.keyboard.clipboard.take(80)
                scroll = 0
                rebuildRows()
            }
            else -> return false
        }
        return true
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!input.isValidChar || query.length >= 80) return false
        query += input.asString()
        scroll = 0
        rebuildRows()
        return true
    }

    private fun filtered(): List<String> {
        val needle = query.trim().lowercase()
        return if (needle.isEmpty()) particleIds else particleIds.filter { needle in it.lowercase() }
    }

    private fun visibleRows(): Int = ((frame.h - 84) / 22).coerceAtLeast(1)

    private fun rebuildRows() {
        val values = filtered()
        scroll = scroll.coerceIn(0, (values.size - visibleRows()).coerceAtLeast(0))
        rows = values.drop(scroll).take(visibleRows()).mapIndexed { index, id ->
            id to UiRect(frame.x + 12, search.bottom() + 8 + index * 22, frame.w - 24, 20)
        }
    }
}
