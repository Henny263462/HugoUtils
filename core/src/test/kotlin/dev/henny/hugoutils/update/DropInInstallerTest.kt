package dev.henny.hugoutils.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class DropInInstallerTest {
    @TempDir
    lateinit var temp: Path

    @Test
    fun recognizesMainHugoJarsOnly() {
        assertTrue(DropInInstaller.isMainHugoJar("HugoUtils-0.1.3.jar"))
        assertTrue(DropInInstaller.isMainHugoJar("hugoutils-0.1.0.jar"))
        assertFalse(DropInInstaller.isMainHugoJar("hugoutils-core-0.1.3.jar"))
        assertFalse(DropInInstaller.isMainHugoJar("hugoutils-itemglow-0.1.3.jar"))
        assertFalse(DropInInstaller.isMainHugoJar("HugoUtils-0.1.3.jar.disabled"))
    }

    @Test
    fun installsNewJarAndDisablesOldOne() {
        val mods = temp.resolve("mods")
        Files.createDirectories(mods)
        val oldJar = mods.resolve("HugoUtils-0.1.0.jar")
        val staged = temp.resolve("HugoUtils-0.1.4.jar")
        val destination = mods.resolve("HugoUtils-0.1.4.jar")
        Files.writeString(oldJar, "old")
        Files.writeString(staged, "new-payload")
        val hash = Checksums.sha256(staged)

        val result = DropInInstaller.install(staged, destination, hash, oldJar)

        assertEquals("new-payload", Files.readString(destination))
        assertFalse(Files.exists(oldJar))
        assertTrue(Files.exists(mods.resolve("HugoUtils-0.1.0.jar.disabled")))
        assertEquals(1, result.disabled.size)
        assertTrue(result.failedToDisable.isEmpty())
    }
}
