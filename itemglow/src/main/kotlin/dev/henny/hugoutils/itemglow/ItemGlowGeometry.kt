package dev.henny.hugoutils.itemglow

import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector2f
import org.joml.Vector3fc

object ItemGlowGeometry {
    fun bounds(quads: List<BakedQuad>): FloatArray? {
        if (quads.isEmpty()) {
            return null
        }
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (quad in quads) {
            for (index in 0 until 4) {
                val pos: Vector3fc = quad.getPosition(index)
                minX = minOf(minX, pos.x())
                minY = minOf(minY, pos.y())
                minZ = minOf(minZ, pos.z())
                maxX = maxOf(maxX, pos.x())
                maxY = maxOf(maxY, pos.y())
                maxZ = maxOf(maxZ, pos.z())
            }
        }
        return floatArrayOf(minX, minY, minZ, maxX, maxY, maxZ)
    }

    /** Emits the complete model geometry; depth removes the center after screen-space expansion. */
    fun emit(
        consumer: VertexConsumer,
        entry: MatrixStack.Entry,
        quad: BakedQuad,
        color: Int,
        light: Int,
        overlay: Int
    ) {
        val faceNormal = quad.face().floatVector
        val nx = faceNormal.x()
        val ny = faceNormal.y()
        val nz = faceNormal.z()
        for (index in 0 until 4) {
            val pos = quad.getPosition(index)
            val packedUv = quad.getTexcoords(index)
            consumer.vertex(entry, pos.x(), pos.y(), pos.z())
                .color(color)
                .texture(Vector2f.getX(packedUv), Vector2f.getY(packedUv))
                .overlay(overlay)
                .light(light)
                .normal(entry, nx, ny, nz)
        }
    }
}
