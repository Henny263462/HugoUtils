package dev.henny.hugoutils.client.ui

import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.EffectsConfig
import dev.henny.hugoutils.client.config.ParticleTypeOverride
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.registry.Registries
import dev.henny.hugoutils.ui.ButtonStyle
import kotlin.math.roundToInt

class EffectsConfigPage : ConfigPage {
    override val category = ConfigCategory.EFFECTS

    private val client = MinecraftClient.getInstance()
    private val particleIds by lazy { Registries.PARTICLE_TYPE.ids.map { it.toString() }.sorted() }
    private var selectedIndex = 0
    private var frame = UiRect(0, 0, 0, 0)
    private var hero = UiRect(0, 0, 0, 0)
    private var global = UiRect(0, 0, 0, 0)
    private var overrideCard = UiRect(0, 0, 0, 0)
    private var particlesToggle = UiRect(0, 0, 0, 0)
    private var previousType = UiRect(0, 0, 0, 0)
    private var nextType = UiRect(0, 0, 0, 0)
    private var searchType = UiRect(0, 0, 0, 0)
    private var customToggle = UiRect(0, 0, 0, 0)
    private var typeToggle = UiRect(0, 0, 0, 0)
    private var resetButton = UiRect(0, 0, 0, 0)
    private val sliders = linkedMapOf<SliderId, UiRect>()
    private var dragging: SliderId? = null
    private var mouseX = 0.0
    private var mouseY = 0.0

    override fun layout(x: Int, y: Int, width: Int, height: Int): Int {
        sliders.clear()
        hero = UiRect(x + 8, y + 8, width - 16, 58)
        particlesToggle = UiRect(hero.right() - 42, hero.y + 14, 32, 14)
        global = UiRect(x + 8, hero.bottom() + 8, width - 16, 28 + GLOBAL.size * SLIDER_H + 12)
        placeSliders(GLOBAL, global.x + 12, global.y + 28, global.w - 24)
        val typeTop = global.bottom() + 8
        val typeH = 28 + 26 + 2 * 24 + TYPE.size * SLIDER_H + 36
        overrideCard = UiRect(x + 8, typeTop, width - 16, typeH)
        previousType = UiRect(overrideCard.x + 12, overrideCard.y + 28, 24, 22)
        nextType = UiRect(overrideCard.right() - 36, overrideCard.y + 28, 24, 22)
        searchType = UiRect(previousType.right() + 6, overrideCard.y + 28, nextType.x - previousType.right() - 12, 22)
        customToggle = UiRect(overrideCard.right() - 44, searchType.bottom() + 10, 32, 14)
        typeToggle = UiRect(overrideCard.right() - 44, customToggle.bottom() + 10, 32, 14)
        placeSliders(TYPE, overrideCard.x + 12, typeToggle.bottom() + 10, overrideCard.w - 24)
        val lastType = sliders[SliderId.TYPE_OPACITY] ?: typeToggle
        resetButton = UiRect(overrideCard.x + 12, lastType.bottom() + 10, overrideCard.w - 24, 20)
        frame = UiRect(x, y, width, overrideCard.bottom() + 8 - y)
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        this.mouseX = mouseX.toDouble()
        this.mouseY = mouseY.toDouble()
        val font = client.textRenderer
        ModPageChrome.hero(
            context, font, hero,
            "Effekte",
            "Partikel, Feuer, Portal und Übelkeit unabhängig von Vanilla.",
            EffectsConfig.particlesEnabled, mouseX.toDouble(), mouseY.toDouble(), particlesToggle
        )
        UiWidgets.hoverCard(context, global, mouseX.toDouble(), mouseY.toDouble(), key = "effects-global")
        context.drawText(font, "Welt & Bildschirm", global.x + 12, global.y + 10, HugoTheme.text, false)
        drawSlider(context, SliderId.DENSITY, "Partikeldichte", EffectsConfig.particleDensity)
        drawSlider(context, SliderId.SIZE, "Partikelgröße", EffectsConfig.particleSize / 3f)
        drawSlider(context, SliderId.OPACITY, "Partikeldeckkraft", EffectsConfig.particleOpacity)
        drawSlider(context, SliderId.FIRE, "Feuer", EffectsConfig.fireScale)
        drawSlider(context, SliderId.PORTAL, "Portal", EffectsConfig.portalScale)
        drawSlider(context, SliderId.NAUSEA, "Übelkeit", EffectsConfig.nauseaScale)
        drawSlider(context, SliderId.WOBBLE, "Nausea-Wobble", EffectsConfig.nauseaWobbleScale)

        UiWidgets.hoverCard(context, overrideCard, mouseX.toDouble(), mouseY.toDouble(), key = "effects-type")
        context.drawText(font, "Pro Partikeltyp", overrideCard.x + 12, overrideCard.y + 10, HugoTheme.text, false)
        drawButton(context, previousType, "‹")
        drawButton(context, nextType, "›")
        val typeId = selectedTypeId()
        drawButton(context, searchType, UiDraw.ellipsize(font, typeId, searchType.w - 16))
        val override = EffectsConfig.particleOverrides[typeId]
        context.drawText(font, "Eigene Werte", overrideCard.x + 12, customToggle.y + 3, HugoTheme.text, false)
        drawToggle(context, customToggle, override != null)
        context.drawText(
            font,
            "Typ aktivieren",
            overrideCard.x + 12,
            typeToggle.y + 3,
            if (override != null) HugoTheme.text else HugoTheme.textDim,
            false
        )
        drawToggle(context, typeToggle, override?.enabled ?: true, override != null)
        drawSlider(context, SliderId.TYPE_DENSITY, "Dichte", override?.density ?: 1f, override != null)
        drawSlider(context, SliderId.TYPE_SIZE, "Größe", (override?.size ?: 1f) / 3f, override != null)
        drawSlider(context, SliderId.TYPE_OPACITY, "Deckkraft", override?.opacity ?: 1f, override != null)
        drawButton(context, resetButton, "Vanilla-Standard wiederherstellen")
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        when {
            particlesToggle.contains(mouseX, mouseY) -> EffectsConfig.particlesEnabled = !EffectsConfig.particlesEnabled
            previousType.contains(mouseX, mouseY) -> selectOffset(-1)
            nextType.contains(mouseX, mouseY) -> selectOffset(1)
            searchType.contains(mouseX, mouseY) -> openParticleSearch()
            customToggle.contains(mouseX, mouseY) -> toggleOverride()
            typeToggle.contains(mouseX, mouseY) -> EffectsConfig.particleOverrides[selectedTypeId()]?.let { it.enabled = !it.enabled }
            resetButton.contains(mouseX, mouseY) -> EffectsConfig.reset()
            else -> {
                val hit = sliders.entries.firstOrNull { it.value.contains(mouseX, mouseY) }
                if (hit != null && sliderEnabled(hit.key)) {
                    dragging = hit.key
                    applySlider(hit.key, mouseX)
                } else {
                    return frame.contains(mouseX, mouseY)
                }
            }
        }
        persist()
        return true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        val id = dragging ?: return false
        applySlider(id, mouseX)
        persist()
        return true
    }

