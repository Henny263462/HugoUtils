package dev.henny.hugoutils.ui

object SliderMath {
    const val TRACK_HEIGHT = 6
    const val HIT_PAD_Y = 8

    fun normalized(track: UiRect, mouseX: Double): Float =
        ((mouseX - track.x) / track.w.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)

    fun hit(rect: UiRect): UiRect = rect.inflate(0, HIT_PAD_Y)

    fun contains(rect: UiRect, mouseX: Double, mouseY: Double): Boolean =
        hit(rect).contains(mouseX, mouseY)

    fun trackIn(bounds: UiRect): UiRect {
        val height = TRACK_HEIGHT.coerceAtMost(bounds.h.coerceAtLeast(1))
        return UiRect(bounds.x, bounds.y + (bounds.h - height) / 2, bounds.w, height)
    }

    fun labeledTrack(bounds: UiRect): UiRect =
        UiRect(bounds.x, bounds.y + 12, bounds.w, TRACK_HEIGHT)
}
