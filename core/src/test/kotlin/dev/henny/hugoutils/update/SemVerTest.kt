package dev.henny.hugoutils.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SemVerTest {
    @Test
    fun stripsLeadingV() {
        assertEquals(SemVer.parse("v1.2.3"), SemVer.parse("1.2.3"))
        assertEquals(0, SemVer.parse("v1.2.3")!!.compareTo(SemVer.parse("1.2.3")!!))
    }

    @Test
    fun comparesPatchNumerically() {
        assertTrue(SemVer.parse("1.2.9")!! < SemVer.parse("1.2.10")!!)
        assertTrue(SemVer.parse("1.9.0")!! < SemVer.parse("1.10.0")!!)
        assertTrue(SemVer.parse("1.0.0")!! < SemVer.parse("2.0.0")!!)
    }

    @Test
    fun prereleaseIsOlderThanRelease() {
        assertTrue(SemVer.parse("1.2.3-beta")!! < SemVer.parse("1.2.3")!!)
        assertTrue(SemVer.parse("1.2.3-alpha")!! < SemVer.parse("1.2.3-beta")!!)
        assertTrue(SemVer.parse("1.2.3-beta.2")!! < SemVer.parse("1.2.3-beta.10")!!)
    }

    @Test
    fun rejectsLexicalOrdering() {
        assertFalse(SemVer.parse("1.2.9")!!.original < SemVer.parse("1.2.10")!!.original)
        assertTrue(SemVer.parse("1.2.9")!! < SemVer.parse("1.2.10")!!)
    }

    @Test
    fun parseRejectsGarbage() {
        assertNull(SemVer.parse(""))
        assertNull(SemVer.parse("not-a-version"))
        assertNull(SemVer.parse("v"))
    }
}
