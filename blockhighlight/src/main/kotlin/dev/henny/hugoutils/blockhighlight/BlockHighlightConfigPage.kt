package dev.henny.hugoutils.blockhighlight

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.ui.ConfigCategory
import dev.henny.hugoutils.client.ui.ConfigPage
import dev.henny.hugoutils.client.ui.HugoTheme
import dev.henny.hugoutils.ui.ColorPicker
import dev.henny.hugoutils.ui.UiDraw
import dev.henny.hugoutils.ui.UiRect
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class BlockHighlightConfigPage : ConfigPage {
    override val category = ConfigCategory.BLOCKS
    private val client = MinecraftClient.getInstance()
    private val picker by lazy { ColorPicker(client.textRenderer) { save() } }
    private var frame = UiRect(0, 0, 0, 0)
    private var toggle = UiRect(0, 0, 0, 0)
    private var input = UiRect(0, 0, 0, 0)
    private var addButton = UiRect(0, 0, 0, 0)
    private var removeButton = UiRect(0, 0, 0, 0)
    private var opacity = UiRect(0, 0, 0, 0)
    private var listArea = UiRect(0, 0, 0, 0)
    private var entryHits = emptyList<Pair<String, UiRect>>()
    private var firstEntry = 0
    private var selected: String? = null
    private var inputText = ""
    private var inputFocused = false
    private var status = ""
    private var draggingOpacity = false
    private var mouseX = 0.0
    private var mouseY = 0.0

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, 246)
        toggle = UiRect(frame.right() - 42, y + 9, 32, 14)
        input = UiRect(x + 10, y + 40, width - 102, 22)
        addButton = UiRect(input.right() + 6, y + 40, 76, 22)
        listArea = UiRect(x + 10, y + 70, width / 2 - 15, 152)
        val allNames = BlockHighlightConfig.entries.keys.toList()
        firstEntry = firstEntry.coerceIn(0, (allNames.size - 7).coerceAtLeast(0))
        val names = allNames.drop(firstEntry).take(7)
        entryHits = names.mapIndexed { index, name ->
            name to UiRect(listArea.x, listArea.y + index * 22, listArea.w, 20)
        }
        picker.layout(x + width / 2 + 5, y + 70, 82, 56)
        opacity = UiRect(x + width / 2 + 5, y + 170, width / 2 - 15, 20)
        removeButton = UiRect(x + width / 2 + 5, y + 200, width / 2 - 15, 22)
        if (selected !in BlockHighlightConfig.entries) selected = BlockHighlightConfig.entries.keys.firstOrNull()
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        this.mouseX = mouseX.toDouble()
        this.mouseY = mouseY.toDouble()
        val font = client.textRenderer
        UiDraw.panel(context, frame, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, "Block Highlight", frame.x + 10, frame.y + 12, HugoTheme.text, false)
        context.drawText(font, "Nur der anvisierte Block wird eingefärbt.", frame.x + 112, frame.y + 12, HugoTheme.textDim, false)
        drawToggle(context)
        drawInput(context)
        drawButton(context, addButton, "Hinzufügen", true)

        if (entryHits.isEmpty()) {
            context.drawText(font, "Noch keine Block-IDs", frame.x + 12, frame.y + 78, HugoTheme.textDim, false)
        }
        entryHits.forEach { (id, rect) ->
            val active = id == selected
            val hovered = rect.contains(this.mouseX, this.mouseY)
            UiDraw.fill(context, rect, if (active) HugoTheme.accentSoft else if (hovered) 0x18FFFFFF else HugoTheme.inset)
            UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, if (active) HugoTheme.accent else HugoTheme.cardBorder)
            context.drawText(font, UiDraw.ellipsize(font, id, rect.w - 12), rect.x + 6, rect.y + 6, HugoTheme.text, false)
        }

        currentStyle()?.let { style ->
            picker.render(context, style)
            context.drawText(font, "Deckkraft", opacity.x, opacity.y, HugoTheme.textMuted, false)
            val value = "${(style.opacity * 100).roundToInt()}%"
            context.drawText(font, value, opacity.right() - font.getWidth(value), opacity.y, HugoTheme.text, false)
            val trackY = opacity.y + 12
            UiDraw.fill(context, opacity.x, trackY, opacity.w, 6, HugoTheme.inset)
            UiDraw.fill(context, opacity.x + 1, trackY + 1, ((opacity.w - 2) * style.opacity).roundToInt(), 4, HugoTheme.accent)
            drawButton(context, removeButton, "Eintrag entfernen", true)
        }
        if (status.isNotBlank()) {
            context.drawText(font, UiDraw.ellipsize(font, status, frame.w - 20), frame.x + 10, frame.bottom() - 14, HugoTheme.textDim, false)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (toggle.contains(mouseX, mouseY)) {
            BlockHighlightConfig.enabled = !BlockHighlightConfig.enabled
            save()
            return true
        }
        if (input.contains(mouseX, mouseY)) {
            inputFocused = true
            picker.unfocus()
            return true
        }
        inputFocused = false
        if (addButton.contains(mouseX, mouseY)) {
            val added = BlockHighlightConfig.add(inputText)
            status = if (added == null) "Ungültige oder unbekannte Block-ID" else "Block hinzugefügt"
            if (added != null) {
                selected = added
                inputText = ""
                save()
            }
            return true
        }
        entryHits.firstOrNull { it.second.contains(mouseX, mouseY) }?.let {
            selected = it.first
            picker.unfocus()
            return true
        }
        val style = currentStyle()
        if (style != null && picker.mouseClicked(style, mouseX, mouseY)) return true
        if (style != null && opacity.contains(mouseX, mouseY)) {
            draggingOpacity = true
            applyOpacity(mouseX)
            return true
        }
        if (style != null && removeButton.contains(mouseX, mouseY)) {
            BlockHighlightConfig.entries.remove(selected)
            selected = BlockHighlightConfig.entries.keys.firstOrNull()
            save()
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        val style = currentStyle() ?: return false
        if (picker.mouseDragged(style, mouseX, mouseY)) return true
        if (draggingOpacity) {
            applyOpacity(mouseX)
            return true
        }
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!listArea.contains(mouseX, mouseY)) return false
        val max = (BlockHighlightConfig.entries.size - 7).coerceAtLeast(0)
        firstEntry = (firstEntry - amount.toInt()).coerceIn(0, max)
        return true
    }

    override fun mouseReleased() {
        picker.mouseReleased()
        draggingOpacity = false
    }

    override fun keyPressed(input: KeyInput): Boolean {
        val style = currentStyle()
        if (style != null && picker.keyPressed(style, input)) return true
        if (!inputFocused) return false
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (inputText.isNotEmpty()) inputText = inputText.dropLast(1)
                true
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                val added = BlockHighlightConfig.add(inputText)
                if (added != null) {
                    selected = added
                    inputText = ""
                    save()
                }
                true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                inputFocused = false
                true
            }
            else -> false
        }
    }

    override fun charTyped(input: CharInput): Boolean {
        val style = currentStyle()
        if (style != null && picker.charTyped(style, input)) return true
        if (!inputFocused || !input.isValidChar || inputText.length >= 80) return false
        inputText += input.asString()
        return true
    }

    override fun resetUi() {
        inputFocused = false
        picker.unfocus()
    }

    override fun persist() = save()

    private fun currentStyle(): BlockHighlightStyle? = selected?.let(BlockHighlightConfig.entries::get)

    private fun applyOpacity(x: Double) {
        currentStyle()?.opacity = ((x - opacity.x) / opacity.w.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        save()
    }

    private fun save() = ConfigManager.requestSave()

    private fun drawInput(context: DrawContext) {
        UiDraw.panel(context, input, HugoTheme.inset, if (inputFocused) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = inputText.ifBlank { "minecraft:stone" }
        context.drawText(client.textRenderer, UiDraw.ellipsize(client.textRenderer, shown, input.w - 10), input.x + 5, input.y + 7,
            if (inputText.isBlank()) HugoTheme.textDim else HugoTheme.text, false)
    }

    private fun drawToggle(context: DrawContext) {
        val enabled = BlockHighlightConfig.enabled
        UiDraw.fill(context, toggle, if (enabled) HugoTheme.success else HugoTheme.trackOff)
        UiDraw.border(context, toggle.x, toggle.y, toggle.w, toggle.h, HugoTheme.cardBorder)
        UiDraw.fill(context, if (enabled) toggle.right() - 14 else toggle.x + 2, toggle.y + 2, 12, toggle.h - 4, HugoTheme.knob)
    }

    private fun drawButton(context: DrawContext, rect: UiRect, text: String, enabled: Boolean) {
        val hovered = enabled && rect.contains(mouseX, mouseY)
        UiDraw.fill(context, rect, if (hovered) HugoTheme.accentSoft else HugoTheme.inset)
        UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, if (hovered) HugoTheme.accent else HugoTheme.cardBorder)
        val shown = UiDraw.ellipsize(client.textRenderer, text, rect.w - 8)
        context.drawText(client.textRenderer, shown, rect.x + (rect.w - client.textRenderer.getWidth(shown)) / 2, rect.y + 7,
            if (enabled) HugoTheme.text else HugoTheme.textDim, false)
    }
}
