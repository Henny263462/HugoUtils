package dev.henny.hugoutils.blockhighlight

import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.render.state.WorldRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.registry.Registries
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random

object BlockHighlightRenderer {
    @JvmStatic
    fun submitTarget(matrices: MatrixStack, renderStates: WorldRenderState, queue: OrderedRenderCommandQueue) {
        val client = MinecraftClient.getInstance()
        val world = client.world ?: return
        val hit = client.crosshairTarget
        if (hit !is BlockHitResult || hit.type != HitResult.Type.BLOCK) return
        val state = world.getBlockState(hit.blockPos)
        if (state.isAir) return
        val blockId = Registries.BLOCK.getId(state.block).toString()
        val style = BlockHighlightConfig.styleFor(blockId) ?: return
        if (style.opacity <= 0f) return

        val camera = renderStates.cameraRenderState.pos
        val model = client.blockRenderManager.getModel(state)
        val random = Random.create(state.getRenderingSeed(hit.blockPos))
        val parts = model.getParts(random)
        if (parts.isEmpty()) return
        val color = style.argb()

        matrices.push()
        matrices.translate(
            hit.blockPos.x - camera.x,
            hit.blockPos.y - camera.y,
            hit.blockPos.z - camera.z
        )
        val modelOffset = state.getModelOffset(hit.blockPos)
        matrices.translate(modelOffset.x, modelOffset.y, modelOffset.z)
        queue.submitCustom(matrices, BlockHighlightRenderLayer.layer) { entry, consumer ->
            for (part in parts) {
                emitQuads(consumer, entry, part.getQuads(null), color)
                for (direction in Direction.entries) {
                    emitQuads(consumer, entry, part.getQuads(direction), color)
                }
            }
        }
        matrices.pop()
    }

    private fun emitQuads(
        consumer: VertexConsumer,
        entry: MatrixStack.Entry,
        quads: List<BakedQuad>,
        color: Int
    ) {
        for (quad in quads) {
            val normal = quad.face().floatVector
            for (index in 0 until 4) {
                val pos = quad.getPosition(index)
                val uv = quad.getTexcoords(index)
                consumer.vertex(entry, pos.x(), pos.y(), pos.z())
                    .color(color)
                    .texture(Float.fromBits((uv ushr 32).toInt()), Float.fromBits(uv.toInt()))
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                    .normal(entry, normal.x(), normal.y(), normal.z())
            }
        }
    }
}
