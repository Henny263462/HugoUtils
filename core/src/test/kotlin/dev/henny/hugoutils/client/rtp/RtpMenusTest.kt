package dev.henny.hugoutils.client.rtp

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RtpMenusTest {
    @Test
    fun detectsRtpMenuTitles() {
        assertTrue(RtpMenus.isRtpTitle("RTP"))
        assertTrue(RtpMenus.isRtpTitle("§6Random Teleport"))
        assertTrue(RtpMenus.isRtpTitle("Zufalls-TP"))
        assertTrue(RtpMenus.isRtpTitle("Zufallsteleport"))
        assertTrue(RtpMenus.isRtpTitle("Wilderness"))
        assertTrue(RtpMenus.isRtpTitle("Wildnis-Teleport"))
        assertFalse(RtpMenus.isRtpTitle("Warps"))
        assertFalse(RtpMenus.isRtpTitle("Teleport"))
        assertFalse(RtpMenus.isRtpTitle(""))
    }

    @Test
    fun detectsRtpItems() {
        assertTrue(RtpMenus.isRtpItem("RTP"))
        assertTrue(RtpMenus.isRtpItem("§aRandom TP"))
        assertTrue(RtpMenus.isRtpItem("Zufalls-Teleport"))
        assertFalse(RtpMenus.isRtpItem("Nether"))
        assertFalse(RtpMenus.isRtpItem("End"))
    }

    @Test
    fun detectsRtpCommands() {
        assertTrue(RtpMenus.isRtpCommand("rtp"))
        assertTrue(RtpMenus.isRtpCommand("/rtp overworld"))
        assertTrue(RtpMenus.isRtpCommand("wild"))
        assertTrue(RtpMenus.isRtpCommand("randomtp"))
        assertFalse(RtpMenus.isRtpCommand("spawn"))
        assertFalse(RtpMenus.isRtpCommand("tp"))
    }
}
