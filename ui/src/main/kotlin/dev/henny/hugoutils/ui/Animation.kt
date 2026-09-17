package dev.henny.hugoutils.ui

import kotlin.math.pow
import kotlin.math.abs

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

class AnimatedFloat(
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
    private var delayRemaining = 0f

    val running: Boolean get() = delayRemaining > 0f || elapsed < durationSeconds

    fun animateTo(newTarget: Float, delaySeconds: Float = 0f) {
        if (abs(newTarget - target) < 0.0001f && delayRemaining <= 0f && delaySeconds <= 0f) return
        start = value
        target = newTarget
        elapsed = 0f
        delayRemaining = delaySeconds.coerceAtLeast(0f)
    }

    fun snapTo(newValue: Float) {
        start = newValue
        target = newValue
        value = newValue
        elapsed = durationSeconds
        delayRemaining = 0f
    }

    fun update(deltaSeconds: Float): Float {
        var dt = deltaSeconds.coerceAtLeast(0f)
        if (delayRemaining > 0f) {
            if (dt < delayRemaining) {
                delayRemaining -= dt
                return value
            }
            dt -= delayRemaining
            delayRemaining = 0f
        }
        if (elapsed >= durationSeconds) return value
        elapsed = (elapsed + dt).coerceAtMost(durationSeconds)
        val progress = if (durationSeconds <= 0f) 1f else elapsed / durationSeconds
        value = start + (target - start) * easing.transform(progress)
        return value
    }
}

typealias Animation = AnimatedFloat

/**
 * Shared real-time frame clock. Screens call [beginFrame] once before drawing.
 * Delta is clamped so controls never jump after window drags or low-FPS stalls.
 */
object UiFrame {
    const val MAX_DELTA_SECONDS = 0.05f
    private var lastNanos = 0L
    var deltaSeconds: Float = 1f / 60f
        private set
    var elapsedSeconds: Float = 0f
        private set

    fun beginFrame(nowNanos: Long = System.nanoTime()): Float {
        deltaSeconds = if (lastNanos == 0L) {
            1f / 60f
        } else {
            ((nowNanos - lastNanos) / 1_000_000_000.0).toFloat().coerceIn(0f, MAX_DELTA_SECONDS)
        }
        lastNanos = nowNanos
        elapsedSeconds += deltaSeconds
        return deltaSeconds
    }

    fun reset() {
        lastNanos = 0L
        deltaSeconds = 1f / 60f
        elapsedSeconds = 0f
    }
}

class InteractionState {
    val hover = AnimatedFloat(0f, .12f)
    val press = AnimatedFloat(0f, .08f, Easing.EASE_OUT)
    val focus = AnimatedFloat(0f, .14f)

    fun update(hovered: Boolean, pressed: Boolean = false, focused: Boolean = false) {
        hover.animateTo(if (hovered) 1f else 0f)
        press.animateTo(if (pressed) 1f else 0f)
        focus.animateTo(if (focused) 1f else 0f)
        val dt = UiFrame.deltaSeconds
        hover.update(dt)
        press.update(dt)
        focus.update(dt)
    }

    fun pulse() {
        press.snapTo(1f)
        press.animateTo(0f)
    }
}
