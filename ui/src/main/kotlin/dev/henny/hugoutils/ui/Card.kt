package dev.henny.hugoutils.ui

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

open class Card(bounds: UiRect = UiRect(0, 0, 0, 0), var title: String = "") : Control(bounds) {
    val children = mutableListOf<Control>()

    override fun render(context: DrawContext, renderer: TextRenderer, mouseX: Int, mouseY: Int) {
        if (!visible) return
        val hovered = enabled && bounds.contains(mouseX.toDouble(), mouseY.toDouble())
        interaction.update(hovered)
        UiDraw.shadow(context, bounds, .2f + interaction.hover.value * .18f)
        UiDraw.panel(
            context,
            bounds,
            Theme.lerpColor(UiDraw.theme.card, UiDraw.theme.cardHover, interaction.hover.value * .55f),
            Theme.lerpColor(UiDraw.theme.border, UiDraw.theme.accentMuted, interaction.hover.value * .35f)
        )
        if (title.isNotEmpty()) {
            context.drawText(renderer, title, bounds.x + UiMetrics.CARD_PADDING, bounds.y + UiMetrics.CARD_PADDING, UiDraw.theme.text, false)
        }
        children.filter { it.visible }.forEach { it.render(context, renderer, mouseX, mouseY) }
    }
}
