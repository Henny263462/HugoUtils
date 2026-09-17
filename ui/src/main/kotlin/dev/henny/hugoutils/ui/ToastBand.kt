package dev.henny.hugoutils.ui

/**
 * Conveyor of HUD toasts: at most [capacity] are on screen. New toasts wait
 * off-screen and enter one at a time as the band shifts.
 */
class ToastBand(
    val capacity: Int = 10,
    shiftDurationSeconds: Float = 0.32f
) {
    data class Slot(
        val toast: Toast,
        var remaining: Float,
        val visibility: AnimatedFloat = AnimatedFloat(1f, .18f)
    )

    private val waiting = ArrayDeque<Toast>()
    private val slots = ArrayDeque<Slot>()
    var incoming: Slot? = null
        private set
    var outgoing: Slot? = null
        private set
    val shift = AnimatedFloat(0f, shiftDurationSeconds, Easing.EASE_OUT)

    val displayed: List<Slot> get() = slots.toList()
    val waitingCount: Int get() = waiting.size
    val toasts: List<Toast>
        get() = buildList {
            incoming?.let { add(it.toast) }
            addAll(slots.map { it.toast })
        }

    fun enqueue(toast: Toast) {
        while (waiting.size >= MAX_WAITING) waiting.removeFirst()
        waiting.addLast(toast)
    }

    fun update(deltaSeconds: Float) {
        val dt = deltaSeconds.coerceAtLeast(0f)
        slots.forEach { tick(it, dt) }
        incoming?.let { tick(it, dt) }
        outgoing?.let { it.visibility.update(dt) }
        shift.update(dt)
        if (shift.running) return
        commitShift()
        pruneFinished()
        startShiftIfNeeded()
    }

    private fun tick(slot: Slot, dt: Float) {
        slot.remaining -= dt
        if (slot.remaining <= .2f) slot.visibility.animateTo(0f)
        slot.visibility.update(dt)
    }

    private fun commitShift() {
        incoming?.let { slots.addFirst(it) }
        incoming = null
        outgoing = null
        while (slots.size > capacity) slots.removeLast()
        shift.snapTo(0f)
    }

    private fun pruneFinished() {
        slots.removeAll { it.remaining <= 0f && !it.visibility.running }
    }

    private fun startShiftIfNeeded() {
        if (waiting.isEmpty()) return
        val toast = waiting.removeFirst()
        incoming = Slot(
            toast,
            toast.durationSeconds,
            AnimatedFloat(0f, .18f).apply { animateTo(1f) }
        )
        if (slots.size >= capacity) {
            val leaving = slots.removeLast()
            leaving.visibility.animateTo(0f)
            outgoing = leaving
        }
        shift.snapTo(0f)
        shift.animateTo(1f)
    }

    companion object {
        private const val MAX_WAITING = 50
    }
}
