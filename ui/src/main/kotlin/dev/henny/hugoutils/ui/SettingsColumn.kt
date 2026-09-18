package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput

interface SettingBlock {
    val bounds: UiRect
    fun layout(x: Int, y: Int, width: Int): Int
    fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int)
    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    fun mouseReleased() {}
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false
    fun keyPressed(input: KeyInput): Boolean = false
    fun charTyped(input: CharInput): Boolean = false
}

class SettingsColumn(private val gap: Int = 10) {
    private val blocks = mutableListOf<SettingBlock>()
    var bounds: UiRect = UiRect(0, 0, 0, 0)
        private set

    fun clear() {
        blocks.clear()
    }

    fun add(block: SettingBlock): SettingsColumn {
        blocks += block
        return this
    }

    fun layout(x: Int, y: Int, width: Int): Int {
        var cursor = y
        for (block in blocks) {
            cursor += block.layout(x, cursor, width) + gap
        }
        val height = (cursor - gap - y).coerceAtLeast(0)
        bounds = UiRect(x, y, width, height)
        return height
    }

    fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        blocks.forEach { it.render(context, renderer, mouseX, mouseY) }
    }

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean =
        blocks.any { it.mouseClicked(mouseX, mouseY) }

    fun mouseDragged(mouseX: Double, mouseY: Double): Boolean =
        blocks.any { it.mouseDragged(mouseX, mouseY) }

    fun mouseReleased() {
        blocks.forEach { it.mouseReleased() }
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean =
        blocks.any { it.mouseScrolled(mouseX, mouseY, amount) }

    fun keyPressed(input: KeyInput): Boolean = blocks.any { it.keyPressed(input) }
    fun charTyped(input: CharInput): Boolean = blocks.any { it.charTyped(input) }
}
