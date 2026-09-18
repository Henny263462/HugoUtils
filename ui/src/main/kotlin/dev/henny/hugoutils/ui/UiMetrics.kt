package dev.henny.hugoutils.ui

object UiMetrics {
    const val SPACE_1 = 4
    const val SPACE_2 = 8
    const val SPACE_3 = 12
    const val SPACE_4 = 16
    const val CONTROL_HEIGHT = 22
    const val COMPACT_HEIGHT = 18
    const val CARD_PADDING = 12
    const val PANEL_MIN_WIDTH = 380
    const val PANEL_MAX_WIDTH = 980
    const val PANEL_MIN_HEIGHT = 260
    const val PANEL_MAX_HEIGHT = 560
    const val CORNER_MAX = 24
    var corner: Int = 0
        set(value) { field = value.coerceIn(0, CORNER_MAX) }
    val CORNER get() = corner
    val CORNER_SM get() = if (corner <= 0) 0 else (corner / 2).coerceAtLeast(1)
    val CORNER_LG get() = (corner + 6).coerceAtMost(CORNER_MAX + 8)

    fun radiusFor(size: Int): Int = CORNER.coerceAtMost((size / 2).coerceAtLeast(0))
    fun radiusSmFor(size: Int): Int = CORNER_SM.coerceAtMost((size / 2).coerceAtLeast(0))
    const val RAIL_WIDTH = 92
    const val SUBNAV_WIDTH = 112
}
