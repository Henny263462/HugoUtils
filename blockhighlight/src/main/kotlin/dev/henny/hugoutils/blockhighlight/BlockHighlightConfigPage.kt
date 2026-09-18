package dev.henny.hugoutils.blockhighlight

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.ui.ConfigCategory
import dev.henny.hugoutils.client.ui.ConfigPage
import dev.henny.hugoutils.client.ui.HugoTheme
import dev.henny.hugoutils.ui.ButtonStyle
import dev.henny.hugoutils.ui.ColorPicker
import dev.henny.hugoutils.ui.SliderMath
import dev.henny.hugoutils.ui.UiDraw
import dev.henny.hugoutils.ui.UiRect
import dev.henny.hugoutils.ui.UiWidgets
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack
import kotlin.math.roundToInt

class BlockHighlightConfigPage : ConfigPage {
    override val category = ConfigCategory.BLOCKS
    private val client = MinecraftClient.getInstance()
    private val picker by lazy { ColorPicker(client.textRenderer) { save() } }
    private val blocks by lazy { HighlightBlockPicker(client.textRenderer) { save() } }
    private var frame = UiRect(0, 0, 0, 0)
    private var toggle = UiRect(0, 0, 0, 0)
    private var editor = UiRect(0, 0, 0, 0)
    private var removeButton = UiRect(0, 0, 0, 0)
    private var opacity = UiRect(0, 0, 0, 0)
    private var selected: String? = null
    private var status = "Nur der Block unter dem Fadenkreuz"
    private var draggingOpacity = false
    private var mouseX = 0.0
    private var mouseY = 0.0

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        val h = height.coerceAtLeast(360)
        frame = UiRect(x, y, width, h)
        toggle = UiRect(frame.right - 42, y + 10, 32, 14)
        picker.layout(x + 10, y, 100, 72)
        val editorH = picker.height() + 10
        editor = UiRect(x + 8, y + h - editorH - 18, width - 16, editorH)
        picker.layout(editor.x + 8, editor.y + 8, 100, 72)
        val controlsX = picker.hueBar.right + 12
        val controlsW = (frame.right - 18 - controlsX).coerceAtLeast(80)
        opacity = UiRect(controlsX, editor.y + 10, controlsW, 20)
        removeButton = UiRect(controlsX, opacity.bottom + 10, controlsW, 22)
        val pickerTop = editor.y - 8
        blocks.layout(x + 10, y + 40, width - 20, (pickerTop - (y + 40)).coerceAtLeast(90))
        val current = selected
        if (current == null || current !in BlockHighlightConfig.entries) {
            selected = BlockHighlightConfig.keys().firstOrNull()
        }
        blocks.selectedId = selected
        return frame.h
    }

    override fun hoveredStack(): ItemStack? = blocks.hoveredStack

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        this.mouseX = mouseX.toDouble()
        this.mouseY = mouseY.toDouble()
        val font = client.textRenderer
        UiWidgets.card(context, frame)
        UiWidgets.heading(
            context,
            font,
            frame.x + 10,
            frame.y + 8,
            "Block-Highlight",
            "${BlockHighlightConfig.keys().size} Blöcke · nur unter dem Fadenkreuz"
        )
        UiWidgets.toggle(context, toggle, BlockHighlightConfig.enabled, this.mouseX, this.mouseY, key = "highlight-enabled")
        blocks.selectedId = selected
        blocks.render(context, mouseX, mouseY)
        UiDraw.panel(context, editor, HugoTheme.inset, HugoTheme.cardBorder)
        currentStyle()?.let { style ->
            picker.render(context, style)
            UiWidgets.labeledSlider(
                context, font, opacity, "Deckkraft", "${(style.opacity * 100).roundToInt()}%",
                style.opacity, this.mouseX, this.mouseY, key = "highlight-opacity", immediate = draggingOpacity
            )
            UiWidgets.button(context, font, removeButton, "Entfernen", this.mouseX, this.mouseY, style = ButtonStyle.DANGER, key = "highlight-remove")
        } ?: context.drawText(font, "Block im Raster wählen", editor.x + 12, editor.y + 16, HugoTheme.textDim, false)
        context.drawText(font, UiDraw.ellipsize(font, status, frame.w - 20), frame.x + 10, frame.bottom - 12, HugoTheme.textDim, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (toggle.contains(mouseX, mouseY)) {
            BlockHighlightConfig.enabled = !BlockHighlightConfig.enabled
            save()
            return true
        }
        val style = currentStyle()
        if (style != null && picker.mouseClicked(style, mouseX, mouseY)) {
            blocks.unfocus()
            return true
        }
        if (style != null && SliderMath.contains(opacity, mouseX, mouseY)) {
            draggingOpacity = true
            applyOpacity(mouseX)
            return true
        }
        if (style != null && removeButton.contains(mouseX, mouseY)) {
            BlockHighlightConfig.remove(selected)
            selected = BlockHighlightConfig.keys().firstOrNull()
            save()
            return true
        }
        if (blocks.mouseClicked(mouseX, mouseY)) {
            picker.unfocus()
            blocks.clickedId?.let { selected = it }
            status = "Highlight gilt nur unter dem Fadenkreuz"
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        val style = currentStyle()
        if (style != null && picker.mouseDragged(style, mouseX, mouseY)) return true
        if (draggingOpacity) {
            applyOpacity(mouseX)
            return true
        }
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean =
        blocks.mouseScrolled(mouseX, mouseY, amount)

    override fun mouseReleased() {
        picker.mouseReleased()
        draggingOpacity = false
    }

    override fun keyPressed(input: KeyInput): Boolean {
        val style = currentStyle()
        if (style != null && picker.keyPressed(style, input)) return true
        return blocks.keyPressed(input)
    }

    override fun charTyped(input: CharInput): Boolean {
        val style = currentStyle()
        if (style != null && picker.charTyped(style, input)) return true
        return blocks.charTyped(input)
    }

    override fun resetUi() {
        blocks.unfocus()
        picker.unfocus()
    }

    override fun persist() = save()

    private fun currentStyle(): BlockHighlightStyle? = selected?.let(BlockHighlightConfig.entries::get)

    private fun applyOpacity(x: Double) {
        currentStyle()?.opacity = SliderMath.normalized(opacity, x)
        save()
    }

    private fun save() = ConfigManager.requestSave()
}
