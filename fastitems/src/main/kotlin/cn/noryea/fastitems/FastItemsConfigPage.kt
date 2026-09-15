package cn.noryea.fastitems

import cn.noryea.fastitems.config.FastItemsConfig
import dev.henny.hugoutils.client.ui.ConfigCategory
import dev.henny.hugoutils.client.ui.ConfigPage
import dev.henny.hugoutils.client.ui.HugoTheme
import dev.henny.hugoutils.client.ui.ItemFilterPanel
import dev.henny.hugoutils.client.ui.ItemFilterPopup
import dev.henny.hugoutils.client.ui.PopupManager
import dev.henny.hugoutils.ui.UiDraw
import dev.henny.hugoutils.ui.UiRect
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack
import kotlin.math.roundToInt

class FastItemsConfigPage : ConfigPage {
    override val category: ConfigCategory = ConfigCategory.FAST_ITEMS

    private var expanded = false
    private var enableAnim = if (FastItemsConfig.isActive()) 1f else 0f
    private var header = UiRect(0, 0, 0, 0)
    private var headerToggle = UiRect(0, 0, 0, 0)
    private var headerHelper = UiRect(0, 0, 0, 0)
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var tooltip: String? = null
    private var filterButton = UiRect(0, 0, 0, 0)

    private data class Toggle(
        val label: String,
        val description: String,
        var value: Boolean,
        var anim: Float = if (value) 1f else 0f,
        var rect: UiRect = UiRect(0, 0, 0, 0),
        var switch: UiRect = UiRect(0, 0, 0, 0),
        val getter: () -> Boolean,
        val onChange: (Boolean) -> Unit
    )

    private val toggles = listOf(
        Toggle(
            "Schatten",
            "Items werfen weiterhin einen Schatten auf den Boden.",
            FastItemsConfig.castShadows,
            getter = { FastItemsConfig.castShadows }
        ) { FastItemsConfig.castShadows = it },
        Toggle(
            "Seiten",
            "Auch die seitlichen Flächen von Block-Items werden gezeichnet (langsamer, aber voluminöser).",
            FastItemsConfig.renderSidesOfItems,
            getter = { FastItemsConfig.renderSidesOfItems }
        ) { FastItemsConfig.renderSidesOfItems = it },
        Toggle(
            "3D-Modelle",
            "Aufwendige 3D-Item-Modelle (z.B. Werkzeuge) werden ebenfalls vereinfacht.",
            FastItemsConfig.affect3DModels,
            getter = { FastItemsConfig.affect3DModels }
        ) { FastItemsConfig.affect3DModels = it }
    )

    private val itemFilter by lazy {
        val font = MinecraftClient.getInstance().textRenderer
        ItemFilterPanel(font, FastItemsConfig.filter) { persist() }
    }

    override fun resetUi() {
        expanded = false
        itemFilter.unfocus()
    }

    override fun hoveredStack(): ItemStack? = null

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        header = UiRect(x, y, width, HEADER_H)
        headerToggle = UiRect(x + width - 42, y + 8, 32, 14)
        headerHelper = UiRect(x + width - 58, y + 9, 12, 12)
        if (!expanded) {
            for (toggle in toggles) {
                toggle.rect = UiRect(0, 0, 0, 0)
                toggle.switch = UiRect(0, 0, 0, 0)
            }
            filterButton = UiRect(0, 0, 0, 0)
            return HEADER_H
        }
        var currentY = y + HEADER_H + 4
        for (toggle in toggles) {
            toggle.value = toggle.getter()
            toggle.rect = UiRect(x + 10, currentY, width - 20, ROW_H)
            toggle.switch = UiRect(toggle.rect.right() - 32, currentY + 4, 32, 14)
            currentY += ROW_H + 4
        }
        filterButton = UiRect(x + 10, currentY + 4, width - 20, 22)
        return HEADER_H + 4 + toggles.size * (ROW_H + 4) + 4 + 22 + 8
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        val font = MinecraftClient.getInstance().textRenderer
        tooltip = null
        enableAnim = UiDraw.lerp(enableAnim, if (FastItemsConfig.isActive()) 1f else 0f, 0.25f)

