package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.ui.UiMetrics
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import kotlin.math.roundToInt

class GeneralConfigPage : ConfigPage {
    override val category = ConfigCategory.GENERAL
    private val client = MinecraftClient.getInstance()
    private var frame = UiRect(0, 0, 0, 0)
    private var restoreCard = UiRect(0, 0, 0, 0)
    private var restoreToggle = UiRect(0, 0, 0, 0)
    private var cornerCard = UiRect(0, 0, 0, 0)
    private var cornerSlider = UiRect(0, 0, 0, 0)
    private var previewSmall = UiRect(0, 0, 0, 0)
    private var previewMid = UiRect(0, 0, 0, 0)
    private var previewLarge = UiRect(0, 0, 0, 0)
    private var restoreAnim = if (ConfigManager.config.restoreLastPage) 1f else 0f
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var dragging = false

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        frame = UiRect(x, y, width, 248)
        restoreCard = UiRect(x + 8, y + 8, width - 16, 72)
        restoreToggle = UiRect(restoreCard.right() - 42, restoreCard.y + 14, 32, 14)
        cornerCard = UiRect(x + 8, restoreCard.bottom() + 10, width - 16, 150)
        val previewW = ((cornerCard.w - 36) / 3).coerceAtLeast(70)
        previewSmall = UiRect(cornerCard.x + 10, cornerCard.y + 42, previewW, 36)
        previewMid = UiRect(previewSmall.right() + 8, previewSmall.y, previewW, 36)
        previewLarge = UiRect(previewMid.right() + 8, previewSmall.y, previewW, 36)
        cornerSlider = UiRect(cornerCard.x + 10, previewSmall.bottom() + 18, cornerCard.w - 20, 20)
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        val font = client.textRenderer
        restoreAnim = UiDraw.lerp(restoreAnim, if (ConfigManager.config.restoreLastPage) 1f else 0f, 0.25f)
        val radius = ConfigManager.config.uiCornerRadius

        UiWidgets.hoverCard(context, restoreCard, lastMouseX, lastMouseY, key = "general-restore")
        context.drawText(font, "Letzte Seite", restoreCard.x + 12, restoreCard.y + 12, HugoTheme.text, false)
        UiWidgets.toggleProgress(context, restoreToggle, restoreAnim, lastMouseX, lastMouseY, key = "general-restore-toggle")
        context.drawText(
            font,
            UiDraw.ellipsize(font, "Beim Öffnen genau dort weitermachen, wo du aufgehört hast.", restoreCard.w - 24),
            restoreCard.x + 12,
            restoreCard.y + 42,
            HugoTheme.textMuted,
            false
        )

        UiWidgets.hoverCard(context, cornerCard, lastMouseX, lastMouseY, key = "general-corner")
        context.drawText(font, "Ecken", cornerCard.x + 12, cornerCard.y + 12, HugoTheme.text, false)
        context.drawText(
            font,
            if (radius <= 0) "Eckig" else "$radius px rund",
            cornerCard.right() - font.getWidth(if (radius <= 0) "Eckig" else "$radius px rund") - 12,
            cornerCard.y + 12,
            HugoTheme.accent,
            false
        )
        drawPreview(context, previewSmall, UiMetrics.CORNER_SM, "Klein")
        drawPreview(context, previewMid, UiMetrics.CORNER, "Karten")
        drawPreview(context, previewLarge, UiMetrics.CORNER_LG, "Groß")
        UiWidgets.labeledSlider(
            context,
            font,
            cornerSlider,
            "Rundung",
            if (radius <= 0) "0" else "$radius",
            radius / 12f,
            lastMouseX,
            lastMouseY,
            key = "general-corner-slider"
        )
        context.drawText(font, "Eckig", cornerSlider.x, cornerSlider.bottom() + 6, HugoTheme.textDim, false)
        val roundLabel = "Rund"
        context.drawText(
            font,
            roundLabel,
            cornerSlider.right() - font.getWidth(roundLabel),
            cornerSlider.bottom() + 6,
            HugoTheme.textDim,
            false
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (restoreToggle.contains(mouseX, mouseY) || restoreCard.contains(mouseX, mouseY) && mouseY <= restoreCard.y + 34) {
            ConfigManager.update { it.restoreLastPage = !it.restoreLastPage }
            return true
        }
        if (cornerSlider.contains(mouseX, mouseY)) {
            dragging = true
            applyCorner(mouseX)
            return true
        }
        return frame.contains(mouseX, mouseY)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        if (!dragging) return false
        applyCorner(mouseX)
        return true
    }

    override fun mouseReleased() {
        dragging = false
    }

    override fun persist() = ConfigManager.requestSave()

    private fun applyCorner(mouseX: Double) {
        val normalized = ((mouseX - cornerSlider.x) / cornerSlider.w.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        ConfigManager.setUiCornerRadius((normalized * 12f).roundToInt())
    }

    private fun drawPreview(context: DrawContext, rect: UiRect, radius: Int, label: String) {
        val font = client.textRenderer
        UiDraw.panel(context, rect, HugoTheme.inset, HugoTheme.accent, radius)
        context.drawText(
            font,
            label,
            rect.x + (rect.w - font.getWidth(label)) / 2,
            rect.y + (rect.h - 8) / 2,
            HugoTheme.text,
            false
        )
    }
}
