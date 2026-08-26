package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ItemFilter
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack
import org.lwjgl.glfw.GLFW

class ItemFilterPopup(
    private val title: String,
    filter: ItemFilter,
    onChange: () -> Unit
) : ConfigPopup {
    private val client = MinecraftClient.getInstance()
    private val panel = ItemFilterPanel(client.textRenderer, filter, onChange)
    private var frame = UiRect(0, 0, 0, 0)
    private var close = UiRect(0, 0, 0, 0)

    override fun layout(screenWidth: Int, screenHeight: Int) {
        val width = (screenWidth - 32).coerceIn(300, 470)
        val panelHeight = panel.layout(0, 0, width - 24)
        val height = panelHeight + 48
        frame = UiRect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height)
        panel.layout(frame.x + 12, frame.y + 34, frame.w - 24)
        close = UiRect(frame.right() - 24, frame.y + 8, 16, 16)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        UiDraw.fill(context, 0, 0, client.window.scaledWidth, client.window.scaledHeight, 0xB0000000.toInt())
        UiDraw.panel(context, frame, HugoTheme.panel, HugoTheme.panelBorder)
        context.drawText(client.textRenderer, title, frame.x + 12, frame.y + 13, HugoTheme.text, false)
        context.drawText(client.textRenderer, "×", close.x + 4, close.y + 3, HugoTheme.textMuted, false)
        panel.render(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (close.contains(mouseX, mouseY) || !frame.contains(mouseX, mouseY)) {
            PopupManager.close()
            return true
        }
        return panel.mouseClicked(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean =
        panel.mouseScrolled(mouseX, mouseY, amount)

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            PopupManager.close()
            return true
        }
        return panel.keyPressed(input)
    }

    override fun charTyped(input: CharInput): Boolean = panel.charTyped(input)
    override fun hoveredStack(): ItemStack? = panel.hoveredStack
}
