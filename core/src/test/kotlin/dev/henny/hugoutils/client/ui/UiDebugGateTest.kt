package dev.henny.hugoutils.client.ui

import com.google.gson.Gson
import dev.henny.hugoutils.client.config.ModConfig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiDebugGateTest {
    @Test
    fun `release commands require persistent opt in`() {
        assertFalse(UiDebugGate.isEnabled(developmentEnvironment = false, configured = false))
        assertTrue(UiDebugGate.isEnabled(developmentEnvironment = false, configured = true))
        assertTrue(UiDebugGate.isEnabled(developmentEnvironment = true, configured = false))
    }

    @Test
    fun `legacy config keeps debug commands disabled`() {
        val legacy = Gson().fromJson("{}", ModConfig::class.java)
        assertFalse(legacy.uiDebugCommandsEnabled)

        val enabled = Gson().fromJson("""{"uiDebugCommandsEnabled":true}""", ModConfig::class.java)
        assertTrue(enabled.uiDebugCommandsEnabled)
    }
}
