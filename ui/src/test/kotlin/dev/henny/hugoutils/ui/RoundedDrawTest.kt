package dev.henny.hugoutils.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoundedDrawTest {
    @Test
    fun `corner radius never exceeds half the smallest side`() {
        assertEquals(2, UiDraw.cornerRadius(10, 4, 8))
        assertEquals(3, UiDraw.cornerRadius(20, 8, 3))
        assertEquals(6, UiDraw.cornerRadius(40, 12, 20))
        assertEquals(0, UiDraw.cornerRadius(0, 10, 4))
    }

    @Test
    fun `circle span stays inside the requested radius`() {
        val radius = 8
        for (row in 0 until radius) {
            val span = UiDraw.circleSpan(radius, row)
            assertTrue(span in 1..radius, "row $row span $span")
        }
        assertEquals(0, UiDraw.circleSpan(0, 0))
        assertEquals(0, UiDraw.circleSpan(6, -1))
        assertEquals(0, UiDraw.circleSpan(6, 6))
    }

    @Test
    fun `corner metrics follow the configured radius`() {
        val previous = UiMetrics.corner
        try {
            UiMetrics.corner = 0
            assertEquals(0, UiMetrics.CORNER)
            assertEquals(0, UiMetrics.CORNER_SM)
            assertEquals(0, UiMetrics.radiusFor(14))
            assertEquals(0, UiMetrics.radiusSmFor(8))
            UiMetrics.corner = 8
            assertEquals(8, UiMetrics.CORNER)
            assertEquals(4, UiMetrics.CORNER_SM)
            assertEquals(14, UiMetrics.CORNER_LG)
            assertEquals(7, UiMetrics.radiusFor(14))
            assertEquals(4, UiMetrics.radiusSmFor(8))
            UiMetrics.corner = UiMetrics.CORNER_MAX
            assertEquals(UiMetrics.CORNER_MAX, UiMetrics.CORNER)
            assertEquals(7, UiMetrics.radiusFor(14))
            UiMetrics.corner = 99
            assertEquals(UiMetrics.CORNER_MAX, UiMetrics.CORNER)
        } finally {
            UiMetrics.corner = previous
        }
    }

    @Test
    fun `coverage is a smooth quarter circle`() {
        val radius = 12
        assertEquals(0f, Rounded.coverage(radius, -1, 0))
        val outer = Rounded.coverage(radius, 0, 0)
        val inner = Rounded.coverage(radius, radius - 1, radius - 1)
        assertTrue(outer < 0.2f, "outer $outer")
        assertEquals(1f, inner, 0.0001f)
        var previous = -1f
        for (col in 0 until radius) {
            val cover = Rounded.coverage(radius, col, 0)
            assertTrue(cover in 0f..1f, "col $col cover $cover")
            assertTrue(cover + 0.0001f >= previous, "coverage should grow toward the interior")
            previous = cover
        }
    }

    @Test
    fun `independent corner radii clamp per side`() {
        val radii = CornerRadii(20, 2, 0, 8).clamp(10, 10)
        assertEquals(5, radii.tl)
        assertEquals(2, radii.tr)
        assertEquals(0, radii.br)
        assertEquals(5, radii.bl)
        assertTrue(CornerRadii.left(6).tr == 0)
        assertTrue(CornerRadii.all(4).max == 4)
    }
}
