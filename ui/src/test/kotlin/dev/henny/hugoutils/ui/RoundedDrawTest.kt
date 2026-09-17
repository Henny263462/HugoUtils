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
            UiMetrics.corner = 8
            assertEquals(8, UiMetrics.CORNER)
            assertEquals(4, UiMetrics.CORNER_SM)
            assertEquals(10, UiMetrics.CORNER_LG)
        } finally {
            UiMetrics.corner = previous
        }
    }
}