        UiDraw.panel(context, header.x, header.y, header.w, layoutHeight(), HugoTheme.card, HugoTheme.cardBorder)
        val chevron = if (expanded) "▾" else "▸"
        context.drawText(font, chevron, header.x + 10, header.y + 10, HugoTheme.textMuted, false)
        context.drawText(font, "Fast Items", header.x + 22, header.y + 9, HugoTheme.text, false)
        drawToggle(context, headerToggle, enableAnim)
        val helperHovered = headerHelper.contains(lastMouseX, lastMouseY)
        UiDraw.helperBadge(context, font, headerHelper, helperHovered)
        if (helperHovered) {
            tooltip = HELPER
        }

        if (expanded) {
            for (toggle in toggles) {
                toggle.anim = UiDraw.lerp(toggle.anim, if (toggle.value) 1f else 0f, 0.25f)
                context.drawText(font, toggle.label, toggle.rect.x, toggle.rect.y + 5, HugoTheme.text, false)
                drawToggle(context, toggle.switch, toggle.anim)
            }
            val hovered = filterButton.contains(mouseX.toDouble(), mouseY.toDouble())
            UiDraw.fill(context, filterButton, if (hovered) HugoTheme.accentSoft else HugoTheme.inset)
            UiDraw.border(context, filterButton.x, filterButton.y, filterButton.w, filterButton.h,
                if (hovered) HugoTheme.accent else HugoTheme.cardBorder)
            context.drawText(font, "Items / Blöcke auswählen…", filterButton.x + 7, filterButton.y + 7,
                if (hovered) HugoTheme.text else HugoTheme.textMuted, false)
        }

        tooltip?.let { text ->
            UiDraw.tooltip(context, font, text, mouseX, mouseY, header.right(), header.bottom() + 200)
        }
    }

    private fun layoutHeight(): Int = header.h.coerceAtLeast(
        if (expanded) {
            val last = toggles.lastOrNull()?.rect?.bottom() ?: header.bottom()
            val filterBottom = filterButton.bottom().coerceAtLeast(last)
            filterBottom + 8 - header.y
        } else {
            HEADER_H
        }
    )

    private fun drawToggle(context: DrawContext, rect: UiRect, anim: Float) {
        val hovered = rect.contains(lastMouseX, lastMouseY)
        val on = HugoTheme.lerpColor(HugoTheme.trackOff, HugoTheme.success, anim)
        UiDraw.fill(context, rect, on)
        UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, if (hovered) HugoTheme.accent else HugoTheme.cardBorder)
        val knobW = 12
        val knobX = rect.x + 2 + ((rect.w - knobW - 4) * anim).roundToInt()
        UiDraw.fill(context, knobX, rect.y + 2, knobW, rect.h - 4, HugoTheme.knob)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (headerToggle.contains(mouseX, mouseY)) {
            FastItemsConfig.enable = !FastItemsConfig.enable
            persist()
            return true
        }
        if (titleHit(mouseX, mouseY)) {
            expanded = !expanded
            if (!expanded) itemFilter.unfocus()
            return true
        }
        if (!expanded) {
            return false
        }
        for (toggle in toggles) {
            if (toggle.rect.contains(mouseX, mouseY) || toggle.switch.contains(mouseX, mouseY)) {
                toggle.value = !toggle.value
                toggle.onChange(toggle.value)
                persist()
                return true
            }
        }
        if (filterButton.contains(mouseX, mouseY)) {
            PopupManager.open(ItemFilterPopup("Fast Items", FastItemsConfig.filter) { persist() })
            return true
        }
        return false
    }

    private fun titleHit(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= header.x && mouseX < headerHelper.x - 2 &&
            mouseY >= header.y && mouseY < header.y + HEADER_H &&
            !headerToggle.contains(mouseX, mouseY)
    }

    override fun persist() {
        FastItemsConfig.save()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false
    override fun keyPressed(input: KeyInput): Boolean = false
    override fun charTyped(input: CharInput): Boolean = false
    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean = false
    override fun mouseReleased() {}

    companion object {
        private const val HEADER_H = 30
        private const val ROW_H = 22
        private const val HELPER =
            "Dropped Items werden als 2D-Sprite gerendert.\n" +
                "- Schatten, Seiten und 3D-Modelle sind optional.\n" +
                "- Items: Welche gedroppten Items Fast Items nutzen."
    }
}
