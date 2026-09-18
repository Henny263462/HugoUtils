package dev.henny.hugoutils.itemglow

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import dev.henny.hugoutils.HugoIds
import dev.henny.hugoutils.client.config.GlowStyle
import dev.henny.hugoutils.mixin.RenderLayerAccessor
import dev.henny.hugoutils.mixin.RenderLayerInvoker
import dev.henny.hugoutils.mixin.RenderSetupAccessor
import dev.henny.hugoutils.mixin.RenderSetupTextureSpecAccessor
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderSetup
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ItemGlowRenderLayer {
    private val directions = Array(12) { index ->
        val angle = index * (PI * 2.0 / 12.0)
        cos(angle).toFloat() to sin(angle).toFloat()
    }

    private val pipelines = Array(GlowStyle.MAX_HELD_THICKNESS) {
        arrayOfNulls<RenderPipeline>(directions.size)
    }

    private data class LayerKey(val texture: Identifier, val direction: Int, val thickness: Int)

    private val layers = object : LinkedHashMap<LayerKey, RenderLayer>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LayerKey, RenderLayer>): Boolean =
            size > MAX_CACHED_LAYERS
    }

    fun initialize() {
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    fun directionCount(): Int = directions.size

    fun forItemLayer(baseLayer: RenderLayer?, direction: Int, thicknessPixels: Int): RenderLayer? {
        baseLayer ?: return null
        val setup = (baseLayer as RenderLayerAccessor).`hugoutils$getRenderSetup`()
        val textureSpec = (setup as RenderSetupAccessor).`hugoutils$getTextures`()["Sampler0"] ?: return null
        val textureId = (textureSpec as RenderSetupTextureSpecAccessor).`hugoutils$getLocation`()
        val thickness = thicknessPixels.coerceIn(1, GlowStyle.MAX_HELD_THICKNESS)
        val key = LayerKey(textureId, direction, thickness)
        return layers.getOrPut(key) {
            RenderLayerInvoker.`hugoutils$create`(
                "hugoutils_item_glow_${thickness}_${direction}_${textureId.namespace}_${textureId.path.replace('/', '_')}",
                RenderSetup.builder(pipeline(thickness, direction))
                    .texture("Sampler0", textureId)
                    .translucent()
                    .outlineMode(RenderSetup.OutlineMode.NONE)
                    .build()
            )
        }
    }

    private fun pipeline(thickness: Int, direction: Int): RenderPipeline {
        val cached = pipelines[thickness - 1][direction]
        if (cached != null) return cached
        val (x, y) = directions[direction]
        val created = RenderPipelines.register(
            RenderPipeline.builder(
                RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET,
                RenderPipelines.FOG_SNIPPET
            )
                .withLocation(HugoIds.id("pipeline/item_glow_${thickness}_$direction"))
                .withVertexShader(HugoIds.id("core/item_glow"))
                .withFragmentShader(HugoIds.id("core/item_glow"))
                .withShaderDefine("OUTLINE_X", x * thickness)
                .withShaderDefine("OUTLINE_Y", y * thickness)
                .withSampler("Sampler0")
                .withDepthWrite(false)
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
                .withDepthBias(1f, 10f)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS)
                .build()
        )
        pipelines[thickness - 1][direction] = created
        return created
    }

    private const val MAX_CACHED_LAYERS = 384
}
