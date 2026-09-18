package dev.henny.hugoutils.ui

import kotlin.math.min

data class CornerRadii(val tl: Int, val tr: Int, val br: Int, val bl: Int) {
    fun clamp(width: Int, height: Int): CornerRadii {
        val max = min(width, height) / 2
        return CornerRadii(
            tl.coerceIn(0, max),
            tr.coerceIn(0, max),
            br.coerceIn(0, max),
            bl.coerceIn(0, max)
        )
    }

    val max: Int get() = maxOf(tl, tr, br, bl)
    val isZero: Boolean get() = tl <= 0 && tr <= 0 && br <= 0 && bl <= 0

    companion object {
        val NONE = CornerRadii(0, 0, 0, 0)
        fun all(radius: Int) = CornerRadii(radius, radius, radius, radius)
        fun left(radius: Int) = CornerRadii(radius, 0, 0, radius)
        fun right(radius: Int) = CornerRadii(0, radius, radius, 0)
        fun top(radius: Int) = CornerRadii(radius, radius, 0, 0)
        fun bottom(radius: Int) = CornerRadii(0, 0, radius, radius)
    }
}
