package dev.henny.hugoutils.blockhighlight

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
import net.minecraft.client.texture.SpriteAtlasTexture

object BlockHighlightRenderLayer {
    private val pipeline: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(
            RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET,
            RenderPipelines.FOG_SNIPPET
        )
            .withLocation(HugoIds.id("pipeline/block_highlight"))
            .withVertexShader(HugoIds.id("core/block_highlight"))
            .withFragmentShader(HugoIds.id("core/block_highlight"))
            .withSampler("Sampler0")
            .withDepthWrite(false)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
            .withDepthBias(-1f, -10f)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS)
            .build()
    )

    val layer: RenderLayer = RenderLayerInvoker.`hugoutils$create`(
        "hugoutils_block_highlight",
        RenderSetup.builder(pipeline)
            .texture("Sampler0", SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
            .translucent()
            .build()
    )

    fun initialize() = Unit
}
