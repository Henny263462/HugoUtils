package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.HugoIds
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.GlintStyle
import dev.henny.hugoutils.client.config.GlintChangeListener
import dev.henny.hugoutils.client.config.GlowStyle
import dev.henny.hugoutils.update.UpdateManager
import dev.henny.hugoutils.ui.Hsv
import dev.henny.hugoutils.ui.NavigationEntry
import dev.henny.hugoutils.ui.ScreenShell
import dev.henny.hugoutils.ui.UiFrame
import dev.henny.hugoutils.ui.UiNavigation
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.Mouse
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.GameMenuScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class HugoScreen(
    private val initialPageId: String? = null,
    val parent: Screen? = null
) : ScreenShell(Text.literal(HugoIds.DISPLAY_NAME)) {
    private var category = ConfigCategory.entries.firstOrNull { it.id == initialPageId }
        ?: ConfigCategory.MARKET_HOME
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

    private var scissor = UiRect(0, 0, 0, 0)
    private val navHits = ArrayList<Pair<NavigationEntry, UiRect>>()
    private val subnavHits = ArrayList<Pair<NavigationEntry, UiRect>>()
    private val footerHits = ArrayList<Pair<NavigationEntry, UiRect>>()

    private var droppedCard = GlowCardLayout()
    private var heldGlowCard = GlowCardLayout()
    private var playerGlowCard = GlowCardLayout()
    private var glintCard = GlintCardLayout()

    private var lastMouseX = -1.0
    private var lastMouseY = -1.0
    private var tooltipText: String? = null
    private val globalSearch = GlobalSearch(
        onOpenPage = { id -> selectPage(id) },
        onOpenItem = { id ->
            selectPage(ConfigCategory.MARKET_ITEMS.id)
            ConfigPages.forCategory(ConfigCategory.MARKET_ITEMS)
                .filterIsInstance<MarketConfigPage>()
                .firstOrNull()
                ?.openItemId(id)
        }
    )

    override fun init() {
        selectedPageId = landingPageId()
        ConfigCategory.entries.firstOrNull { it.id == selectedPageId }?.let { category = it }
        val landingChildren = UiNavigation.registry.children(category.id)
        if (landingChildren.isNotEmpty() && ConfigPages.forId(category.id).isEmpty()) {
            val remembered = rememberedChild(category, landingChildren)
            val first = remembered ?: landingChildren.firstOrNull { it.available } ?: landingChildren.first()
            selectedPageId = first.id
            ConfigCategory.entries.firstOrNull { it.id == first.id }?.let { category = it }
        }
        super.init()
        PopupManager.close()
        MarketLinks.openPage = { selectPage(it) }
        pageTransition.snapTo(1f)
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
        registerVisualPages()
        expandedDropped = true
        expandedGlint = true
        expandedHeldGlow = true
        expandedPlayerGlow = true
        UiNavigation.registry.entries().flatMap { UiNavigation.registry.pages(it.id) }.forEach { it.resetUi() }
        relayout()
        notifyShown()
    }

    override fun close() {
        client?.setScreen(parent)
    }

    override fun removed() {
        UiWidgets.clearAnimationStates()
        super.removed()
    }

    override fun subnavParentId(): String? = when (category.group) {
        NavGroup.MARKET -> ConfigCategory.MARKET.id
        NavGroup.MODS -> ConfigCategory.MODS.id
        NavGroup.SETTINGS -> ConfigCategory.SETTINGS.id
        else -> when (category) {
            ConfigCategory.MARKET -> ConfigCategory.MARKET.id
            ConfigCategory.MODS -> ConfigCategory.MODS.id
            ConfigCategory.SETTINGS -> ConfigCategory.SETTINGS.id
            else -> null
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        tickShell()
        relayout()
        lastMouseX = mouseX.toDouble()
        lastMouseY = mouseY.toDouble()
        tooltipText = null
        val dt = deltaTicks.coerceIn(0.01f, 1.5f)
        pageTransition.update(UiFrame.deltaSeconds)
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
        renderShell(context)
        drawSidebar(context, mouseX, mouseY)
        drawContent(context, mouseX, mouseY)
        PopupManager.active?.let { popup ->
            popup.layout(width, height)
            popup.render(context, mouseX, mouseY)
            popup.hoveredStack()?.let { context.drawItemTooltip(textRenderer, it, mouseX, mouseY) }
        }

        val stackTooltip = hoveredContentStack() != null
        if (!stackTooltip) {
            tooltipText?.let { UiDraw.tooltip(context, textRenderer, it, mouseX, mouseY, width, height) }
        }
    }

    private fun relayout() {
        relayoutShell()
        globalSearch.layout(content.x, content.y, content.w)
        scissor = UiRect(content.x, content.y + globalSearch.height(), content.w, (content.h - globalSearch.height()).coerceAtLeast(80))

        navHits.clear()
        subnavHits.clear()
        footerHits.clear()
        val registry = UiNavigation.registry
        val footerEntries = registry.footerEntries()
        val footerStart = sidebar.bottom() - 18 - 8 - footerEntries.size * 28
        val railEntries = registry.roots()
        var navY = sidebar.y + 34
        for (entry in railEntries) {
            navHits += entry to UiRect(sidebar.x + 8, navY, sidebar.w - 16, 24)
            navY += 28
        }
        var footerY = footerStart
        for (entry in footerEntries) {
            footerHits += entry to UiRect(sidebar.x + 8, footerY, sidebar.w - 16, 24)
            footerY += 28
        }

        val parentId = subnavParentId()
        if (parentId != null && subnav.w > 12) {
            var subY = subnav.y + 32
            for (entry in registry.children(parentId).filter { it.available }) {
                subnavHits += entry to UiRect(subnav.x + 6, subY, (subnav.w - 12).coerceAtLeast(8), 22)
                subY += 26
            }
        }

        val pages = extraPages()
        if (pages.size == 1) {
            val h = pages[0].layout(content.x, scissor.y - scroll, content.w, scissor.h)
            maxScroll = (h - scissor.h).coerceAtLeast(0)
        } else if (pages.isNotEmpty()) {
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

    private fun sideBySide(cardW: Int, picker: ColorPicker): Boolean =
        cardW - 20 - picker.width() - 12 >= 140

    private fun registerVisualPages() {
        BUILT_IN_VISUALS.forEach { visual ->
            val entry = UiNavigation.registry.entry(visual.id) ?: return@forEach
            ConfigPages.register(
                entry,
                VisualConfigPage(visual.id, ::layoutVisualPage, ::renderVisualPage, visual)
            )
        }
    }

    private fun layoutVisualPage(
        visual: ConfigCategory,
        x: Int,
        y: Int,
        width: Int,
        availableHeight: Int
    ): Int {
        val compact = availableHeight < 276
        val pickerW = if (compact) 88 else 112
        val pickerH = if (compact) 60 else 78
        return when (visual) {
            ConfigCategory.DROPPED_GLOW -> {
                droppedPicker.layout(x + 10, y + HEADER_H, pickerW, pickerH)
                layoutGlowCard(droppedCard, x, y, width, droppedPicker, pickerW, pickerH, true, droppedFilter)
                glowCardHeight(true, droppedPicker, width, droppedFilter)
            }
            ConfigCategory.HAND_GLINT -> {
                glintPicker.layout(x + 10, y + HEADER_H + 24, pickerW, pickerH)
                layoutGlintCard(glintCard, x, y, width, glintPicker, pickerW, pickerH, true, glintFilter)
                glintCardHeight(true, glintPicker, width, glintFilter)
            }
            ConfigCategory.HAND_GLOW -> {
                heldGlowPicker.layout(x + 10, y + HEADER_H, pickerW, pickerH)
                layoutGlowCard(heldGlowCard, x, y, width, heldGlowPicker, pickerW, pickerH, true, heldGlowFilter)
                glowCardHeight(true, heldGlowPicker, width, heldGlowFilter)
            }
            ConfigCategory.PLAYER_GLOW -> {
                playerGlowPicker.layout(x + 10, y + HEADER_H, pickerW, pickerH)
                layoutGlowCard(playerGlowCard, x, y, width, playerGlowPicker, pickerW, pickerH, true, null)
                glowCardHeight(true, playerGlowPicker, width, null)
            }
            else -> 0
        }
    }

    private fun renderVisualPage(visual: ConfigCategory, context: DrawContext, mouseX: Int, mouseY: Int) {
        when (visual) {
            ConfigCategory.DROPPED_GLOW -> drawGlowCard(
                context, droppedCard, "Dropped Item Glow", DROPPED_HELPER,
                ConfigManager.config.droppedItemGlow, toggleAnimDropped, droppedPicker,
                SliderId.DROPPED_TRANSPARENCY, SliderId.DROPPED_INTENSITY, SliderId.DROPPED_WIDTH,
                true, droppedFilter
            )
            ConfigCategory.HAND_GLINT -> drawGlintCard(context)
            ConfigCategory.HAND_GLOW -> drawGlowCard(
                context, heldGlowCard, "Hand-Glow", HELD_GLOW_HELPER,
                ConfigManager.config.heldItemGlow, toggleAnimHeldGlow, heldGlowPicker,
                SliderId.HELD_TRANSPARENCY, SliderId.HELD_INTENSITY, SliderId.HELD_WIDTH,
                true, heldGlowFilter
            )
            ConfigCategory.PLAYER_GLOW -> drawGlowCard(
                context, playerGlowCard, "Player Glow", PLAYER_GLOW_HELPER,
                ConfigManager.config.playerGlow, toggleAnimPlayerGlow, playerGlowPicker,
                SliderId.PLAYER_TRANSPARENCY, SliderId.PLAYER_INTENSITY, SliderId.PLAYER_WIDTH,
                true, null
            )
            else -> Unit
        }
    }

    private fun glowCardHeight(expanded: Boolean, picker: ColorPicker, cardW: Int, filter: ItemFilterPanel?): Int {
        if (!expanded) {
            return HEADER_H
        }
        val pickerBlock = if (sideBySide(cardW, picker)) picker.height() else picker.height() + 66
        return HEADER_H + pickerBlock + 8 + 28 + 8
    }

    private fun glintCardHeight(expanded: Boolean, picker: ColorPicker, cardW: Int, filter: ItemFilterPanel): Int {
        if (!expanded) {
            return HEADER_H
        }
        val pickerBlock = if (sideBySide(cardW, picker)) picker.height() else picker.height() + 44
        return HEADER_H + 24 + pickerBlock + 8 + 28 + 8
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
        card.filterButton = UiRect(x + 10, y + HEADER_H + pickerBlock + 8, w - 20, 24)
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
        card.filterButton = UiRect(x + 10, y + HEADER_H + 24 + pickerBlock + 8, w - 20, 24)
        filter.layout(x, y, 0)
    }

    private fun drawSidebar(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(textRenderer, HugoIds.DISPLAY_NAME, sidebar.x + 12, sidebar.y + 14, HugoTheme.textMuted, false)

        val activeRail = category.parentId ?: category.id
        for ((entry, hit) in navHits) {
            drawNavEntry(context, entry, hit, mouseX, mouseY, selected = entry.id == activeRail, compact = false)
        }
        for ((entry, hit) in footerHits) {
            drawNavEntry(context, entry, hit, mouseX, mouseY, selected = entry.id == selectedPageId, compact = false)
        }

        if (subnav.w > 12) {
            context.enableScissor(subnav.x, subnav.y + 8, subnav.right(), subnav.bottom() - 8)
            context.matrices.pushMatrix()
            context.matrices.translate((1f - subnavReveal.value) * -16f, 0f)
            val parentTitle = UiNavigation.registry.entry(subnavParentId().orEmpty())?.title ?: "Mods"
            context.drawText(textRenderer, parentTitle, subnav.x + 10, subnav.y + 14, HugoTheme.textDim, false)
            for ((entry, hit) in subnavHits) {
                drawNavEntry(context, entry, hit, mouseX, mouseY, selected = entry.id == selectedPageId, compact = true)
            }
            context.matrices.popMatrix()
            context.disableScissor()
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
            sidebar.bottom() - 18,
            if (UpdateManager.hasUpdate()) HugoTheme.accent else HugoTheme.textDim,
            false
        )
    }

    private fun drawNavEntry(
        context: DrawContext,
        entry: NavigationEntry,
        hit: UiRect,
        mouseX: Int,
        mouseY: Int,
        selected: Boolean,
        compact: Boolean
    ) {
        val hovered = hit.contains(mouseX.toDouble(), mouseY.toDouble())
        UiWidgets.navItem(context, textRenderer, hit, entry.title, selected, hovered, compact, entry.available, key = entry.id)
        if (entry.id == ConfigCategory.UPDATES.id && UpdateManager.hasUpdate() && !selected) {
            context.fill(hit.right() - 8, hit.y + 7, hit.right() - 4, hit.y + 11, HugoTheme.accent)
        }
    }

    private fun drawContent(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.enableScissor(scissor.x, scissor.y, scissor.right(), scissor.bottom())
        context.matrices.pushMatrix()
        context.matrices.translate((1f - pageTransition.value) * 24f, 0f)
        val pages = extraPages()
        if (pages.isNotEmpty()) {
            pages.forEach { it.render(context, mouseX, mouseY) }
        } else {
            UiDraw.panel(context, scissor.x, scissor.y + 8, scissor.w, 40, HugoTheme.card, HugoTheme.cardBorder)
            context.drawText(textRenderer, "Bald verfügbar", scissor.x + 12, scissor.y + 22, HugoTheme.text, false)
        }
        context.matrices.popMatrix()
        context.disableScissor()
        globalSearch.render(context, textRenderer, mouseX, mouseY)
        if (pages.isNotEmpty()) {
            UiDraw.scrollbar(context, scissor, scroll, maxScroll)
            if (tooltipText == null) {
                tooltipText = pages.firstNotNullOfOrNull { it.hoveredTooltip() }
            }
        }
        extraPages().firstNotNullOfOrNull { it.hoveredStack() }?.let { stack ->
            context.drawItemTooltip(textRenderer, stack, mouseX, mouseY)
        }
        if (isBuiltInVisual()) {
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
        UiWidgets.hoverCard(context, layout.card, lastMouseX, lastMouseY, key = title)
        drawCardHeader(context, layout.card, layout.toggle, layout.helper, title, expanded, toggleAnim, helperText, Hsv.toArgb(style))
        if (!expanded) {
            return
        }
        picker.render(context, style)
        drawSlider(context, layout.transparency, "Deckkraft", style.opacity, transparencyId)
        drawSlider(
            context,
            layout.width,
            "Konturbreite: ${style.thicknessPixels}px",
            (style.thicknessPixels - 1) / 3f,
            widthId
        )
        drawFilterButton(context, layout.filterButton, if (filter == null) "Spieler & Farben…" else "Items / Blöcke auswählen…")
    }

    private fun drawGlintCard(context: DrawContext) {
        val layout = glintCard
        val glint = ConfigManager.config.heldGlint
        val mode = glint.mode()
        UiWidgets.hoverCard(context, layout.card, lastMouseX, lastMouseY, key = "Hand-Glint")
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
        UiDraw.panel(
            context,
            rect,
            if (hovered) HugoTheme.accentSoft else HugoTheme.inset,
            if (hovered) HugoTheme.accent else HugoTheme.cardBorder
        )
        context.drawText(textRenderer, label, rect.x + 8, rect.y + (rect.h - 8) / 2, if (hovered) HugoTheme.text else HugoTheme.textMuted, false)
    }

    private fun drawCardHeader(
        context: DrawContext,
        card: UiRect,
        toggle: UiRect,
        helper: UiRect,
        title: String,
        expanded: Boolean,
        toggleAnim: Float,
        helperText: String,
        swatch: Int? = null
    ) {
        swatch?.let { color ->
            UiDraw.panel(context, UiRect(card.x + 10, card.y + 12, 14, 14), color, HugoTheme.cardBorder)
        }
        val textX = card.x + if (swatch != null) 30 else 12
        context.drawText(textRenderer, title, textX, card.y + 8, HugoTheme.text, false)
        context.drawText(
            textRenderer,
            UiDraw.ellipsize(textRenderer, helperText.substringBefore('\n'), card.w - 90),
            textX,
            card.y + 22,
            HugoTheme.textMuted,
            false
        )
        val badge = if (toggleAnim > 0.5f) "Aktiv" else "Aus"
        context.drawText(
            textRenderer,
            badge,
            textX,
            card.y + 36,
            if (toggleAnim > 0.5f) HugoTheme.success else HugoTheme.textDim,
            false
        )
        drawToggle(context, toggle, toggleAnim)
        val helperHovered = helper.contains(lastMouseX, lastMouseY)
        UiDraw.helperBadge(context, textRenderer, helper, helperHovered)
        if (helperHovered) {
            tooltipText = helperText
        }
    }

    private fun drawToggle(context: DrawContext, rect: UiRect, anim: Float) {
        UiWidgets.toggleProgress(context, rect, anim, lastMouseX, lastMouseY)
    }

    private fun drawSlider(context: DrawContext, rect: UiRect, label: String, value: Float, id: SliderId) {
        val hovered = rect.contains(lastMouseX, lastMouseY) || draggingSlider == id
        context.drawText(textRenderer, label, rect.x, rect.y, if (hovered) HugoTheme.text else HugoTheme.textMuted, false)
        val percent = "${(value * 100).roundToInt()}%"
        context.drawText(textRenderer, percent, rect.right() - textRenderer.getWidth(percent), rect.y, HugoTheme.text, false)
        val trackY = rect.y + 12
        UiWidgets.sliderTrack(context, UiRect(rect.x, trackY, rect.w, 6), value, hovered, key = id)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        PopupManager.active?.let {
            val (mx, my) = pointer(click)
            return it.mouseClicked(mx, my)
        }
        val (mx, my) = pointer(click)
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            for (page in extraPages()) {
                if (page.mouseClicked(mx, my, GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
                    return true
                }
            }
            return super.mouseClicked(click, doubled)
        }
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(click, doubled)
        }
        relayout()
        if (globalSearch.mouseClicked(mx, my)) {
            unfocusPickers()
            unfocusFilters()
            return true
        }
        globalSearch.unfocus()

        for ((entry, hit) in navHits) {
            if (hit.contains(mx, my)) {
                selectRail(entry)
                unfocusAll()
                return true
            }
        }
        for ((entry, hit) in subnavHits) {
            if (hit.contains(mx, my)) {
                if (entry.available) {
                    if (entry.id != selectedPageId) selectPage(entry.id)
                    else notifyShown()
                }
                unfocusAll()
                return true
            }
        }
        for ((entry, hit) in footerHits) {
            if (hit.contains(mx, my)) {
                if (entry.available && entry.id != selectedPageId) {
                    selectPage(entry.id)
                }
                unfocusAll()
                return true
            }
        }

        if (isBuiltInVisual() && scissor.contains(mx, my)) {
            if (category == ConfigCategory.DROPPED_GLOW && droppedCard.toggle.contains(mx, my)) {
                ConfigManager.update { it.droppedItemGlow.enabled = !it.droppedItemGlow.enabled }
                return true
            }
            if (category == ConfigCategory.HAND_GLINT && glintCard.toggle.contains(mx, my)) {
                ConfigManager.update { it.heldGlint.enabled = !it.heldGlint.enabled }
                return true
            }
            if (category == ConfigCategory.HAND_GLOW && heldGlowCard.toggle.contains(mx, my)) {
                ConfigManager.update { it.heldItemGlow.enabled = !it.heldItemGlow.enabled }
                return true
            }
            if (category == ConfigCategory.PLAYER_GLOW && playerGlowCard.toggle.contains(mx, my)) {
                ConfigManager.update { it.playerGlow.enabled = !it.playerGlow.enabled }
                return true
            }
            if (category == ConfigCategory.HAND_GLINT) {
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
            if (category == ConfigCategory.DROPPED_GLOW && droppedCard.filterButton.contains(mx, my)) {
                PopupManager.open(ItemFilterPopup("Dropped Item Glow", ConfigManager.config.droppedItemGlow.filter) { persist() })
                return true
            }
            if (category == ConfigCategory.HAND_GLOW && heldGlowCard.filterButton.contains(mx, my)) {
                PopupManager.open(ItemFilterPopup("Hand-Glow", ConfigManager.config.heldItemGlow.filter) { persist() })
                return true
            }
            if (category == ConfigCategory.PLAYER_GLOW && playerGlowCard.filterButton.contains(mx, my)) {
                PopupManager.open(
                    PlayerFilterPopup(
                        ConfigManager.config.playerGlow.playerFilter,
                        { ConfigManager.config.playerGlow }
                    ) { persist() }
                )
                return true
            }
            if (category == ConfigCategory.HAND_GLINT && glintCard.filterButton.contains(mx, my)) {
                PopupManager.open(ItemFilterPopup("Hand-Glint", ConfigManager.config.heldGlint.filter) { persist() })
                return true
            }
            if (category == ConfigCategory.DROPPED_GLOW && droppedPicker.mouseClicked(ConfigManager.config.droppedItemGlow, mx, my)) {
                heldGlowPicker.unfocus()
                playerGlowPicker.unfocus()
                glintPicker.unfocus()
                unfocusFilters()
                persist()
                return true
            }
            if (category == ConfigCategory.HAND_GLOW && heldGlowPicker.mouseClicked(ConfigManager.config.heldItemGlow, mx, my)) {
                droppedPicker.unfocus()
                playerGlowPicker.unfocus()
                glintPicker.unfocus()
                unfocusFilters()
                persist()
                return true
            }
            if (category == ConfigCategory.PLAYER_GLOW && playerGlowPicker.mouseClicked(ConfigManager.config.playerGlow, mx, my)) {
                droppedPicker.unfocus()
                heldGlowPicker.unfocus()
                glintPicker.unfocus()
                unfocusFilters()
                persist()
                return true
            }
            if (category == ConfigCategory.HAND_GLINT &&
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
                if (page.mouseClicked(mx, my, GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
                    persist()
                    relayout()
                    return true
                }
            }
        } else {
            for (page in extraPages()) {
                if (scissor.contains(mx, my) && page.mouseClicked(mx, my, GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
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
        PopupManager.active?.let {
            if (it.mouseDragged(mx, my)) return true
        }
        if (isBuiltInVisual()) {
            val pickerDragged =
                (category == ConfigCategory.DROPPED_GLOW &&
                    droppedPicker.mouseDragged(ConfigManager.config.droppedItemGlow, mx, my)) ||
                    (category == ConfigCategory.HAND_GLOW &&
                        heldGlowPicker.mouseDragged(ConfigManager.config.heldItemGlow, mx, my)) ||
                    (category == ConfigCategory.PLAYER_GLOW &&
                        playerGlowPicker.mouseDragged(ConfigManager.config.playerGlow, mx, my)) ||
                    (category == ConfigCategory.HAND_GLINT &&
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
        PopupManager.active?.mouseReleased()
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
        if (isBuiltInVisual()) {
            if (category == ConfigCategory.DROPPED_GLOW && droppedFilter.mouseScrolled(mx, my, verticalAmount)
            ) {
                return true
            }
            if (category == ConfigCategory.HAND_GLOW && heldGlowFilter.mouseScrolled(mx, my, verticalAmount)
            ) {
                return true
            }
            if (category == ConfigCategory.HAND_GLINT && glintFilter.mouseScrolled(mx, my, verticalAmount)
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
        if (globalSearch.keyPressed(input)) return true
        val extrasHandled = extraPages().any { it.keyPressed(input) }
        val handled = extrasHandled || if (isBuiltInVisual()) {
            (category == ConfigCategory.DROPPED_GLOW &&
                (droppedFilter.keyPressed(input) ||
                    droppedPicker.keyPressed(ConfigManager.config.droppedItemGlow, input))) ||
                (category == ConfigCategory.HAND_GLOW &&
                    (heldGlowFilter.keyPressed(input) ||
                        heldGlowPicker.keyPressed(ConfigManager.config.heldItemGlow, input))) ||
                (category == ConfigCategory.PLAYER_GLOW &&
                    playerGlowPicker.keyPressed(ConfigManager.config.playerGlow, input)) ||
                (category == ConfigCategory.HAND_GLINT &&
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
        if (globalSearch.charTyped(input)) return true
        val extrasHandled = extraPages().any { it.charTyped(input) }
        val handled = extrasHandled || if (isBuiltInVisual()) {
            (category == ConfigCategory.DROPPED_GLOW &&
                (droppedFilter.charTyped(input) ||
                    droppedPicker.charTyped(ConfigManager.config.droppedItemGlow, input))) ||
                (category == ConfigCategory.HAND_GLOW &&
                    (heldGlowFilter.charTyped(input) ||
                        heldGlowPicker.charTyped(ConfigManager.config.heldItemGlow, input))) ||
                (category == ConfigCategory.PLAYER_GLOW &&
                    playerGlowPicker.charTyped(ConfigManager.config.playerGlow, input)) ||
                (category == ConfigCategory.HAND_GLINT &&
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

    private fun extraPages(): List<dev.henny.hugoutils.ui.UiPage> =
        ConfigPages.forId(selectedPageId ?: ConfigCategory.MARKET_HOME.id)

    private fun landingPageId(): String {
        val requested = initialPageId ?: selectedPageId
        if (!requested.isNullOrBlank() && pageAvailable(requested)) return requested
        val config = ConfigManager.config
        if (config.restoreLastPage && pageAvailable(config.lastOpenedPage)) return config.lastOpenedPage
        return ConfigCategory.MARKET_HOME.id
    }

    private fun pageAvailable(id: String): Boolean {
        val entry = UiNavigation.registry.entry(id)
        if (entry != null) return entry.available
        return ConfigCategory.entries.any { it.id == id && it.available }
    }

    fun onFlagsChanged() {
        ConfigPages.refreshAvailability()
        val current = selectedPageId
        if (current != null && !pageAvailable(current)) {
            val parent = category.parentId ?: category.id
            val fallback = UiNavigation.registry.children(parent).firstOrNull { it.available }
                ?: UiNavigation.registry.roots().firstOrNull { it.available }
            if (fallback != null) selectPage(fallback.id) else relayout()
        } else {
            relayout()
        }
    }

    private fun selectRail(entry: NavigationEntry) {
        val children = UiNavigation.registry.children(entry.id)
        if (children.isNotEmpty()) {
            val alreadyInside = selectedPageId != null &&
                (children.any { it.id == selectedPageId } ||
                    UiNavigation.registry.descendants(entry.id).any { it.id == selectedPageId })
            if (!alreadyInside) {
                val parentCat = ConfigCategory.entries.firstOrNull { it.id == entry.id }
                val remembered = parentCat?.let { rememberedChild(it, children) }
                val first = remembered ?: children.firstOrNull { it.available } ?: children.firstOrNull() ?: return
                selectPage(first.id)
            }
            return
        }
        if (entry.available && entry.id != selectedPageId) {
            selectPage(entry.id)
        }
    }

    private fun selectPage(id: String) {
        ConfigCategory.entries.firstOrNull { it.id == id }?.let { category = it }
        selectedPageId = id
        ConfigManager.config.lastOpenedPage = id
        if (category.group == NavGroup.SETTINGS) {
            ConfigManager.config.lastSettingsPage = id
        }
        ConfigManager.requestSave()
        animatePageChange()
        scroll = 0
        relayout()
        notifyShown()
    }

    private fun notifyShown() {
        extraPages().filterIsInstance<ConfigPage>().forEach { it.onShown() }
    }

    private fun rememberedChild(
        parent: ConfigCategory,
        children: List<NavigationEntry>
    ): NavigationEntry? {
        val config = ConfigManager.config
        if (config.restoreLastPage) {
            children.firstOrNull { it.id == config.lastOpenedPage && it.available }?.let { return it }
        }
        if (parent != ConfigCategory.SETTINGS && parent.group != NavGroup.SETTINGS) return null
        val remembered = config.lastSettingsPage
        return children.firstOrNull { it.id == remembered && it.available }
    }

    private fun isBuiltInVisual(): Boolean =
        selectedPageId in BUILT_IN_VISUALS.map { it.id }

    private fun titleHit(card: UiRect, toggle: UiRect, helper: UiRect, mx: Double, my: Double): Boolean {
        return mx >= card.x && mx < helper.x - 2 &&
            my >= card.y && my < card.y + HEADER_H &&
            !toggle.contains(mx, my)
    }

    private fun sliderEnabled(id: SliderId): Boolean = when (id) {
        SliderId.DROPPED_TRANSPARENCY, SliderId.DROPPED_WIDTH -> expandedDropped
        SliderId.DROPPED_INTENSITY -> false
        SliderId.GLINT_TRANSPARENCY, SliderId.GLINT_SPEED -> expandedGlint
        SliderId.HELD_TRANSPARENCY, SliderId.HELD_WIDTH -> expandedHeldGlow
        SliderId.HELD_INTENSITY -> false
        SliderId.PLAYER_TRANSPARENCY, SliderId.PLAYER_WIDTH -> expandedPlayerGlow
        SliderId.PLAYER_INTENSITY -> false
    }

    private fun hoveredContentStack(): ItemStack? =
        extraPages().firstNotNullOfOrNull { it.hoveredStack() } ?: hoveredVisualStack()

    private fun hoveredVisualStack() = when (category) {
        ConfigCategory.DROPPED_GLOW -> droppedFilter.hoveredStack
        ConfigCategory.HAND_GLOW -> heldGlowFilter.hoveredStack
        ConfigCategory.HAND_GLINT -> glintFilter.hoveredStack
        else -> extraPages().firstNotNullOfOrNull { it.hoveredStack() }
    }

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
            SliderId.DROPPED_WIDTH -> config.droppedItemGlow.thicknessPixels = (1 + value * 3f).roundToInt().coerceIn(1, 4)
            SliderId.DROPPED_INTENSITY -> Unit
            SliderId.GLINT_TRANSPARENCY -> config.heldGlint.transparency = 1f - value
            SliderId.GLINT_SPEED -> config.heldGlint.speed = value
            SliderId.HELD_TRANSPARENCY -> config.heldItemGlow.opacity = value
            SliderId.HELD_INTENSITY -> Unit
            SliderId.HELD_WIDTH -> config.heldItemGlow.thicknessPixels = (1 + value * 3f).roundToInt().coerceIn(1, 4)
            SliderId.PLAYER_TRANSPARENCY -> config.playerGlow.opacity = value
            SliderId.PLAYER_WIDTH -> config.playerGlow.thicknessPixels = (1 + value * 3f).roundToInt().coerceIn(1, 4)
            SliderId.PLAYER_INTENSITY -> Unit
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
        globalSearch.unfocus()
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
        fun open(client: MinecraftClient, pageId: String? = null, parent: Screen? = client.currentScreen) {
            val origin = if (parent is HugoScreen) parent.parent else parent
            client.setScreen(HugoScreen(pageId, origin))
        }

        fun toggle(client: MinecraftClient) {
            when (val current = client.currentScreen) {
                is HugoScreen -> client.setScreen(current.parent)
                is TitleScreen, is GameMenuScreen, null -> open(client, parent = current)
                else -> Unit
            }
        }

        private const val HEADER_H = 54
        private val ALL_SLIDERS = SliderId.entries.toSet()
        private val BUILT_IN_VISUALS = setOf(
            ConfigCategory.DROPPED_GLOW,
            ConfigCategory.HAND_GLINT,
            ConfigCategory.HAND_GLOW,
            ConfigCategory.PLAYER_GLOW
        )

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
                "- Spieler auswählen: Filter (alle / nur ausgewählte / außer) und " +
                "pro Spieler eine eigene Glow-Farbe (Farbfeld am Eintrag).\n" +
                "Farbe, Transparenz, Kontur-Stärke und Kontur-Breite sind getrennt einstellbar."
    }
}
