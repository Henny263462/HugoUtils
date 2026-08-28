package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.HugoIds
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.GlintStyle
import dev.henny.hugoutils.client.config.GlintChangeListener
import dev.henny.hugoutils.client.config.GlowStyle
import dev.henny.hugoutils.update.UpdateManager
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Mouse
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class HugoScreen : Screen(Text.literal(HugoIds.DISPLAY_NAME)) {
    private var category = ConfigCategory.VISUALS
    private var pageAnim = 1f
    private var scroll = 0
    private var maxScroll = 0
    private var draggingSlider: SliderId? = null
    private var toggleAnimDropped = 0f
    private var toggleAnimHeldGlow = 0f
    private var toggleAnimPlayerGlow = 0f
    private var toggleAnimGlint = 0f

    private lateinit var droppedPicker: ColorPicker
    private lateinit var heldGlowPicker: ColorPicker
    private lateinit var playerGlowPicker: ColorPicker
    private lateinit var glintPicker: ColorPicker
    private lateinit var droppedFilter: ItemFilterPanel
    private lateinit var heldGlowFilter: ItemFilterPanel
    private lateinit var glintFilter: ItemFilterPanel

    private var expandedDropped = false
    private var expandedGlint = false
    private var expandedHeldGlow = false
    private var expandedPlayerGlow = false

    private var panel = UiRect(0, 0, 0, 0)
    private var sidebar = UiRect(0, 0, 0, 0)
    private var content = UiRect(0, 0, 0, 0)
    private var scissor = UiRect(0, 0, 0, 0)
    private val navHits = ArrayList<Pair<ConfigCategory, UiRect>>()

    private var droppedCard = GlowCardLayout()
    private var heldGlowCard = GlowCardLayout()
    private var playerGlowCard = GlowCardLayout()
    private var glintCard = GlintCardLayout()

    private var lastMouseX = -1.0
    private var lastMouseY = -1.0
    private var tooltipText: String? = null

    override fun init() {
        super.init()
        PopupManager.close()
        pageAnim = 1f
        droppedPicker = ColorPicker(textRenderer) { persist() }
        heldGlowPicker = ColorPicker(textRenderer) { persist() }
        playerGlowPicker = ColorPicker(textRenderer) { persist() }
        glintPicker = ColorPicker(textRenderer) {
            GlintChangeListener.fire()
            persist()
        }
        droppedFilter = ItemFilterPanel(textRenderer, ConfigManager.config.droppedItemGlow.filter) { persist() }
        heldGlowFilter = ItemFilterPanel(textRenderer, ConfigManager.config.heldItemGlow.filter) { persist() }
        glintFilter = ItemFilterPanel(textRenderer, ConfigManager.config.heldGlint.filter) { persist() }
        expandedDropped = false
        expandedGlint = false
        expandedHeldGlow = false
        expandedPlayerGlow = false
        ConfigPages.forCategory(ConfigCategory.VISUALS).forEach { it.resetUi() }
        relayout()
    }

    override fun shouldPause(): Boolean = false

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        context.fill(0, 0, width, height, HugoTheme.overlay)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        relayout()
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        tooltipText = null
        val dt = deltaTicks.coerceIn(0.01f, 1.5f)
        pageAnim = UiDraw.lerp(pageAnim, 1f, 0.25f * dt * 3f)
        toggleAnimDropped = UiDraw.lerp(
            toggleAnimDropped,
            if (ConfigManager.config.droppedItemGlow.enabled) 1f else 0f,
            0.3f * dt * 3f
        )
        toggleAnimHeldGlow = UiDraw.lerp(
            toggleAnimHeldGlow,
            if (ConfigManager.config.heldItemGlow.enabled) 1f else 0f,
            0.3f * dt * 3f
        )
        toggleAnimPlayerGlow = UiDraw.lerp(
            toggleAnimPlayerGlow,
            if (ConfigManager.config.playerGlow.enabled) 1f else 0f,
            0.3f * dt * 3f
        )
        toggleAnimGlint = UiDraw.lerp(
            toggleAnimGlint,
            if (ConfigManager.config.heldGlint.enabled) 1f else 0f,
            0.3f * dt * 3f
        )

        super.render(context, mouseX, mouseY, deltaTicks)

        UiDraw.panel(context, panel, HugoTheme.panel, HugoTheme.panelBorder)
        drawSidebar(context, mouseX, mouseY)
        drawContent(context, mouseX, mouseY)
        PopupManager.active?.let { popup ->
            popup.layout(width, height)
            popup.render(context, mouseX, mouseY)
            popup.hoveredStack()?.let { context.drawItemTooltip(textRenderer, it, mouseX, mouseY) }
        }

        val stackTooltip = category == ConfigCategory.VISUALS && hoveredVisualStack() != null
        if (!stackTooltip) {
            tooltipText?.let { UiDraw.tooltip(context, textRenderer, it, mouseX, mouseY, width, height) }
        }
    }

    private fun relayout() {
        val margin = if (height < 280) 8 else 14
        val panelW = (width - margin * 2).coerceIn(360, 680)
        val panelH = (height - margin * 2).coerceIn(240, 460)
        panel = UiRect((width - panelW) / 2, (height - panelH) / 2, panelW, panelH)
        val sidebarW = (panelW * 0.27f).toInt().coerceIn(108, 142)
        sidebar = UiRect(panel.x, panel.y, sidebarW, panelH)
        content = UiRect(panel.x + sidebarW + 14, panel.y + 12, panel.w - sidebarW - 26, panel.h - 24)
        scissor = UiRect(content.x, content.y, content.w, content.h)

        navHits.clear()
        var navY = sidebar.y + 36
        for (entry in ConfigCategory.entries) {
            navHits += entry to UiRect(sidebar.x + 8, navY - 4, sidebar.w - 16, 18)
            navY += 20
        }

        val compact = height < 300
        val pickerW = if (compact) 84 else 100
        val pickerH = if (compact) 56 else 68
        val cardW = content.w
        droppedPicker.layout(content.x + 10, 0, pickerW, pickerH)
        heldGlowPicker.layout(content.x + 10, 0, pickerW, pickerH)
        playerGlowPicker.layout(content.x + 10, 0, pickerW, pickerH)
        glintPicker.layout(content.x + 10, 0, pickerW, pickerH)

        if (category == ConfigCategory.VISUALS) {
            val extras = extraPages()
            val extraHeights = extras.map { it.layout(content.x, 0, cardW, scissor.h) }
            val droppedH = glowCardHeight(expandedDropped, droppedPicker, cardW, droppedFilter)
            val glintH = glintCardHeight(expandedGlint, glintPicker, cardW, glintFilter)
            val heldH = glowCardHeight(expandedHeldGlow, heldGlowPicker, cardW, heldGlowFilter)
            val playerH = glowCardHeight(expandedPlayerGlow, playerGlowPicker, cardW, null)
            val totalH = 4 + droppedH + 8 + glintH + 8 + heldH + 8 + playerH + 8 +
                extraHeights.sumOf { it + 8 } + 4
            maxScroll = (totalH - scissor.h).coerceAtLeast(0)
            scroll = scroll.coerceIn(0, maxScroll)

            var y = scissor.y + 4 - scroll
            layoutGlowCard(droppedCard, content.x, y, cardW, droppedPicker, pickerW, pickerH, expandedDropped, droppedFilter)
            y += droppedH + 8
            layoutGlintCard(glintCard, content.x, y, cardW, glintPicker, pickerW, pickerH, expandedGlint, glintFilter)
            y += glintH + 8
            layoutGlowCard(heldGlowCard, content.x, y, cardW, heldGlowPicker, pickerW, pickerH, expandedHeldGlow, heldGlowFilter)
            y += heldH + 8
            layoutGlowCard(playerGlowCard, content.x, y, cardW, playerGlowPicker, pickerW, pickerH, expandedPlayerGlow, null)
            y += playerH + 8
            for (page in extras) {
                val h = page.layout(content.x, y, cardW, scissor.h)
                y += h + 8
            }
        } else {
            val pages = extraPages()
            if (pages.isNotEmpty()) {
                var y = scissor.y + 4 - scroll
                var totalH = 4
                for (page in pages) {
                    val h = page.layout(content.x, y, content.w, scissor.h)
                    totalH += h + 8
                    y += h + 8
                }
                maxScroll = (totalH - scissor.h).coerceAtLeast(0)
            } else {
                maxScroll = 0
            }
            scroll = scroll.coerceIn(0, maxScroll)
        }
    }

    private fun sideBySide(cardW: Int, picker: ColorPicker): Boolean =
        cardW - 20 - picker.width() - 12 >= 140

    private fun glowCardHeight(expanded: Boolean, picker: ColorPicker, cardW: Int, filter: ItemFilterPanel?): Int {
        if (!expanded) {
            return HEADER_H
        }
        val pickerBlock = if (sideBySide(cardW, picker)) picker.height() else picker.height() + 66
        return HEADER_H + pickerBlock + 8 + 26 + 8
    }

    private fun glintCardHeight(expanded: Boolean, picker: ColorPicker, cardW: Int, filter: ItemFilterPanel): Int {
        if (!expanded) {
            return HEADER_H
        }
        val pickerBlock = if (sideBySide(cardW, picker)) picker.height() else picker.height() + 44
        return HEADER_H + 24 + pickerBlock + 8 + 26 + 8
    }

    private fun layoutGlowCard(
        card: GlowCardLayout,
        x: Int,
        y: Int,
        w: Int,
        picker: ColorPicker,
        pickerW: Int,
        pickerH: Int,
        expanded: Boolean,
        filter: ItemFilterPanel?
    ) {
        card.card = UiRect(x, y, w, glowCardHeight(expanded, picker, w, filter))
        card.toggle = UiRect(x + w - 42, y + 8, 32, 14)
        card.helper = UiRect(x + w - 58, y + 9, 12, 12)
        if (!expanded) {
            picker.layout(x + 10, y + HEADER_H, pickerW, pickerH)
            card.transparency = UiRect(0, 0, 0, 0)
            card.intensity = UiRect(0, 0, 0, 0)
            card.width = UiRect(0, 0, 0, 0)
            card.filterButton = UiRect(0, 0, 0, 0)
            filter?.layout(x, y, 0)
            return
        }
        picker.layout(x + 10, y + HEADER_H, pickerW, pickerH)
        if (sideBySide(w, picker)) {
            val sliderX = picker.field.x + picker.width() + 14
            val sliderW = (x + w - 10 - sliderX).coerceAtLeast(80)
            card.transparency = UiRect(sliderX, y + HEADER_H, sliderW, 20)
            card.intensity = UiRect(sliderX, y + HEADER_H + 24, sliderW, 20)
            card.width = UiRect(sliderX, y + HEADER_H + 48, sliderW, 20)
        } else {
            val sliderY = y + HEADER_H + picker.height() + 8
            card.transparency = UiRect(x + 10, sliderY, w - 20, 20)
            card.intensity = UiRect(x + 10, sliderY + 22, w - 20, 20)
            card.width = UiRect(x + 10, sliderY + 44, w - 20, 20)
        }
        val pickerBlock = if (sideBySide(w, picker)) picker.height() else picker.height() + 66
        card.filterButton = UiRect(x + 10, y + HEADER_H + pickerBlock + 8, w - 20, 22)
        filter?.layout(x, y, 0)
    }

    private fun layoutGlintCard(
        card: GlintCardLayout,
        x: Int,
        y: Int,
        w: Int,
        picker: ColorPicker,
        pickerW: Int,
        pickerH: Int,
        expanded: Boolean,
        filter: ItemFilterPanel
    ) {
        card.card = UiRect(x, y, w, glintCardHeight(expanded, picker, w, filter))
        card.toggle = UiRect(x + w - 42, y + 8, 32, 14)
        card.helper = UiRect(x + w - 58, y + 9, 12, 12)
        if (!expanded) {
            card.modeHits = emptyList()
            picker.layout(x + 10, y + HEADER_H, pickerW, pickerH)
            card.transparency = UiRect(0, 0, 0, 0)
            card.speed = UiRect(0, 0, 0, 0)
            card.filterButton = UiRect(0, 0, 0, 0)
            filter.layout(x, y, 0)
            return
        }
        val chipW = (w - 20 - 12) / 4
        card.modeHits = GlintStyle.Mode.entries.mapIndexed { index, mode ->
            mode to UiRect(x + 10 + index * (chipW + 4), y + HEADER_H, chipW, 18)
        }
        picker.layout(x + 10, y + HEADER_H + 24, pickerW, pickerH)
        if (sideBySide(w, picker)) {
            val sliderX = picker.field.x + picker.width() + 14
            val sliderW = (x + w - 10 - sliderX).coerceAtLeast(80)
            card.transparency = UiRect(sliderX, y + HEADER_H + 24, sliderW, 20)
            card.speed = UiRect(sliderX, y + HEADER_H + 48, sliderW, 20)
        } else {
            val sliderY = y + HEADER_H + 24 + picker.height() + 8
            card.transparency = UiRect(x + 10, sliderY, w - 20, 20)
            card.speed = UiRect(x + 10, sliderY + 22, w - 20, 20)
        }
        val pickerBlock = if (sideBySide(w, picker)) picker.height() else picker.height() + 44
        card.filterButton = UiRect(x + 10, y + HEADER_H + 24 + pickerBlock + 8, w - 20, 22)
        filter.layout(x, y, 0)
    }

    private fun drawSidebar(context: DrawContext, mouseX: Int, mouseY: Int) {
        UiDraw.fill(context, sidebar.x, sidebar.y, sidebar.w, sidebar.h, HugoTheme.sidebar)
        context.fill(sidebar.right() - 1, sidebar.y, sidebar.right(), sidebar.bottom(), HugoTheme.panelBorder)
        context.drawText(textRenderer, HugoIds.DISPLAY_NAME, sidebar.x + 12, sidebar.y + 12, HugoTheme.text, false)

        for ((entry, hit) in navHits) {
            val hovered = hit.contains(mouseX.toDouble(), mouseY.toDouble())
            val selected = entry == category
            if (selected) {
                UiDraw.fill(context, hit, HugoTheme.accentSoft)
                context.fill(hit.x, hit.y, hit.x + 2, hit.bottom(), HugoTheme.accent)
            } else if (hovered && entry.available) {
                UiDraw.fill(context, hit, 0x18FFFFFF)
            }
            val color = when {
                selected -> HugoTheme.text
                entry.available -> if (hovered) HugoTheme.text else HugoTheme.textMuted
                else -> HugoTheme.comingSoon
            }
            context.drawText(
                textRenderer,
                UiDraw.ellipsize(textRenderer, entry.title, hit.w - 12),
                hit.x + 8,
                hit.y + 5,
                color,
                false
            )
        }

        val version = FabricLoader.getInstance()
            .getModContainer(HugoIds.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("dev")
        val versionLabel = if (UpdateManager.hasUpdate()) "v$version ↑" else "v$version"
        context.drawText(
            textRenderer,
            versionLabel,
            sidebar.x + 12,
            sidebar.bottom() - 16,
            if (UpdateManager.hasUpdate()) HugoTheme.accent else HugoTheme.textDim,
            false
        )
    }

    private fun drawContent(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.enableScissor(scissor.x, scissor.y, scissor.right(), scissor.bottom())
        if (category == ConfigCategory.VISUALS) {
            drawGlowCard(
                context,
                droppedCard,
                "Dropped Item Glow",
                DROPPED_HELPER,
                ConfigManager.config.droppedItemGlow,
                toggleAnimDropped,
                droppedPicker,
                SliderId.DROPPED_TRANSPARENCY,
                SliderId.DROPPED_INTENSITY,
                SliderId.DROPPED_WIDTH,
                expandedDropped,
                droppedFilter
            )
            drawGlintCard(context)
            drawGlowCard(
                context,
                heldGlowCard,
                "Hand-Glow",
                HELD_GLOW_HELPER,
                ConfigManager.config.heldItemGlow,
                toggleAnimHeldGlow,
                heldGlowPicker,
                SliderId.HELD_TRANSPARENCY,
                SliderId.HELD_INTENSITY,
                SliderId.HELD_WIDTH,
                expandedHeldGlow,
                heldGlowFilter
            )
            drawGlowCard(
                context,
                playerGlowCard,
                "Player Glow",
                PLAYER_GLOW_HELPER,
                ConfigManager.config.playerGlow,
                toggleAnimPlayerGlow,
                playerGlowPicker,
                SliderId.PLAYER_TRANSPARENCY,
                SliderId.PLAYER_INTENSITY,
                SliderId.PLAYER_WIDTH,
                expandedPlayerGlow,
                null
            )
            for (page in extraPages()) {
                page.render(context, mouseX, mouseY)
            }
        } else {
            val pages = extraPages()
            if (pages.isNotEmpty()) {
                for (page in pages) {
                    page.render(context, mouseX, mouseY)
                }
            } else {
                UiDraw.panel(context, scissor.x, scissor.y + 8, scissor.w, 40, HugoTheme.card, HugoTheme.cardBorder)
                context.drawText(textRenderer, "Bald verfügbar", scissor.x + 12, scissor.y + 22, HugoTheme.text, false)
            }
        }
        context.disableScissor()
        if (category == ConfigCategory.VISUALS || extraPages().isNotEmpty()) {
            UiDraw.scrollbar(context, scissor, scroll, maxScroll)
        }
        if (category == ConfigCategory.VISUALS) {
            hoveredVisualStack()?.let { stack ->
                context.drawItemTooltip(textRenderer, stack, mouseX, mouseY)
            }
        }
    }

    private fun drawGlowCard(
        context: DrawContext,
        layout: GlowCardLayout,
        title: String,
        helperText: String,
        style: GlowStyle,
        toggleAnim: Float,
        picker: ColorPicker,
        transparencyId: SliderId,
        intensityId: SliderId,
        widthId: SliderId,
        expanded: Boolean,
        filter: ItemFilterPanel?
    ) {
        UiDraw.panel(context, layout.card, HugoTheme.card, HugoTheme.cardBorder)
        drawCardHeader(context, layout.card, layout.toggle, layout.helper, title, expanded, toggleAnim, helperText)
        if (!expanded) {
            return
        }
        picker.render(context, style)
        drawSlider(context, layout.transparency, "Deckkraft", style.opacity, transparencyId)
        if (style === ConfigManager.config.heldItemGlow) {
            drawSlider(context, layout.width, "Konturbreite: ${style.thicknessPixels}px",
                (style.thicknessPixels - 1) / 3f, widthId)
        }
        drawFilterButton(context, layout.filterButton, if (filter == null) "Spieler auswählen…" else "Items / Blöcke auswählen…")
    }

    private fun drawGlintCard(context: DrawContext) {
        val layout = glintCard
        val glint = ConfigManager.config.heldGlint
        val mode = glint.mode()
        UiDraw.panel(context, layout.card, HugoTheme.card, HugoTheme.cardBorder)
        drawCardHeader(context, layout.card, layout.toggle, layout.helper, "Hand-Glint", expandedGlint, toggleAnimGlint, GLINT_HELPER)
        if (!expandedGlint) {
            return
        }

        for ((entry, hit) in layout.modeHits) {
            UiDraw.chip(
                context,
                textRenderer,
                hit,
                entry.label,
                entry == mode,
                hit.contains(lastMouseX, lastMouseY)
            )
        }

        glintPicker.render(context, glint)
        if (mode != GlintStyle.Mode.CUSTOM) {
            val px = glintPicker.field.x - 2
            val py = glintPicker.field.y - 2
            UiDraw.fill(context, px, py, glintPicker.width() + 4, glintPicker.height() + 4, 0xB010131A.toInt())
        }

        drawSlider(context, layout.transparency, "Deckkraft", glint.glintAlpha(), SliderId.GLINT_TRANSPARENCY)
        val speedFactor = 0.2f + glint.speed * 2.3f
        drawSlider(context, layout.speed, "Geschwindigkeit (${String.format("%.1f", speedFactor)}×)", glint.speed, SliderId.GLINT_SPEED)
        drawFilterButton(context, layout.filterButton, "Items / Blöcke auswählen…")
    }

    private fun drawFilterButton(context: DrawContext, rect: UiRect, label: String) {
        val hovered = rect.contains(lastMouseX, lastMouseY)
        UiDraw.fill(context, rect, if (hovered) HugoTheme.accentSoft else HugoTheme.inset)
        UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, if (hovered) HugoTheme.accent else HugoTheme.cardBorder)
        context.drawText(textRenderer, label, rect.x + 7, rect.y + 7, if (hovered) HugoTheme.text else HugoTheme.textMuted, false)
    }

    private fun drawCardHeader(
        context: DrawContext,
        card: UiRect,
        toggle: UiRect,
        helper: UiRect,
        title: String,
        expanded: Boolean,
        toggleAnim: Float,
        helperText: String
    ) {
        val chevron = if (expanded) "▾" else "▸"
        context.drawText(textRenderer, chevron, card.x + 10, card.y + 10, HugoTheme.textMuted, false)
        context.drawText(textRenderer, title, card.x + 22, card.y + 9, HugoTheme.text, false)
        drawToggle(context, toggle, toggleAnim)
        val helperHovered = helper.contains(lastMouseX, lastMouseY)
        UiDraw.helperBadge(context, textRenderer, helper, helperHovered)
        if (helperHovered) {
            tooltipText = helperText
        }
    }

    private fun drawToggle(context: DrawContext, rect: UiRect, anim: Float) {
        val hovered = rect.contains(lastMouseX, lastMouseY)
        val on = HugoTheme.lerpColor(HugoTheme.trackOff, HugoTheme.success, anim)
        UiDraw.fill(context, rect, on)
        UiDraw.border(context, rect.x, rect.y, rect.w, rect.h, if (hovered) HugoTheme.accent else HugoTheme.cardBorder)
        val knobW = 12
        val knobX = rect.x + 2 + ((rect.w - knobW - 4) * anim).roundToInt()
        UiDraw.fill(context, knobX, rect.y + 2, knobW, rect.h - 4, HugoTheme.knob)
    }

    private fun drawSlider(context: DrawContext, rect: UiRect, label: String, value: Float, id: SliderId) {
        val hovered = rect.contains(lastMouseX, lastMouseY) || draggingSlider == id
        context.drawText(textRenderer, label, rect.x, rect.y, if (hovered) HugoTheme.text else HugoTheme.textMuted, false)
        val percent = "${(value * 100).roundToInt()}%"
        context.drawText(textRenderer, percent, rect.right() - textRenderer.getWidth(percent), rect.y, HugoTheme.text, false)
        val trackY = rect.y + 12
        UiDraw.fill(context, rect.x, trackY, rect.w, 6, HugoTheme.inset)
        UiDraw.border(context, rect.x, trackY, rect.w, 6, if (hovered) HugoTheme.accentMuted else HugoTheme.cardBorder)
        val fillW = (rect.w * value).roundToInt().coerceIn(2, rect.w)
        UiDraw.fill(context, rect.x + 1, trackY + 1, fillW - 2, 4, HugoTheme.accent)
        val knobX = rect.x + ((rect.w - 6) * value).roundToInt()
        UiDraw.fill(context, knobX, trackY - 2, 6, 10, if (hovered) HugoTheme.accent else HugoTheme.knob)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        PopupManager.active?.let {
            val (mx, my) = pointer(click)
            return it.mouseClicked(mx, my)
        }
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(click, doubled)
        }
        relayout()
        val (mx, my) = pointer(click)

        for ((entry, hit) in navHits) {
            if (hit.contains(mx, my)) {
                if (entry.available && entry != category) {
                    category = entry
                    pageAnim = 0f
                    scroll = 0
                }
                unfocusAll()
                return true
            }
        }

        if (category == ConfigCategory.VISUALS && scissor.contains(mx, my)) {
            if (droppedCard.toggle.contains(mx, my)) {
                ConfigManager.update { it.droppedItemGlow.enabled = !it.droppedItemGlow.enabled }
                return true
            }
            if (titleHit(droppedCard.card, droppedCard.toggle, droppedCard.helper, mx, my)) {
                expandedDropped = !expandedDropped
                if (!expandedDropped) droppedFilter.unfocus()
                relayout()
                return true
            }
            if (glintCard.toggle.contains(mx, my)) {
                ConfigManager.update { it.heldGlint.enabled = !it.heldGlint.enabled }
                return true
            }
            if (titleHit(glintCard.card, glintCard.toggle, glintCard.helper, mx, my)) {
                expandedGlint = !expandedGlint
                if (!expandedGlint) glintFilter.unfocus()
                relayout()
                return true
            }
            if (heldGlowCard.toggle.contains(mx, my)) {
                ConfigManager.update { it.heldItemGlow.enabled = !it.heldItemGlow.enabled }
                return true
            }
            if (titleHit(heldGlowCard.card, heldGlowCard.toggle, heldGlowCard.helper, mx, my)) {
                expandedHeldGlow = !expandedHeldGlow
                if (!expandedHeldGlow) heldGlowFilter.unfocus()
                relayout()
                return true
            }
            if (playerGlowCard.toggle.contains(mx, my)) {
                ConfigManager.update { it.playerGlow.enabled = !it.playerGlow.enabled }
                return true
            }
            if (titleHit(playerGlowCard.card, playerGlowCard.toggle, playerGlowCard.helper, mx, my)) {
                expandedPlayerGlow = !expandedPlayerGlow
                relayout()
                return true
            }
            if (expandedGlint) {
                for ((mode, hit) in glintCard.modeHits) {
                    if (hit.contains(mx, my)) {
                        ConfigManager.update { it.heldGlint.mode = mode.id }
                        GlintChangeListener.fire()
                        if (mode != GlintStyle.Mode.CUSTOM) {
                            glintPicker.unfocus()
                        }
                        return true
                    }
                }
            }
            if (expandedDropped && droppedCard.filterButton.contains(mx, my)) {
                PopupManager.open(ItemFilterPopup("Dropped Item Glow", ConfigManager.config.droppedItemGlow.filter) { persist() })
                return true
            }
            if (expandedHeldGlow && heldGlowCard.filterButton.contains(mx, my)) {
                PopupManager.open(ItemFilterPopup("Hand-Glow", ConfigManager.config.heldItemGlow.filter) { persist() })
                return true
            }
            if (expandedPlayerGlow && playerGlowCard.filterButton.contains(mx, my)) {
                PopupManager.open(PlayerFilterPopup(ConfigManager.config.playerGlow.playerFilter) { persist() })
                return true
            }
            if (expandedGlint && glintCard.filterButton.contains(mx, my)) {
                PopupManager.open(ItemFilterPopup("Hand-Glint", ConfigManager.config.heldGlint.filter) { persist() })
                return true
            }
            if (expandedDropped && droppedPicker.mouseClicked(ConfigManager.config.droppedItemGlow, mx, my)) {
                heldGlowPicker.unfocus()
                playerGlowPicker.unfocus()
                glintPicker.unfocus()
                unfocusFilters()
                persist()
                return true
            }
            if (expandedHeldGlow && heldGlowPicker.mouseClicked(ConfigManager.config.heldItemGlow, mx, my)) {
                droppedPicker.unfocus()
                playerGlowPicker.unfocus()
                glintPicker.unfocus()
                unfocusFilters()
                persist()
                return true
            }
            if (expandedPlayerGlow && playerGlowPicker.mouseClicked(ConfigManager.config.playerGlow, mx, my)) {
                droppedPicker.unfocus()
                heldGlowPicker.unfocus()
                glintPicker.unfocus()
                unfocusFilters()
                persist()
                return true
            }
            if (expandedGlint &&
                ConfigManager.config.heldGlint.mode() == GlintStyle.Mode.CUSTOM &&
                glintPicker.mouseClicked(ConfigManager.config.heldGlint, mx, my)
            ) {
                droppedPicker.unfocus()
                heldGlowPicker.unfocus()
                playerGlowPicker.unfocus()
                unfocusFilters()
                persist()
                return true
            }
            for (id in ALL_SLIDERS) {
                if (sliderEnabled(id) && sliderRect(id).contains(mx, my)) {
                    draggingSlider = id
                    applySlider(id, mx)
                    persist()
                    return true
                }
            }
            for (page in extraPages()) {
                if (page.mouseClicked(mx, my)) {
                    persist()
                    relayout()
                    return true
                }
            }
        } else {
            for (page in extraPages()) {
                if (scissor.contains(mx, my) && page.mouseClicked(mx, my)) {
                    persist()
                    return true
                }
            }
        }

        unfocusAll()
        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
        val (mx, my) = pointer(click)
        if (category == ConfigCategory.VISUALS) {
            val pickerDragged =
                (expandedDropped &&
                    droppedPicker.mouseDragged(ConfigManager.config.droppedItemGlow, mx, my)) ||
                    (expandedHeldGlow &&
                        heldGlowPicker.mouseDragged(ConfigManager.config.heldItemGlow, mx, my)) ||
                    (expandedPlayerGlow &&
                        playerGlowPicker.mouseDragged(ConfigManager.config.playerGlow, mx, my)) ||
                    (expandedGlint &&
                        glintPicker.mouseDragged(ConfigManager.config.heldGlint, mx, my))
            if (pickerDragged) {
                persist()
                return true
            }
        }
        for (page in extraPages()) {
            if (page.mouseDragged(mx, my)) {
                persist()
                return true
            }
        }
        draggingSlider?.let { id ->
            if (!sliderEnabled(id)) {
                draggingSlider = null
                return@let
            }
            applySlider(id, mx)
            persist()
            return true
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: Click): Boolean {
        droppedPicker.mouseReleased()
        heldGlowPicker.mouseReleased()
        playerGlowPicker.mouseReleased()
        glintPicker.mouseReleased()
        extraPages().forEach { it.mouseReleased() }
        draggingSlider = null
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        val (mx, my) = scalePointer(mouseX, mouseY)
        PopupManager.active?.let { return it.mouseScrolled(mx, my, verticalAmount) }
        if (category == ConfigCategory.VISUALS) {
            if (expandedDropped && droppedFilter.mouseScrolled(mx, my, verticalAmount)
            ) {
                return true
            }
            if (expandedHeldGlow && heldGlowFilter.mouseScrolled(mx, my, verticalAmount)
            ) {
                return true
            }
            if (expandedGlint && glintFilter.mouseScrolled(mx, my, verticalAmount)
            ) {
                return true
            }
        }
        for (page in extraPages()) {
            if (page.mouseScrolled(mx, my, verticalAmount)) {
                return true
            }
        }
        if (scissor.contains(mx, my) || content.contains(mx, my)) {
            scroll = (scroll - (verticalAmount * 18).roundToInt()).coerceIn(0, maxScroll)
            relayout()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        PopupManager.active?.let { return it.keyPressed(input) }
        val extrasHandled = extraPages().any { it.keyPressed(input) }
        val handled = extrasHandled || if (category == ConfigCategory.VISUALS) {
            (expandedDropped &&
                (droppedFilter.keyPressed(input) ||
                    droppedPicker.keyPressed(ConfigManager.config.droppedItemGlow, input))) ||
                (expandedHeldGlow &&
                    (heldGlowFilter.keyPressed(input) ||
                        heldGlowPicker.keyPressed(ConfigManager.config.heldItemGlow, input))) ||
                (expandedPlayerGlow &&
                    playerGlowPicker.keyPressed(ConfigManager.config.playerGlow, input)) ||
                (expandedGlint &&
                    (glintFilter.keyPressed(input) ||
                        glintPicker.keyPressed(ConfigManager.config.heldGlint, input)))
        } else {
            false
        }
        if (handled) {
            return true
        }
        return super.keyPressed(input)
    }

    override fun charTyped(input: CharInput): Boolean {
        PopupManager.active?.let { return it.charTyped(input) }
        val extrasHandled = extraPages().any { it.charTyped(input) }
        val handled = extrasHandled || if (category == ConfigCategory.VISUALS) {
            (expandedDropped &&
                (droppedFilter.charTyped(input) ||
                    droppedPicker.charTyped(ConfigManager.config.droppedItemGlow, input))) ||
                (expandedHeldGlow &&
                    (heldGlowFilter.charTyped(input) ||
                        heldGlowPicker.charTyped(ConfigManager.config.heldItemGlow, input))) ||
                (expandedPlayerGlow &&
                    playerGlowPicker.charTyped(ConfigManager.config.playerGlow, input)) ||
                (expandedGlint &&
                    (glintFilter.charTyped(input) ||
                        glintPicker.charTyped(ConfigManager.config.heldGlint, input)))
        } else {
            false
        }
        if (handled) {
            return true
        }
        return super.charTyped(input)
    }

    private fun extraPages(): List<ConfigPage> = ConfigPages.forCategory(category)

    private fun titleHit(card: UiRect, toggle: UiRect, helper: UiRect, mx: Double, my: Double): Boolean {
        return mx >= card.x && mx < helper.x - 2 &&
            my >= card.y && my < card.y + HEADER_H &&
            !toggle.contains(mx, my)
    }

    private fun sliderEnabled(id: SliderId): Boolean = when (id) {
        SliderId.DROPPED_TRANSPARENCY -> expandedDropped
        SliderId.DROPPED_INTENSITY, SliderId.DROPPED_WIDTH -> false
        SliderId.GLINT_TRANSPARENCY, SliderId.GLINT_SPEED -> expandedGlint
        SliderId.HELD_TRANSPARENCY, SliderId.HELD_WIDTH -> expandedHeldGlow
        SliderId.HELD_INTENSITY -> false
        SliderId.PLAYER_TRANSPARENCY -> expandedPlayerGlow
        SliderId.PLAYER_INTENSITY, SliderId.PLAYER_WIDTH -> false
    }

    private fun hoveredVisualStack() =
        droppedFilter.hoveredStack
            ?: heldGlowFilter.hoveredStack
            ?: glintFilter.hoveredStack
            ?: extraPages().firstNotNullOfOrNull { it.hoveredStack() }

    private fun sliderRect(id: SliderId): UiRect = when (id) {
        SliderId.DROPPED_TRANSPARENCY -> droppedCard.transparency
        SliderId.DROPPED_INTENSITY -> droppedCard.intensity
        SliderId.DROPPED_WIDTH -> droppedCard.width
        SliderId.GLINT_TRANSPARENCY -> glintCard.transparency
        SliderId.GLINT_SPEED -> glintCard.speed
        SliderId.HELD_TRANSPARENCY -> heldGlowCard.transparency
        SliderId.HELD_INTENSITY -> heldGlowCard.intensity
        SliderId.HELD_WIDTH -> heldGlowCard.width
        SliderId.PLAYER_TRANSPARENCY -> playerGlowCard.transparency
        SliderId.PLAYER_INTENSITY -> playerGlowCard.intensity
        SliderId.PLAYER_WIDTH -> playerGlowCard.width
    }

    private fun applySlider(id: SliderId, mouseX: Double) {
        val rect = sliderRect(id)
        val value = ((mouseX - rect.x) / rect.w.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        val config = ConfigManager.config
        when (id) {
            SliderId.DROPPED_TRANSPARENCY -> config.droppedItemGlow.opacity = value
            SliderId.DROPPED_INTENSITY, SliderId.DROPPED_WIDTH -> Unit
            SliderId.GLINT_TRANSPARENCY -> config.heldGlint.transparency = 1f - value
            SliderId.GLINT_SPEED -> config.heldGlint.speed = value
            SliderId.HELD_TRANSPARENCY -> config.heldItemGlow.opacity = value
            SliderId.HELD_INTENSITY -> Unit
            SliderId.HELD_WIDTH -> config.heldItemGlow.thicknessPixels = (1 + value * 3f).roundToInt().coerceIn(1, 4)
            SliderId.PLAYER_TRANSPARENCY -> config.playerGlow.opacity = value
            SliderId.PLAYER_INTENSITY, SliderId.PLAYER_WIDTH -> Unit
        }
    }

    private fun unfocusPickers() {
        droppedPicker.unfocus()
        heldGlowPicker.unfocus()
        playerGlowPicker.unfocus()
        glintPicker.unfocus()
    }

    private fun unfocusFilters() {
        droppedFilter.unfocus()
        heldGlowFilter.unfocus()
        glintFilter.unfocus()
    }

    private fun unfocusAll() {
        unfocusPickers()
        unfocusFilters()
    }

    private fun pointer(click: Click): Pair<Double, Double> = scalePointer(click.x(), click.y())

    private fun scalePointer(x: Double, y: Double): Pair<Double, Double> {
        val current = client ?: return x to y
        return if (x > width + 2 || y > height + 2) {
            Mouse.scaleX(current.window, x) to Mouse.scaleY(current.window, y)
        } else {
            x to y
        }
    }

    private fun persist() {
        ConfigManager.requestSave()
    }

    private enum class SliderId {
        DROPPED_TRANSPARENCY,
        DROPPED_INTENSITY,
        DROPPED_WIDTH,
        GLINT_TRANSPARENCY,
        GLINT_SPEED,
        HELD_TRANSPARENCY,
        HELD_INTENSITY,
        HELD_WIDTH,
        PLAYER_TRANSPARENCY,
        PLAYER_INTENSITY,
        PLAYER_WIDTH
    }

    private class GlowCardLayout {
        var card = UiRect(0, 0, 0, 0)
        var toggle = UiRect(0, 0, 0, 0)
        var helper = UiRect(0, 0, 0, 0)
        var transparency = UiRect(0, 0, 0, 0)
        var intensity = UiRect(0, 0, 0, 0)
        var width = UiRect(0, 0, 0, 0)
        var filterButton = UiRect(0, 0, 0, 0)
    }

    private class GlintCardLayout {
        var card = UiRect(0, 0, 0, 0)
        var toggle = UiRect(0, 0, 0, 0)
        var helper = UiRect(0, 0, 0, 0)
        var modeHits = emptyList<Pair<GlintStyle.Mode, UiRect>>()
        var transparency = UiRect(0, 0, 0, 0)
        var speed = UiRect(0, 0, 0, 0)
        var filterButton = UiRect(0, 0, 0, 0)
    }

    companion object {
        private const val HEADER_H = 30
        private val ALL_SLIDERS = SliderId.entries.toSet()

        private const val DROPPED_HELPER =
            "Leuchtende Silhouette um gedroppte Items.\n" +
                "- Farbe: Klicken und Ziehen im Farbfeld, Farbton am rechten Balken, Helligkeit am unteren Balken. " +
                "Ins Hex-Feld kannst du exakte Farbcodes tippen.\n" +
                "- Transparenz: Wie durchsichtig der Glow ist.\n" +
                "- Kontur-Stärke: Wie kräftig und satt die Umrandung leuchtet.\n" +
                "- Kontur-Breite: Von einer dünnen Kante bis zu einem breiten Glow.\n" +
                "- Items: Welche gedroppten Items den Glow bekommen.\n" +
                "Der Glow ist tiefengetestet und deshalb niemals durch Blöcke sichtbar."

        private const val GLINT_HELPER =
            "Verzauberungs-Glanz auf Items in deiner Hand (Eigen- und Fremdperspektive).\n" +
                "- Vanilla: normaler lila Glanz.\n" +
                "- Stark: intensiverer Vanilla-Glanz.\n" +
                "- Eigene Farbe: Glint in deiner Wunschfarbe (Farbfeld nutzen).\n" +
                "- Regenbogen: Die Farbe wechselt automatisch.\n" +
                "- Transparenz: Wie stark der Glint sichtbar ist.\n" +
                "- Geschwindigkeit: Wie schnell der Glint über das Item wandert.\n" +
                "- Items: Welche gehaltenen Items den Glint bekommen."

        private const val HELD_GLOW_HELPER =
            "Leuchtende Umrandung um das Item in deiner Hand - wie der Dropped-Glow, nur am gehaltenen Item.\n" +
                "Farbe, Transparenz, Stärke und Breite funktionieren genauso wie dort. " +
                "Items lassen sich separat filtern. " +
                "Funktioniert in normaler Ansicht und F5; in der Welt bleibt der Glow hinter Blöcken verborgen."

        private const val PLAYER_GLOW_HELPER =
            "Leuchtende, frei einstellbare Kontur um sichtbare Spieler.\n" +
                "- Normale Ansicht: andere sichtbare Spieler leuchten.\n" +
                "- F5: zusätzlich kann dein eigenes Spielermodell leuchten.\n" +
                "- Blöcke verdecken den Glow vollständig; es ist kein Wallhack/ESP.\n" +
                "Farbe, Transparenz, Kontur-Stärke und Kontur-Breite sind getrennt einstellbar."
    }
}
