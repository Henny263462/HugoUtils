package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

class SettingToggle(
    private val key: String,
    var title: String,
    var description: String,
    private val read: () -> Boolean,
    private val write: (Boolean) -> Unit
) : SettingBlock {
    override var bounds: UiRect = UiRect(0, 0, 0, 0)
        private set
    private var toggle = UiRect(0, 0, 0, 0)
    private val anim = AnimatedFloat(if (read()) 1f else 0f, .14f)

    override fun layout(x: Int, y: Int, width: Int): Int {
        bounds = UiRect(x, y, width, HEIGHT)
        toggle = UiRect(bounds.right - 42, bounds.y + 14, 32, 14)
        return HEIGHT
    }

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        anim.animateTo(if (read()) 1f else 0f)
        anim.update(UiFrame.deltaSeconds)
        UiWidgets.hoverCard(context, bounds, mouseX.toDouble(), mouseY.toDouble(), key = key)
        Labels.title(context, renderer, bounds.x + 12, bounds.y + 12, title)
        UiWidgets.toggleProgress(context, toggle, anim.value, mouseX.toDouble(), mouseY.toDouble(), key = "$key-toggle")
        Labels.muted(context, renderer, bounds.x + 12, bounds.y + 42, description, bounds.w - 24)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (!bounds.contains(mouseX, mouseY)) return false
        if (toggle.contains(mouseX, mouseY) || mouseY <= bounds.y + 34) {
            write(!read())
        }
        return true
    }

    companion object {
        const val HEIGHT = 72
    }
}
