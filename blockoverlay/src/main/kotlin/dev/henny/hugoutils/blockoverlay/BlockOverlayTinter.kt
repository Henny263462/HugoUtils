package dev.henny.hugoutils.blockoverlay

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.minecraft.block.BlockState
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.ColorHelper
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object BlockOverlayTinter {
    @JvmStatic
    fun wrap(consumer: VertexConsumer, state: BlockState): VertexConsumer {
        if (consumer is OverlayTintConsumer) return consumer
        val style = BlockOverlayConfig.styleFor(state) ?: return consumer
        if (style.opacity <= 0.001f) return consumer
        return OverlayTintConsumer(consumer, style.argb())
    }

    @JvmStatic
    fun tintQuad(quad: MutableQuadView, state: BlockState) {
        val style = BlockOverlayConfig.styleFor(state) ?: return
        if (style.opacity <= 0.001f) return
        val tint = style.argb()
        val amount = ColorHelper.getAlpha(tint) / 255f
        val tintR = ColorHelper.getRed(tint)
        val tintG = ColorHelper.getGreen(tint)
        val tintB = ColorHelper.getBlue(tint)
        for (vertex in 0 until 4) {
            val color = quad.color(vertex)
            quad.color(
                vertex,
                ColorHelper.getArgb(
                    ColorHelper.getAlpha(color),
                    OverlayTintConsumer.mixChannel(ColorHelper.getRed(color), tintR, amount),
                    OverlayTintConsumer.mixChannel(ColorHelper.getGreen(color), tintG, amount),
                    OverlayTintConsumer.mixChannel(ColorHelper.getBlue(color), tintB, amount)
                )
            )
        }
    }

    @JvmStatic
    fun tintSodiumQuad(quad: Any?, state: BlockState) {
        if (quad == null) return
        val style = BlockOverlayConfig.styleFor(state) ?: return
        if (style.opacity <= 0.001f) return
        val getter = colorGetter(quad.javaClass) ?: return
        val setter = colorSetter(quad.javaClass) ?: return
        val tint = style.argb()
        for (vertex in 0 until 4) {
            val current = getter.invoke(quad, vertex) as Int
            setter.invoke(quad, vertex, OverlayTintConsumer.mixPacked(current, tint))
        }
    }

    @JvmStatic
    fun tintAbgrColors(colors: IntArray, state: BlockState) {
        val style = BlockOverlayConfig.styleFor(state) ?: return
        if (style.opacity <= 0.001f) return
        val tint = style.argb()
        for (index in colors.indices) {
            colors[index] = OverlayTintConsumer.mixAbgr(colors[index], tint)
        }
    }

    private fun colorGetter(type: Class<*>): Method? {
        getters[type]?.let { return it }
        val found = findMethod(type, listOf("baseColor", "getColor"), INTEGER) ?: return null
        getters.putIfAbsent(type, found)
        return found
    }

    private fun colorSetter(type: Class<*>): Method? {
        setters[type]?.let { return it }
        val found = findMethod(type, listOf("setColor"), INTEGER, INTEGER) ?: return null
        setters.putIfAbsent(type, found)
        return found
    }

    private fun findMethod(type: Class<*>, names: List<String>, vararg args: Class<*>): Method? {
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            for (name in names) {
                val method = runCatching { current.getMethod(name, *args) }.getOrNull()
                    ?: runCatching { current.getDeclaredMethod(name, *args) }.getOrNull()
                if (method != null) {
                    method.isAccessible = true
                    return method
                }
            }
            current = current.superclass
        }
        return null
    }

    private val getters = ConcurrentHashMap<Class<*>, Method>()
    private val setters = ConcurrentHashMap<Class<*>, Method>()
    private val INTEGER = Integer.TYPE
}

