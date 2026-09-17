package dev.henny.hugoutils.blockoverlay

import com.google.gson.JsonObject
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.ConfigSection
import dev.henny.hugoutils.client.config.GlowStyle
import dev.henny.hugoutils.client.config.HsvColor
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.ColorHelper
import java.util.concurrent.ConcurrentHashMap

object BlockOverlayConfig : ConfigSection {
    override val id = "blockOverlay"
    @Volatile
    var enabled = true
    private val styles = ConcurrentHashMap<String, BlockOverlayStyle>()
    @Volatile
    private var reloadAfter = 0L

    val entries: Map<String, BlockOverlayStyle> get() = styles

    override fun read(json: JsonObject) {
        enabled = json.get("enabled")?.asBoolean ?: true
        styles.clear()
        val blocks = json.getAsJsonObject("blocks")
        if (blocks != null) {
            readBlocks(blocks)
        } else {
            readBlocks(ConfigManager.moduleData("blockHighlight")?.getAsJsonObject("blocks"))
        }
    }

    override fun write(): JsonObject = JsonObject().apply {
        addProperty("enabled", enabled)
        add("blocks", JsonObject().also { blocks ->
            styles.forEach { (blockId, style) ->
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
        styles.putIfAbsent(key, BlockOverlayStyle())
        return key
    }

    fun remove(id: String?) {
        if (id != null) styles.remove(id)
    }

    fun style(id: String): BlockOverlayStyle? = styles[id]

    @JvmStatic
    fun styleFor(state: BlockState): BlockOverlayStyle? {
        if (!enabled || styles.isEmpty()) return null
        val blockId = Registries.BLOCK.getId(state.block).toString()
        styles[blockId]?.let { return it }
        if (!state.fluidState.isEmpty) {
            val fluidBlock = Registries.BLOCK.getId(state.fluidState.blockState.block).toString()
            styles[fluidBlock]?.let { return it }
        }
        return null
    }

    fun keys(): List<String> = styles.keys.sorted()

    fun normalizeId(rawId: String): Identifier? {
        val id = Identifier.tryParse(rawId.trim().lowercase()) ?: return null
        return id.takeIf { Registries.BLOCK.containsId(it) }
    }

    fun notifyWorld() {
        ConfigManager.requestSave()
        reloadAfter = System.currentTimeMillis() + RELOAD_DEBOUNCE_MS
    }

    fun flushWorldReload(client: MinecraftClient) {
        val deadline = reloadAfter
        if (deadline == 0L || System.currentTimeMillis() < deadline) return
        reloadAfter = 0L
        if (client.world != null) {
            client.worldRenderer.reload()
            client.worldRenderer.scheduleTerrainUpdate()
        }
    }

    private fun readBlocks(blocks: JsonObject?) {
        blocks?.entrySet()?.forEach { (rawId, value) ->
            if (!value.isJsonObject || normalizeId(rawId) == null) return@forEach
            val styleJson = value.asJsonObject
            styles[rawId] = BlockOverlayStyle().apply {
                hue = styleJson.get("hue")?.asFloat ?: hue
                saturation = styleJson.get("saturation")?.asFloat ?: saturation
                brightness = styleJson.get("brightness")?.asFloat ?: brightness
                opacity = styleJson.get("opacity")?.asFloat ?: opacity
                clamp()
            }
        }
    }

    private const val RELOAD_DEBOUNCE_MS = 200L
}

class BlockOverlayStyle : HsvColor {
    override var hue = 0.55f
    override var saturation = 0.8f
    override var brightness = 1f
    var opacity = 0.6f

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
