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
        frame = UiRect(x, y, width, 426)
        particlesToggle = UiRect(x + width - 42, y + 34, 32, 14)
        sliders.clear()
        SliderId.entries.take(7).forEachIndexed { index, id ->
            sliders[id] = UiRect(x + width - 150, y + 62 + index * 25, 140, 12)
        }
        previousType = UiRect(x + 10, y + 252, 24, 20)
        nextType = UiRect(x + width - 34, y + 252, 24, 20)
        searchType = UiRect(previousType.right() + 5, y + 252, width - 78, 20)
        customToggle = UiRect(x + width - 42, y + 281, 32, 14)
        typeToggle = UiRect(x + width - 42, y + 306, 32, 14)
        SliderId.entries.drop(7).forEachIndexed { index, id ->
            sliders[id] = UiRect(x + width - 150, y + 333 + index * 25, 140, 12)
        }
        resetButton = UiRect(x + 10, y + 401, width - 20, 18)
        return frame.h
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int) {
        this.mouseX = mouseX.toDouble()
        this.mouseY = mouseY.toDouble()
        val font = client.textRenderer
        UiDraw.panel(context, frame, HugoTheme.card, HugoTheme.cardBorder)
        context.drawText(font, "Partikel- und Bildschirm-Effekte", frame.x + 10, frame.y + 12, HugoTheme.text, false)
        context.drawText(font, "Partikel aktivieren", frame.x + 10, frame.y + 37, HugoTheme.text, false)
        drawToggle(context, particlesToggle, EffectsConfig.particlesEnabled)

        drawSlider(context, SliderId.DENSITY, "Partikeldichte", EffectsConfig.particleDensity)
        drawSlider(context, SliderId.SIZE, "Partikelgröße", EffectsConfig.particleSize / 3f)
        drawSlider(context, SliderId.OPACITY, "Partikeldeckkraft", EffectsConfig.particleOpacity)
        drawSlider(context, SliderId.FIRE, "Feuer", EffectsConfig.fireScale)
        drawSlider(context, SliderId.PORTAL, "Portal", EffectsConfig.portalScale)
        drawSlider(context, SliderId.NAUSEA, "Übelkeit", EffectsConfig.nauseaScale)
        drawSlider(context, SliderId.WOBBLE, "Nausea-Wobble", EffectsConfig.nauseaWobbleScale)

        context.drawText(font, "Override pro Partikeltyp", frame.x + 10, frame.y + 232, HugoTheme.text, false)
        drawButton(context, previousType, "‹")
        drawButton(context, nextType, "›")
        val typeId = selectedTypeId()
        drawButton(context, searchType, "⌕  $typeId")
        val override = EffectsConfig.particleOverrides[typeId]
        context.drawText(font, "Eigene Werte", frame.x + 10, frame.y + 284, HugoTheme.text, false)
        drawToggle(context, customToggle, override != null)
        context.drawText(font, "Typ aktivieren", frame.x + 10, frame.y + 309, if (override != null) HugoTheme.text else HugoTheme.textDim, false)
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
        val normalized = ((mouseX - rect.x) / rect.w).toFloat().coerceIn(0f, 1f)
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
        val font = client.textRenderer
        val color = if (enabled) HugoTheme.text else HugoTheme.textDim
        context.drawText(font, label, frame.x + 10, rect.y + 2, color, false)
        UiWidgets.sliderTrack(context, rect, normalized, rect.contains(mouseX, mouseY), enabled, id)
        val value = when (id) {
            SliderId.SIZE, SliderId.TYPE_SIZE -> "%.2f×".format(normalized * 3f)
            else -> "${(normalized * 100).roundToInt()}%"
        }
        context.drawText(font, value, rect.right() - font.getWidth(value) - 3, rect.y + 2, color, false)
    }

    private fun drawToggle(context: DrawContext, rect: UiRect, enabled: Boolean, interactive: Boolean = true) {
        UiWidgets.toggle(context, rect, enabled, mouseX, mouseY, interactive)
    }

    private fun drawButton(context: DrawContext, rect: UiRect, label: String) {
        UiWidgets.button(context, client.textRenderer, rect, label, mouseX, mouseY, style = ButtonStyle.SECONDARY)
    }

    private enum class SliderId {
        DENSITY, SIZE, OPACITY, FIRE, PORTAL, NAUSEA, WOBBLE,
        TYPE_DENSITY, TYPE_SIZE, TYPE_OPACITY
    }
}