internal class OverlayTintConsumer(
    private val inner: VertexConsumer,
    tint: Int
) : VertexConsumer {
    private val tintR = ColorHelper.getRed(tint)
    private val tintG = ColorHelper.getGreen(tint)
    private val tintB = ColorHelper.getBlue(tint)
    private val amount = ColorHelper.getAlpha(tint) / 255f

    override fun vertex(x: Float, y: Float, z: Float): VertexConsumer {
        inner.vertex(x, y, z)
        return this
    }

    override fun vertex(
        x: Float,
        y: Float,
        z: Float,
        color: Int,
        u: Float,
        v: Float,
        overlay: Int,
        light: Int,
        normalX: Float,
        normalY: Float,
        normalZ: Float
    ) {
        inner.vertex(x, y, z, mixPacked(color), u, v, overlay, light, normalX, normalY, normalZ)
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
        inner.color(mix(red, tintR), mix(green, tintG), mix(blue, tintB), alpha)
        return this
    }

    override fun color(argb: Int): VertexConsumer = color(
        ColorHelper.getRed(argb),
        ColorHelper.getGreen(argb),
        ColorHelper.getBlue(argb),
        ColorHelper.getAlpha(argb)
    )

    override fun color(red: Float, green: Float, blue: Float, alpha: Float): VertexConsumer =
        color(
            (red * 255f).toInt().coerceIn(0, 255),
            (green * 255f).toInt().coerceIn(0, 255),
            (blue * 255f).toInt().coerceIn(0, 255),
            (alpha * 255f).toInt().coerceIn(0, 255)
        )

    override fun texture(u: Float, v: Float): VertexConsumer {
        inner.texture(u, v)
        return this
    }

    override fun overlay(u: Int, v: Int): VertexConsumer {
        inner.overlay(u, v)
        return this
    }

    override fun light(u: Int, v: Int): VertexConsumer {
        inner.light(u, v)
        return this
    }

    override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
        inner.normal(x, y, z)
        return this
    }

    override fun lineWidth(width: Float): VertexConsumer {
        inner.lineWidth(width)
        return this
    }

    override fun quad(
        matrixEntry: MatrixStack.Entry,
        quad: BakedQuad,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float,
        light: Int,
        overlay: Int
    ) {
        inner.quad(matrixEntry, quad, mixF(red, tintR), mixF(green, tintG), mixF(blue, tintB), alpha, light, overlay)
    }

    override fun quad(
        matrixEntry: MatrixStack.Entry,
        quad: BakedQuad,
        brightnesses: FloatArray,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float,
        lights: IntArray,
        overlay: Int
    ) {
        inner.quad(
            matrixEntry,
            quad,
            brightnesses,
            mixF(red, tintR),
            mixF(green, tintG),
            mixF(blue, tintB),
            alpha,
            lights,
            overlay
        )
    }

    private fun mixPacked(color: Int): Int = ColorHelper.getArgb(
        ColorHelper.getAlpha(color),
        mix(ColorHelper.getRed(color), tintR),
        mix(ColorHelper.getGreen(color), tintG),
        mix(ColorHelper.getBlue(color), tintB)
    )

    private fun mix(source: Int, tint: Int): Int = mixChannel(source, tint, amount)

    private fun mixF(source: Float, tint: Int): Float =
        mixChannel((source * 255f).toInt().coerceIn(0, 255), tint, amount) / 255f

    companion object {
        fun mixChannel(source: Int, tint: Int, amount: Float): Int {
            val multiplied = source + (tint - source) * amount
            return multiplied.toInt().coerceIn(0, 255)
        }

        fun mixPacked(color: Int, tint: Int): Int {
            val amount = ColorHelper.getAlpha(tint) / 255f
            return ColorHelper.getArgb(
                ColorHelper.getAlpha(color),
                mixChannel(ColorHelper.getRed(color), ColorHelper.getRed(tint), amount),
                mixChannel(ColorHelper.getGreen(color), ColorHelper.getGreen(tint), amount),
                mixChannel(ColorHelper.getBlue(color), ColorHelper.getBlue(tint), amount)
            )
        }

        fun mixAbgr(color: Int, tint: Int): Int = swapRb(mixPacked(swapRb(color), tint))

        private fun swapRb(color: Int): Int =
            (color and 0xFF00FF00.toInt()) or ((color ushr 16) and 0xFF) or ((color and 0xFF) shl 16)
    }
}
