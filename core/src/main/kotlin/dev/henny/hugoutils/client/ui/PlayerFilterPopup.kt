package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.PlayerFilter
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW
import java.util.UUID

class PlayerFilterPopup(
    private val filter: PlayerFilter,
    private val onChange: () -> Unit
) : ConfigPopup {
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)
    private var input = UiRect(0, 0, 0, 0)
    private var add = UiRect(0, 0, 0, 0)
    private var inputText = ""
    private var focused = false
    private var modeHits = emptyList<Pair<PlayerFilter.Mode, UiRect>>()
    private val playerHits = ArrayList<Triple<String, UUID?, UiRect>>()

    override fun layout(screenWidth: Int, screenHeight: Int) {
        val width = (screenWidth - 32).coerceIn(320, 480)
        val height = (screenHeight - 32).coerceIn(220, 330)
        frame = UiRect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height)
        close = UiRect(frame.right() - 24, frame.y + 8, 16, 16)
        val chipWidth = (frame.w - 32) / 3
        modeHits = PlayerFilter.Mode.entries.mapIndexed { index, mode ->
            mode to UiRect(frame.x + 10 + index * (chipWidth + 4), frame.y + 34, chipWidth, 18)
        }
        input = UiRect(frame.x + 10, frame.y + 60, frame.w - 82, 18)
        add = UiRect(input.right() + 6, input.y, 56, 18)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        UiDraw.fill(context, 0, 0, client.window.scaledWidth, client.window.scaledHeight, 0xB0000000.toInt())
        UiDraw.panel(context, frame, HugoTheme.panel, HugoTheme.panelBorder)
        context.drawText(client.textRenderer, "Spieler auswählen", frame.x + 12, frame.y + 13, HugoTheme.text, false)
        context.drawText(client.textRenderer, "×", close.x + 4, close.y + 3, HugoTheme.textMuted, false)
        val active = PlayerFilter.Mode.from(filter.mode)
        modeHits.forEach { (mode, rect) ->
            UiDraw.chip(context, client.textRenderer, rect, mode.label, mode == active, rect.contains(mouseX.toDouble(), mouseY.toDouble()))
        }
        UiDraw.panel(context, input, HugoTheme.inset, if (focused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = inputText.ifBlank { "Name oder UUID…" }
        context.drawText(client.textRenderer, UiDraw.ellipsize(client.textRenderer, shown, input.w - 8), input.x + 4, input.y + 5,
            if (inputText.isBlank()) HugoTheme.textDim else HugoTheme.text, false)
        UiDraw.panel(context, add, HugoTheme.inset, HugoTheme.cardBorder)
        context.drawText(client.textRenderer, "Hinzufügen", add.x + 4, add.y + 5, HugoTheme.textMuted, false)

        playerHits.clear()
        val online = client.networkHandler?.playerList.orEmpty()
            .map { it.profile.name to it.profile.id }
            .sortedBy { it.first.lowercase() }
        val combined = LinkedHashMap<String, Pair<String, UUID?>>()
        online.forEach { combined[it.second.toString()] = it }
        filter.players.forEach { entry ->
            val uuid = PlayerFilter.parseUuid(entry.uuid)
            combined.putIfAbsent(uuid?.toString() ?: entry.name.lowercase(), entry.name to uuid)
        }
        var y = input.bottom() + 10
        context.drawText(client.textRenderer, "Online und ausgewählt", frame.x + 10, y, HugoTheme.textMuted, false)
        y += 14
        for ((name, uuid) in combined.values) {
            if (y + 18 > frame.bottom() - 10) break
            val rect = UiRect(frame.x + 10, y, frame.w - 20, 18)
            playerHits += Triple(name, uuid, rect)
            val selected = filter.players.any {
                (uuid != null && PlayerFilter.parseUuid(it.uuid) == uuid) || it.name.equals(name, true)
            }
            UiDraw.fill(context, rect, if (selected) HugoTheme.accentSoft else HugoTheme.inset)
            UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, if (selected) HugoTheme.accent else HugoTheme.cardBorder)
            context.drawText(client.textRenderer, name.ifBlank { uuid.toString() }, rect.x + 6, rect.y + 5, HugoTheme.text, false)
            y += 20
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (close.contains(mouseX, mouseY) || !frame.contains(mouseX, mouseY)) {
            PopupManager.close()
            return true
        }
        modeHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let {
            filter.mode = it.first.id
            onChange()
            return true
        }
        focused = input.contains(mouseX, mouseY)
        if (add.contains(mouseX, mouseY)) {
            addManual()
            return true
        }
        playerHits.firstOrNull { it.third.contains(mouseX, mouseY) }?.let {
            filter.toggle(it.first, it.second)
            onChange()
            return true
        }
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            PopupManager.close()
            return true
        }
        if (!focused) return false
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (inputText.isNotEmpty()) inputText = inputText.dropLast(1)
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                addManual()
                true
            }
            else -> false
        }
    }

    override fun charTyped(input: CharInput): Boolean {
        if (!focused || !input.isValidChar || inputText.length >= 36) return false
        inputText += input.asString()
        return true
    }

    private fun addManual() {
        val value = inputText.trim()
        if (value.isBlank()) return
        val uuid = PlayerFilter.parseUuid(value)
        filter.toggle(if (uuid == null) value else "", uuid)
        inputText = ""
        onChange()
    }
}
