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

    val running: Boolean get() = elapsed < durationSeconds

    fun animateTo(newTarget: Float) {
        if (abs(newTarget - target) < 0.0001f) return
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
