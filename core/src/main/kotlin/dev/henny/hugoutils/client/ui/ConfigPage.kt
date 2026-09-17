package dev.henny.hugoutils.client.ui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack

interface ConfigPage : dev.henny.hugoutils.ui.UiPage {
    val category: ConfigCategory
    override val id: String get() = this::class.qualifiedName ?: this::class.simpleName ?: category.id

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int)

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean = false
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        if (button == 0) mouseClicked(mouseX, mouseY) else false
    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    override fun mouseReleased() {}
    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false
    override fun keyPressed(input: KeyInput): Boolean = false
    override fun charTyped(input: CharInput): Boolean = false

    override fun resetUi() {}

    override fun hoveredStack(): ItemStack? = null
    override fun hoveredTooltip(): String? = null

    override fun persist()

    fun onShown() {}
}
