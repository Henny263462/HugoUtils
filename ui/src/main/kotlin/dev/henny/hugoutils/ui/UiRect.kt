package dev.henny.hugoutils.ui

data class UiRect(val x: Int, val y: Int, val w: Int, val h: Int) {
    val right: Int get() = x + w
    val bottom: Int get() = y + h
    fun right(): Int = right
    fun bottom(): Int = bottom
    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseX < right && mouseY >= y && mouseY < bottom
    fun inflate(dx: Int, dy: Int): UiRect =
        UiRect(x - dx, y - dy, (w + dx * 2).coerceAtLeast(1), (h + dy * 2).coerceAtLeast(1))
    fun scaleFromCenter(scale: Float): UiRect {
        val width = (w * scale).toInt().coerceAtLeast(1)
        val height = (h * scale).toInt().coerceAtLeast(1)
        return UiRect(x + (w - width) / 2, y + (h - height) / 2, width, height)
    }
}
