package dev.henny.hugoutils.ui

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
