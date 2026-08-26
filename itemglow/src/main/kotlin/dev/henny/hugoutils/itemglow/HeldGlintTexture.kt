package dev.henny.hugoutils.itemglow

import dev.henny.hugoutils.HugoIds
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.GlintStyle
import dev.henny.hugoutils.client.config.GlowStyle
import dev.henny.hugoutils.client.config.GlintChangeListener
import dev.henny.hugoutils.mixin.RenderLayerInvoker
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.OutputTarget
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderSetup
import net.minecraft.client.render.TextureTransform
import net.minecraft.client.render.item.ItemRenderer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.BufferAllocator
import net.minecraft.util.Util
import net.minecraft.util.math.ColorHelper
import org.joml.Matrix4f
import java.util.SequencedMap
import kotlin.math.max

/**
 * Vanilla-compatible colored item glint. The texture is recolored dynamically, while Minecraft's
 * own GLINT pipeline and item vertex consumer keep depth, UV projection and command ordering exact.
 */
object HeldGlintTexture {
    private val textureId = HugoIds.id("dynamic/held_glint")
    private var texture: NativeImageBackedTexture? = null
    private var sourcePixels: IntArray? = null
    private var imageWidth = 0
    private var imageHeight = 0
    private var lastColor = Int.MIN_VALUE
    private var lastRainbowUpload = 0L

    init {
        GlintChangeListener.register { invalidateColor() }
    }

    private val texturing = TextureTransform("hugoutils_held_glint") {
        val speed = ConfigManager.config.heldGlint.speedMultiplier()
        val time = (Util.getMeasuringTimeMs().toDouble() * speed * 8.0).toLong()
        val u = (time % 110000L) / 110000.0f
        val v = (time % 30000L) / 30000.0f
        Matrix4f().translation(-u, v, 0f).rotateZ(0.17453292f).scale(8f)
    }

    private val renderLayer: RenderLayer = RenderLayerInvoker.`hugoutils$create`(
        "hugoutils_colored_glint",
        RenderSetup.builder(RenderPipelines.GLINT)
            .texture("Sampler0", textureId)
            .textureTransform(texturing)
            .build()
    )

    private val translucentRenderLayer: RenderLayer = RenderLayerInvoker.`hugoutils$create`(
        "hugoutils_colored_glint_translucent",
        RenderSetup.builder(RenderPipelines.GLINT)
            .texture("Sampler0", textureId)
            .textureTransform(texturing)
            .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .build()
    )

    fun initialize() = Unit

    /** Forces the next held-item draw to upload the freshly selected UI color. */
    fun invalidateColor() {
        lastColor = Int.MIN_VALUE
    }

    /** Keeps both custom layers buffered until the normal entity-layer flush, after item depth exists. */
    @JvmStatic
    fun registerBuffers(buffers: SequencedMap<RenderLayer, BufferAllocator>): SequencedMap<RenderLayer, BufferAllocator> {
        buffers.putIfAbsent(renderLayer, BufferAllocator(renderLayer.expectedBufferSize))
        buffers.putIfAbsent(translucentRenderLayer, BufferAllocator(translucentRenderLayer.expectedBufferSize))
        return buffers
    }

    fun layer(style: GlintStyle, translucent: Boolean): RenderLayer? {
        if (!ensureTexture()) return null
        updateColor(style)
        return if (translucent) translucentRenderLayer else renderLayer
    }

    fun color(style: GlintStyle): Int {
        val rgb = if (style.mode() == GlintStyle.Mode.RAINBOW) {
            val hue = ((Util.getMeasuringTimeMs().toDouble() * style.speedMultiplier() / 6000.0) % 1.0).toFloat()
            val c = GlowStyle.hsvToRgb(hue, 0.85f, 1f)
            ColorHelper.getArgb(255, c[0], c[1], c[2])
        } else {
            style.rgbInt()
        }
        return ColorHelper.withAlpha((style.glintAlpha() * 255f).toInt().coerceIn(0, 255), rgb)
    }

    private fun ensureTexture(): Boolean {
        if (texture != null) return true
        return try {
            val client = MinecraftClient.getInstance()
            client.resourceManager.open(ItemRenderer.ITEM_ENCHANTMENT_GLINT).use { input ->
                NativeImage.read(input).use { source ->
                    imageWidth = source.width
                    imageHeight = source.height
                    sourcePixels = source.copyPixelsArgb()
                    val target = NativeImage(imageWidth, imageHeight, false)
                    texture = NativeImageBackedTexture({ "HugoUtils colored held-item glint" }, target).also {
                        client.textureManager.registerTexture(textureId, it)
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun updateColor(style: GlintStyle) {
        val now = Util.getMeasuringTimeMs()
        if (style.mode() == GlintStyle.Mode.RAINBOW && now - lastRainbowUpload < 40L) return
        val tint = color(style)
        if (tint == lastColor) return

        val pixels = sourcePixels ?: return
        val target = texture?.image ?: return
        val tintR = ColorHelper.getRed(tint) / 255f
        val tintG = ColorHelper.getGreen(tint) / 255f
        val tintB = ColorHelper.getBlue(tint) / 255f
        val strength = ColorHelper.getAlpha(tint) / 255f
        for (index in pixels.indices) {
            val source = pixels[index]
            val level = max(ColorHelper.getRed(source), max(ColorHelper.getGreen(source), ColorHelper.getBlue(source))) / 255f
            val red = (tintR * level * strength * 255f).toInt().coerceIn(0, 255)
            val green = (tintG * level * strength * 255f).toInt().coerceIn(0, 255)
            val blue = (tintB * level * strength * 255f).toInt().coerceIn(0, 255)
            val alpha = ColorHelper.getAlpha(source)
            target.setColorArgb(index % imageWidth, index / imageWidth, ColorHelper.getArgb(alpha, red, green, blue))
        }
        texture?.upload()
        lastColor = tint
        lastRainbowUpload = now
    }
}
