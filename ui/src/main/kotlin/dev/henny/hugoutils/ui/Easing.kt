package dev.henny.hugoutils.ui

import kotlin.math.pow

fun interface Easing {
    fun transform(progress: Float): Float

    companion object {
        @JvmField val LINEAR = Easing { it.coerceIn(0f, 1f) }
        @JvmField val EASE_IN = Easing { it.coerceIn(0f, 1f).pow(3) }
        @JvmField val EASE_OUT = Easing { 1f - (1f - it.coerceIn(0f, 1f)).pow(3) }
        @JvmField val EASE_IN_OUT = Easing {
            val t = it.coerceIn(0f, 1f)
            if (t < .5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
        }
        @JvmField val EASE_OUT_BACK = Easing { t ->
            val x = t.coerceIn(0f, 1f)
            val overshoot = 1.70158f
            1f + (overshoot + 1f) * (x - 1f).pow(3) + overshoot * (x - 1f).pow(2)
        }
        @JvmField val EASE_OUT_BOUNCE = Easing(::bounceOut)

        private fun bounceOut(progress: Float): Float {
            val x = progress.coerceIn(0f, 1f)
            val n1 = 7.5625f
            val d1 = 2.75f
            return when {
                x < 1f / d1 -> n1 * x * x
                x < 2f / d1 -> n1 * (x - 1.5f / d1) * (x - 1.5f / d1) + 0.75f
                x < 2.5f / d1 -> n1 * (x - 2.25f / d1) * (x - 2.25f / d1) + 0.9375f
                else -> n1 * (x - 2.625f / d1) * (x - 2.625f / d1) + 0.984375f
            }
        }
    }
}
