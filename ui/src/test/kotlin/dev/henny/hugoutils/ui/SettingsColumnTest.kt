package dev.henny.hugoutils.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsColumnTest {
    @Test
    fun `column stacks finished setting blocks`() {
        val toggle = SettingToggle("t", "Titel", "Text", read = { true }, write = {})
        val corners = SettingCorners("c", read = { 8 }, write = {})
        val column = SettingsColumn().add(toggle).add(corners)
        val height = column.layout(10, 20, 240)
        assertEquals(10, toggle.bounds.x)
        assertEquals(20, toggle.bounds.y)
        assertEquals(240, toggle.bounds.w)
        assertEquals(SettingToggle.HEIGHT, toggle.bounds.h)
        assertEquals(toggle.bounds.bottom + 10, corners.bounds.y)
        assertEquals(SettingToggle.HEIGHT + 10 + SettingCorners.HEIGHT, height)
    }

    @Test
    fun `slider writes a normalized value`() {
        var stored = 0f
        val slider = SettingSlider("s", "Rundung", readNormalized = { stored }, writeNormalized = { stored = it }, valueText = { stored.toString() })
        slider.layout(0, 0, 100)
        slider.mouseClicked(50.0, slider.bounds.y + 40.0)
        assertEquals(0.5f, stored, 0.02f)
        slider.mouseDragged(88.0, slider.bounds.y + 40.0)
        assertEquals(1f, stored, 0.02f)
        slider.mouseReleased()
        slider.mouseDragged(12.0, slider.bounds.y + 40.0)
        assertEquals(1f, stored, 0.02f)
    }

    @Test
    fun `corner slider follows drag after the click`() {
        var radius = 0
        val corners = SettingCorners("c", read = { radius }, write = { radius = it })
        corners.layout(0, 0, 240)
        val y = 106.0
        assertTrue(corners.mouseClicked(10.0, y))
        assertEquals(0, radius)
        assertTrue(corners.mouseDragged(130.0, y))
        assertTrue(radius in 10..14)
        corners.mouseReleased()
    }

    @Test
    fun `slider hitbox includes the knob above a thin track`() {
        val track = UiRect(10, 20, 100, 20)
        assertTrue(SliderMath.contains(track, 50.0, 16.0))
        assertEquals(0.5f, SliderMath.normalized(track, 60.0), 0.001f)
        assertEquals(6, SliderMath.trackIn(track).h)
    }
}

class ChartsLogicTest {
    @Test
    fun `sample count matches the compact budget`() {
        assertEquals(12, Charts.sampleCount(40, 100))
        assertEquals(100, Charts.sampleCount(200, 100))
        assertEquals(2, Charts.sampleCount(200, 2))
        val sampled = Charts.downsample((0..99).map { it.toFloat() }, 5)
        assertEquals(5, sampled.size)
        assertEquals(0f, sampled.first())
        assertEquals(99f, sampled.last())
    }

    @Test
    fun `hover maps x onto the interpolated series`() {
        val rect = UiRect(10, 10, 100, 40)
        val values = listOf(0f, 10f, 20f)
        val hover = Charts.chartHover(rect, values, 60.0)
        assertEquals(60, hover!!.first)
        assertTrue(hover.second in rect.y until rect.bottom)
    }
}