    override fun mouseReleased() {
        dragging = null
    }

    override fun resetUi() {
        dragging = null
    }

    override fun persist() = ConfigManager.requestSave()

    private fun selectedTypeId(): String = particleIds.getOrElse(selectedIndex) { "minecraft:unknown" }

    private fun selectOffset(offset: Int) {
        if (particleIds.isNotEmpty()) selectedIndex = (selectedIndex + offset).mod(particleIds.size)
    }

    private fun openParticleSearch() {
        PopupManager.open(ParticleTypePopup(particleIds, selectedTypeId()) { id ->
            selectedIndex = particleIds.indexOf(id).takeIf { it >= 0 } ?: selectedIndex
        })
    }

    private fun toggleOverride() {
        val id = selectedTypeId()
        if (EffectsConfig.particleOverrides.remove(id) == null) {
            EffectsConfig.particleOverrides[id] = ParticleTypeOverride()
        }
    }

    private fun sliderEnabled(id: SliderId): Boolean =
        !id.name.startsWith("TYPE_") || EffectsConfig.particleOverrides.containsKey(selectedTypeId())

    private fun applySlider(id: SliderId, mouseX: Double) {
        val rect = sliders[id] ?: return
        val track = UiRect(rect.x, rect.y + 12, rect.w, 6)
        val normalized = ((mouseX - track.x) / track.w.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        val override = EffectsConfig.particleOverrides[selectedTypeId()]
        when (id) {
            SliderId.DENSITY -> EffectsConfig.particleDensity = normalized
            SliderId.SIZE -> EffectsConfig.particleSize = normalized * 3f
            SliderId.OPACITY -> EffectsConfig.particleOpacity = normalized
            SliderId.FIRE -> EffectsConfig.fireScale = normalized
            SliderId.PORTAL -> EffectsConfig.portalScale = normalized
            SliderId.NAUSEA -> EffectsConfig.nauseaScale = normalized
            SliderId.WOBBLE -> EffectsConfig.nauseaWobbleScale = normalized
            SliderId.TYPE_DENSITY -> override?.density = normalized
            SliderId.TYPE_SIZE -> override?.size = normalized * 3f
            SliderId.TYPE_OPACITY -> override?.opacity = normalized
        }
        EffectsConfig.clamp()
    }

    private fun drawSlider(context: DrawContext, id: SliderId, label: String, normalized: Float, enabled: Boolean = true) {
        val rect = sliders.getValue(id)
        val value = when (id) {
            SliderId.SIZE, SliderId.TYPE_SIZE -> "%.2f×".format(normalized * 3f)
            else -> "${(normalized * 100).roundToInt()}%"
        }
        UiWidgets.labeledSlider(
            context,
            client.textRenderer,
            rect,
            label,
            value,
            normalized,
            mouseX,
            mouseY,
            enabled,
            id
        )
    }

    private fun drawToggle(context: DrawContext, rect: UiRect, enabled: Boolean, interactive: Boolean = true) {
        UiWidgets.toggle(context, rect, enabled, mouseX, mouseY, interactive)
    }

    private fun drawButton(context: DrawContext, rect: UiRect, label: String) {
        UiWidgets.button(context, client.textRenderer, rect, label, mouseX, mouseY, style = ButtonStyle.SECONDARY)
    }

    private fun placeSliders(ids: List<SliderId>, x: Int, y: Int, w: Int) {
        ids.forEachIndexed { index, id ->
            sliders[id] = UiRect(x, y + index * SLIDER_H, w, 22)
        }
    }

    private enum class SliderId {
        DENSITY, SIZE, OPACITY, FIRE, PORTAL, NAUSEA, WOBBLE,
        TYPE_DENSITY, TYPE_SIZE, TYPE_OPACITY
    }

    companion object {
        private const val SLIDER_H = 28
        private val GLOBAL = listOf(
            SliderId.DENSITY, SliderId.SIZE, SliderId.OPACITY,
            SliderId.FIRE, SliderId.PORTAL, SliderId.NAUSEA, SliderId.WOBBLE
        )
        private val TYPE = listOf(SliderId.TYPE_DENSITY, SliderId.TYPE_SIZE, SliderId.TYPE_OPACITY)
    }
}
