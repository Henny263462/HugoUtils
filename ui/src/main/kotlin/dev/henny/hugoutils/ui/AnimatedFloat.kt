package dev.henny.hugoutils.ui

import kotlin.math.abs

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
