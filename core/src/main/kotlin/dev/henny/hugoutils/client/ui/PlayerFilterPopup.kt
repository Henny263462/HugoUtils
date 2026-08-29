package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.PlayerFilter
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.util.math.ColorHelper
import org.lwjgl.glfw.GLFW
import java.util.UUID
import kotlin.math.roundToInt

class PlayerFilterPopup(
    private val filter: PlayerFilter,
    private val onChange: () -> Unit
) : ConfigPopup {
    private val client = MinecraftClient.getInstance()
    private val colorPicker = ColorPicker(client.textRenderer) {
        editingEntry?.customColor = true
        onChange()
    }
    private var frame = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)
    private var input = UiRect(0, 0, 0, 0)
    private var add = UiRect(0, 0, 0, 0)
    private var resetColor = UiRect(0, 0, 0, 0)
    private var listArea = UiRect(0, 0, 0, 0)
    private var inputText = ""
    private var focused = false
    private var modeHits = emptyList<Pair<PlayerFilter.Mode, UiRect>>()
    private val playerHits = ArrayList<PlayerHit>()
    private var editingKey: String? = null
    private var scroll = 0
    private var maxScroll = 0
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    private val editingEntry: PlayerFilter.Entry?
        get() = editingKey?.let { key ->
            filter.players.firstOrNull { entryKey(it) == key }
        }

    override fun layout(screenWidth: Int, screenHeight: Int) {
        val width = (screenWidth - 24).coerceIn(360, 520)
        val height = (screenHeight - 24).coerceIn(280, 400)
        frame = UiRect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height)
        close = UiRect(frame.right() - 24, frame.y + 8, 16, 16)
        val chipWidth = (frame.w - 32) / 3
        modeHits = PlayerFilter.Mode.entries.mapIndexed { index, mode ->
            mode to UiRect(frame.x + 10 + index * (chipWidth + 4), frame.y + 34, chipWidth, 18)
        }
        input = UiRect(frame.x + 10, frame.y + 60, frame.w - 82, 18)
        add = UiRect(input.right() + 6, input.y, 56, 18)

        val pickerW = 100
        val pickerH = 64
        colorPicker.layout(frame.x + 12, frame.bottom() - colorPickerReserve() + 8, pickerW, pickerH)
        resetColor = UiRect(
            colorPicker.hexBox.right() + 48,
            colorPicker.hexBox.y,
            72,
            16
        )

        val listTop = input.bottom() + 24
        val listBottom = if (editingEntry != null) {
            frame.bottom() - colorPickerReserve() - 4
        } else {
            frame.bottom() - 10
        }
        listArea = UiRect(frame.x + 10, listTop, frame.w - 20, (listBottom - listTop).coerceAtLeast(40))
        recomputeScroll()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        val font = client.textRenderer
        val style = ConfigManager.config.playerGlow

        UiDraw.fill(context, 0, 0, client.window.scaledWidth, client.window.scaledHeight, 0xB0000000.toInt())
        UiDraw.panel(context, frame, HugoTheme.panel, HugoTheme.panelBorder)
        context.drawText(font, "Spieler & Farben", frame.x + 12, frame.y + 13, HugoTheme.text, false)
        context.drawText(font, "×", close.x + 4, close.y + 3, HugoTheme.textMuted, false)

        val active = PlayerFilter.Mode.from(filter.mode)
        modeHits.forEach { (mode, rect) ->
            UiDraw.chip(context, font, rect, mode.label, mode == active, rect.contains(lastMouseX, lastMouseY))
        }

        UiDraw.panel(context, input, HugoTheme.inset, if (focused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = inputText.ifBlank { "Name oder UUID…" }
        context.drawText(
            font,
            UiDraw.ellipsize(font, shown, input.w - 8),
            input.x + 4,
            input.y + 5,
            if (inputText.isBlank()) HugoTheme.textDim else HugoTheme.text,
            false
        )
        val addHovered = add.contains(lastMouseX, lastMouseY)
        UiDraw.panel(context, add, if (addHovered) HugoTheme.accentSoft else HugoTheme.inset,
            if (addHovered) HugoTheme.accent else HugoTheme.cardBorder)
        context.drawText(font, "Add", add.x + (add.w - font.getWidth("Add")) / 2, add.y + 5, HugoTheme.textMuted, false)

        context.drawText(font, "Online / ausgewählt — Farbe antippen", listArea.x, listArea.y - 12, HugoTheme.textMuted, false)

        playerHits.clear()
        val rows = buildRows()
        recomputeScroll(rows.size)
        UiDraw.panel(context, listArea, HugoTheme.inset, HugoTheme.cardBorder)
        context.enableScissor(listArea.x, listArea.y, listArea.right(), listArea.bottom())
        var y = listArea.y + 2 - scroll
        if (rows.isEmpty()) {
            context.drawText(
                font,
                "Keine Spieler online. Name eingeben oder warten.",
                listArea.x + 8,
                listArea.y + listArea.h / 2 - 4,
                HugoTheme.textDim,
                false
            )
        } else {
            for (row in rows) {
                val rect = UiRect(listArea.x + 2, y, listArea.w - 4, ROW_H)
                if (rect.bottom() >= listArea.y && rect.y <= listArea.bottom()) {
                    drawPlayerRow(context, font, row, rect, style)
                }
                if (rect.bottom() > listArea.y && rect.y < listArea.bottom()) {
                    val swatch = UiRect(rect.x + 4, rect.y + 4, 12, 12)
                    val toggle = UiRect(swatch.right() + 4, rect.y, rect.w - 22, ROW_H)
                    playerHits += PlayerHit(row.name, row.uuid, row.entry, rect, swatch, toggle)
                }
                y += ROW_H
            }
        }
        context.disableScissor()
        drawListScrollbar(context)

        editingEntry?.let { entry ->
            context.drawText(
                font,
                UiDraw.ellipsize(font, "Farbe: ${entry.name.ifBlank { "Spieler" }}", frame.w - 24),
                frame.x + 12,
                colorPicker.field.y - 12,
                HugoTheme.text,
                false
            )
            colorPicker.render(context, entry)
            val resetHovered = resetColor.contains(lastMouseX, lastMouseY)
            UiDraw.panel(
                context,
                resetColor,
                if (resetHovered) HugoTheme.accentSoft else HugoTheme.inset,
                if (resetHovered) HugoTheme.accent else HugoTheme.cardBorder
            )
            context.drawText(font, "Standard", resetColor.x + 6, resetColor.y + 4, HugoTheme.textMuted, false)
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

        editingEntry?.let { entry ->
            if (resetColor.contains(mouseX, mouseY)) {
                entry.customColor = false
                entry.hue = ConfigManager.config.playerGlow.hue
                entry.saturation = ConfigManager.config.playerGlow.saturation
                entry.brightness = ConfigManager.config.playerGlow.brightness
                onChange()
                return true
            }
            if (colorPicker.mouseClicked(entry, mouseX, mouseY)) {
                entry.customColor = true
                return true
            }
        }

        playerHits.firstOrNull { it.row.contains(mouseX, mouseY) }?.let { hit ->
            if (hit.swatch.contains(mouseX, mouseY)) {
                ensureListed(hit.name, hit.uuid)
                editingKey = filter.find(hit.name, hit.uuid)?.let { entryKey(it) }
                onChange()
                return true
            }
            if (hit.toggle.contains(mouseX, mouseY)) {
                filter.toggle(hit.name, hit.uuid, ConfigManager.config.playerGlow)
                val stillThere = filter.find(hit.name, hit.uuid)
                if (stillThere == null && editingKey != null) {
                    editingKey = null
                } else if (stillThere != null) {
                    editingKey = entryKey(stillThere)
                }
                onChange()
                return true
            }
        }
        return true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        val entry = editingEntry ?: return false
        return colorPicker.mouseDragged(entry, mouseX, mouseY)
    }

    override fun mouseReleased() {
        colorPicker.mouseReleased()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!listArea.contains(mouseX, mouseY) || maxScroll <= 0) return false
        scroll = (scroll - (amount * 16).roundToInt()).coerceIn(0, maxScroll)
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (editingEntry != null && colorPicker.isFocused()) {
                colorPicker.unfocus()
                return true
            }
            if (editingKey != null) {
                editingKey = null
                colorPicker.unfocus()
                return true
            }
            PopupManager.close()
            return true
        }
        editingEntry?.let {
            if (colorPicker.keyPressed(it, input)) return true
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
        editingEntry?.let {
            if (colorPicker.charTyped(it, input)) return true
        }
        if (!focused || !input.isValidChar || inputText.length >= 36) return false
        inputText += input.asString()
        return true
    }

    private fun drawPlayerRow(
        context: DrawContext,
        font: net.minecraft.client.font.TextRenderer,
        row: Row,
        rect: UiRect,
        style: dev.henny.hugoutils.client.config.GlowStyle
    ) {
        val selected = row.entry != null
        val editing = selected && editingKey == entryKey(row.entry!!)
        val hovered = rect.contains(lastMouseX, lastMouseY)
        when {
            editing -> UiDraw.fill(context, rect, HugoTheme.accentSoft)
            selected -> UiDraw.fill(context, rect, 0x2218FFFFFF.toInt())
            hovered -> UiDraw.fill(context, rect, 0x14FFFFFF)
        }
        if (editing) {
            context.fill(rect.x, rect.y + 2, rect.x + 2, rect.bottom() - 2, HugoTheme.accent)
        }

        val rgb = if (row.entry != null) {
            filter.previewRgb(row.entry, style)
        } else {
            style.rgb()
        }
        val swatchColor = ColorHelper.getArgb(255, rgb[0], rgb[1], rgb[2])
        val swatch = UiRect(rect.x + 4, rect.y + 4, 12, 12)
        UiDraw.fill(context, swatch, swatchColor)
        UiDraw.border(
            context,
            swatch.x,
            swatch.y,
            swatch.w,
            swatch.h,
            if (editing) HugoTheme.accent else HugoTheme.cardBorder
        )

        val label = row.name.ifBlank { row.uuid?.toString() ?: "?" }
        val suffix = when {
            selected && row.entry?.customColor == true -> "  • eigene Farbe"
            selected -> "  • Standardfarbe"
            else -> ""
        }
        context.drawText(
            font,
            UiDraw.ellipsize(font, label + suffix, rect.w - 28),
            swatch.right() + 6,
            rect.y + 7,
            if (selected) HugoTheme.text else HugoTheme.textMuted,
            false
        )
    }

    private fun drawListScrollbar(context: DrawContext) {
        if (maxScroll <= 0) return
        val trackX = listArea.right() - 4
        val trackY = listArea.y + 2
        val trackH = listArea.h - 4
        UiDraw.fill(context, trackX, trackY, 2, trackH, HugoTheme.cardBorder)
        val thumbH = (trackH * listArea.h / (listArea.h + maxScroll).toFloat()).roundToInt().coerceIn(10, trackH)
        val thumbY = trackY + ((trackH - thumbH) * (scroll / maxScroll.toFloat())).roundToInt()
        UiDraw.fill(context, trackX, thumbY, 2, thumbH, HugoTheme.accentMuted)
    }

    private fun buildRows(): List<Row> {
        val online = client.networkHandler?.playerList.orEmpty()
            .map { it.profile.name to it.profile.id }
            .sortedBy { it.first.lowercase() }
        val combined = LinkedHashMap<String, Row>()
        online.forEach { (name, uuid) ->
            combined[uuid.toString()] = Row(name, uuid, filter.find(name, uuid))
        }
        filter.players.forEach { entry ->
            val uuid = PlayerFilter.parseUuid(entry.uuid)
            val key = uuid?.toString() ?: entry.name.lowercase()
            combined.putIfAbsent(key, Row(entry.name, uuid, entry))
            combined.computeIfPresent(key) { _, row -> row.copy(entry = entry) }
        }
        return combined.values.toList()
    }

    private fun recomputeScroll(rowCount: Int = buildRows().size) {
        val contentH = if (rowCount == 0) ROW_H else rowCount * ROW_H
        maxScroll = (contentH - listArea.h + 4).coerceAtLeast(0)
        scroll = scroll.coerceIn(0, maxScroll)
    }

    private fun ensureListed(name: String, uuid: UUID?) {
        if (filter.find(name, uuid) == null) {
            filter.toggle(name, uuid, ConfigManager.config.playerGlow)
        }
    }

    private fun addManual() {
        val value = inputText.trim()
        if (value.isBlank()) return
        val uuid = PlayerFilter.parseUuid(value)
        val name = if (uuid == null) value else ""
        if (filter.find(name, uuid) == null) {
            filter.toggle(name, uuid, ConfigManager.config.playerGlow)
        }
        filter.find(name, uuid)?.let {
            editingKey = entryKey(it)
            if (!it.customColor) {
                it.hue = ConfigManager.config.playerGlow.hue
                it.saturation = ConfigManager.config.playerGlow.saturation
                it.brightness = ConfigManager.config.playerGlow.brightness
            }
        }
        inputText = ""
        onChange()
    }

    private fun entryKey(entry: PlayerFilter.Entry): String =
        PlayerFilter.parseUuid(entry.uuid)?.toString() ?: entry.name.lowercase()

    private fun colorPickerReserve(): Int = 118

    private data class Row(val name: String, val uuid: UUID?, val entry: PlayerFilter.Entry?)
    private data class PlayerHit(
        val name: String,
        val uuid: UUID?,
        val entry: PlayerFilter.Entry?,
        val row: UiRect,
        val swatch: UiRect,
        val toggle: UiRect
    )

    companion object {
        private const val ROW_H = 22
    }
}
