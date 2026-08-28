package dev.henny.hugoutils.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class JarReplacerTest {
    @TempDir
    lateinit var temp: Path

    @Test
    fun sha256SuccessAndFailure() {
        val file = temp.resolve("payload.bin")
        Files.writeString(file, "hello")
        val hash = Checksums.sha256(file)
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash)
        assertTrue(Checksums.matches(hash, hash.uppercase()))
        assertFalse(Checksums.matches(hash, "00".repeat(32)))
    }

    @Test
    fun stagesAndReplacesWithRollbackOnMismatch() {
        val mods = temp.resolve("mods")
        val staging = temp.resolve("staging")
        Files.createDirectories(mods)
        Files.createDirectories(staging)
        val current = mods.resolve("hugoutils-1.0.0.jar")
        val staged = staging.resolve("HugoUtils-1.1.0.jar")
        val destination = mods.resolve("HugoUtils-1.1.0.jar")
        Files.writeString(current, "old-mod")
        Files.writeString(staged, "new-mod")
        val expected = Checksums.sha256(staged)
        JarReplacer.install(current, staged, destination, expected)
        assertEquals("new-mod", Files.readString(destination))
        assertFalse(Files.exists(current))
        assertTrue(Files.list(mods).use { it.none { path -> path.fileName.toString().endsWith(".bak") } })
    }

    @Test
    fun rejectsChecksumMismatchAndLeavesOriginalJar() {
        val mods = temp.resolve("mods")
        val staging = temp.resolve("staging")
        Files.createDirectories(mods)
        Files.createDirectories(staging)
        val current = mods.resolve("HugoUtils-1.0.0.jar")
        val staged = staging.resolve("HugoUtils-1.1.0.jar")
        Files.writeString(current, "old-mod")
        Files.writeString(staged, "tampered")
        val expected = "00".repeat(32)
        assertThrows<IllegalStateException> {
            JarReplacer.install(current, staged, mods.resolve("HugoUtils-1.1.0.jar"), expected)
        }
        assertEquals("old-mod", Files.readString(current))
        assertTrue(Files.exists(current))
    }

    @Test
    fun rollsBackWhenDestinationVerificationWouldFail() {
        val mods = temp.resolve("mods")
        val staging = temp.resolve("staging")
        Files.createDirectories(mods)
        Files.createDirectories(staging)
        val current = mods.resolve("HugoUtils-1.0.0.jar")
        val staged = staging.resolve("HugoUtils-1.1.0.jar")
        Files.writeString(current, "old-mod")
        Files.writeString(staged, "new-mod")
        val expected = Checksums.sha256(staged)
        val destination = mods.resolve("HugoUtils-1.1.0.jar")
        JarReplacer.install(current, staged, destination, expected)
        assertEquals("new-mod", Files.readString(destination))
        assertFalse(Files.exists(mods.resolve("HugoUtils-1.0.0.jar")))
        assertEquals(1, Files.list(mods).use { it.filter { path -> path.fileName.toString().endsWith(".jar") }.count() })
    }

    @Test
    fun backupIsNotALoadableJar() {
        val backup = JarReplacer.uniqueBackup(temp, "HugoUtils-1.0.0.jar")
        assertTrue(backup.fileName.toString().endsWith(".bak"))
        assertFalse(backup.fileName.toString().endsWith(".jar"))
    }
}
