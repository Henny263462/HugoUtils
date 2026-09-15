package dev.henny.hugoutils.blockhighlight

import com.google.gson.JsonObject
import dev.henny.hugoutils.client.config.ConfigSection
import dev.henny.hugoutils.client.config.GlowStyle
import dev.henny.hugoutils.client.config.HsvColor
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.ColorHelper

object BlockHighlightConfig : ConfigSection {
    override val id = "blockHighlight"
    var enabled = true
    val entries: LinkedHashMap<String, BlockHighlightStyle> = linkedMapOf()

    override fun read(json: JsonObject) {
        enabled = json.get("enabled")?.asBoolean ?: true
        entries.clear()
        json.getAsJsonObject("blocks")?.entrySet()?.forEach { (rawId, value) ->
            if (!value.isJsonObject || normalizeId(rawId) == null) return@forEach
            val styleJson = value.asJsonObject
            entries[rawId] = BlockHighlightStyle().apply {
                hue = styleJson.get("hue")?.asFloat ?: hue
                saturation = styleJson.get("saturation")?.asFloat ?: saturation
                brightness = styleJson.get("brightness")?.asFloat ?: brightness
                opacity = styleJson.get("opacity")?.asFloat ?: opacity
                clamp()
            }
        }
    }

    override fun write(): JsonObject = JsonObject().apply {
        addProperty("enabled", enabled)
        add("blocks", JsonObject().also { blocks ->
            entries.forEach { (blockId, style) ->
                style.clamp()
                blocks.add(blockId, JsonObject().apply {
                    addProperty("hue", style.hue)
                    addProperty("saturation", style.saturation)
                    addProperty("brightness", style.brightness)
                    addProperty("opacity", style.opacity)
                })
            }
        })
    }

    fun add(rawId: String): String? {
        val id = normalizeId(rawId) ?: return null
        val key = id.toString()
        entries.putIfAbsent(key, BlockHighlightStyle())
        return key
    }

    fun styleFor(rawId: String): BlockHighlightStyle? =
        if (enabled) entries[rawId] else null

    fun normalizeId(rawId: String): Identifier? {
        val id = Identifier.tryParse(rawId.trim().lowercase()) ?: return null
        return id.takeIf { Registries.BLOCK.containsId(it) }
    }
}

class BlockHighlightStyle : HsvColor {
    override var hue = 0.55f
    override var saturation = 0.8f
    override var brightness = 1f
    var opacity = 0.35f

    fun clamp() {
        hue = GlowStyle.wrapHue(hue)
        saturation = saturation.coerceIn(0f, 1f)
        brightness = brightness.coerceIn(0f, 1f)
        opacity = opacity.coerceIn(0f, 1f)
    }

    fun argb(): Int {
        val rgb = GlowStyle.hsvToRgb(hue, saturation, brightness)
        return ColorHelper.getArgb((opacity * 255f).toInt(), rgb[0], rgb[1], rgb[2])
    }

    override fun hex(): String {
        val rgb = GlowStyle.hsvToRgb(hue, saturation, brightness)
        return "%02X%02X%02X".format(rgb[0], rgb[1], rgb[2])
    }

    override fun setFromHex(hex: String): Boolean {
        val cleaned = hex.removePrefix("#").trim()
        if (cleaned.length != 6 || cleaned.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) return false
        val packed = cleaned.toInt(16)
        val hsv = GlowStyle.rgbToHsv((packed shr 16) and 255, (packed shr 8) and 255, packed and 255)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        return true
    }
}
