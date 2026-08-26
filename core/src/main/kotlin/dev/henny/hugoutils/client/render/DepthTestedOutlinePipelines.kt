package dev.henny.hugoutils.client.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import dev.henny.hugoutils.HugoIds
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.VertexFormats

object DepthTestedOutlinePipelines {
    @JvmField
    val CULL: RenderPipeline = create("outline_cull", true)

    @JvmField
    val NO_CULL: RenderPipeline = create("outline_no_cull", false)

    private fun create(name: String, cull: Boolean): RenderPipeline =
        RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(HugoIds.id("pipeline/$name"))
                .withVertexShader(HugoIds.id("core/depth_outline"))
                .withFragmentShader(HugoIds.id("core/depth_outline"))
                .withSampler("Sampler0")
                .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
                .withDepthWrite(false)
                .withDepthBias(-1f, -10f)
                .withCull(cull)
                .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                .build()
        )
}
