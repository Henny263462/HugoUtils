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
    }
}

class Animation(
    initialValue: Float = 0f,
    var durationSeconds: Float = .2f,
    var easing: Easing = Easing.EASE_OUT
) {
    var value: Float = initialValue
        private set
    var target: Float = initialValue
        private set
    private var start = initialValue
    private var elapsed = durationSeconds

    val running: Boolean get() = elapsed < durationSeconds

    fun animateTo(newTarget: Float) {
        if (newTarget == target) return
        start = value
        target = newTarget
        elapsed = 0f
    }

    fun snapTo(newValue: Float) {
        start = newValue
        target = newValue
        value = newValue
        elapsed = durationSeconds
    }

    fun update(deltaSeconds: Float): Float {
        if (!running) return value
        elapsed = (elapsed + deltaSeconds.coerceAtLeast(0f)).coerceAtMost(durationSeconds)
        val progress = if (durationSeconds <= 0f) 1f else elapsed / durationSeconds
        value = start + (target - start) * easing.transform(progress)
        return value
    }
}
