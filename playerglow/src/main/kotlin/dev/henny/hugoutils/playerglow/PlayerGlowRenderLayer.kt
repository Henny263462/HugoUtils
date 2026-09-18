package dev.henny.hugoutils.playerglow

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import dev.henny.hugoutils.HugoIds
import dev.henny.hugoutils.mixin.RenderLayerInvoker
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderSetup
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier

object PlayerGlowRenderLayer {
    private val pipeline: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(
            RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET,
            RenderPipelines.FOG_SNIPPET
        )
            .withLocation(HugoIds.id("pipeline/player_glow"))
            .withVertexShader(HugoIds.id("core/player_glow"))
            .withFragmentShader(HugoIds.id("core/player_glow"))
            .withSampler("Sampler0")
            .withDepthWrite(false)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS)
            .build()
    )

    private val layers = object : LinkedHashMap<Identifier, RenderLayer>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Identifier, RenderLayer>): Boolean =
            size > MAX_CACHED_LAYERS
    }

    fun initialize() = Unit

    @Synchronized
    fun forTexture(texture: Identifier): RenderLayer = layers.getOrPut(texture) {
        RenderLayerInvoker.`hugoutils$create`(
            "hugoutils_player_glow_${texture.namespace}_${texture.path.replace('/', '_')}",
            RenderSetup.builder(pipeline)
                .texture("Sampler0", texture)
                .translucent()
                .build()
        )
    }

    private const val MAX_CACHED_LAYERS = 64
}
