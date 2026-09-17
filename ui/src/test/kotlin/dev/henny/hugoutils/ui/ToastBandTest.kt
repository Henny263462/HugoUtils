package dev.henny.hugoutils.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ToastBandTest {
    @Test
    fun `new toasts wait off the band until a shift starts`() {
        val band = ToastBand(capacity = 10, shiftDurationSeconds = .3f)
        band.enqueue(Toast("a", durationSeconds = 5f))
        assertTrue(band.displayed.isEmpty())
        assertNull(band.incoming)
        assertEquals(1, band.waitingCount)

        band.update(0f)
        assertEquals("a", band.incoming?.toast?.message)
        assertTrue(band.displayed.isEmpty())
        assertEquals(0, band.waitingCount)
    }

    @Test
    fun `a toast joins the visible list only after the band finishes moving`() {
        val band = ToastBand(capacity = 10, shiftDurationSeconds = .3f)
        band.enqueue(Toast("a", durationSeconds = 5f))
        band.update(0f)
        band.update(.15f)
        assertEquals("a", band.incoming?.toast?.message)
        assertTrue(band.displayed.isEmpty())

        band.update(.15f)
        assertNull(band.incoming)
        assertEquals(listOf("a"), band.displayed.map { it.toast.message })
    }

    @Test
    fun `only the last ten toasts stay on the band`() {
        val band = ToastBand(capacity = 10, shiftDurationSeconds = .1f)
        repeat(15) { index -> band.enqueue(Toast("t$index", durationSeconds = 30f)) }
        repeat(20) { band.update(.1f) }

        assertEquals(10, band.displayed.size)
        assertEquals(0, band.waitingCount)
        assertNull(band.incoming)
        assertEquals((14 downTo 5).map { "t$it" }, band.displayed.map { it.toast.message })
    }

    @Test
    fun `queued toasts enter one shift at a time`() {
        val band = ToastBand(capacity = 10, shiftDurationSeconds = .4f)
        band.enqueue(Toast("first", durationSeconds = 8f))
        band.update(0f)
        band.enqueue(Toast("second", durationSeconds = 8f))
        band.update(.01f)

        assertEquals("first", band.incoming?.toast?.message)
        assertEquals(1, band.waitingCount)
        assertTrue(band.displayed.isEmpty())
    }

    @Test
    fun `event bursts cannot grow the waiting queue forever`() {
        val band = ToastBand()
        repeat(500) { band.enqueue(Toast("t$it")) }
        assertEquals(50, band.waitingCount)
    }
}
