package dev.henny.hugoutils.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class UpdateHelperTest {
    @TempDir
    lateinit var temp: Path

    @Test
    fun parsesRequiredArguments() {
        val parsed = UpdateHelper.Arguments.parse(
            arrayOf(
                "--pid", "0",
                "--target", "/mods/HugoUtils-1.0.0.jar",
                "--staged", "/tmp/HugoUtils-1.1.0.jar",
                "--destination", "/mods/HugoUtils-1.1.0.jar",
                "--sha256", "abc",
                "--restart"
            )
        )
        assertEquals(0L, parsed.pid)
        assertEquals("HugoUtils-1.0.0.jar", parsed.target.fileName.toString())
        assertTrue(parsed.restart)
    }

    @Test
    fun helperInstallsVerifiedJarAndRemovesPreviousCopy() {
        val mods = temp.resolve("mods")
        val staging = temp.resolve("staging")
        Files.createDirectories(mods)
        Files.createDirectories(staging)
        val current = mods.resolve("hugoutils-1.0.0.jar")
        val staged = staging.resolve("HugoUtils-1.1.0.jar")
        val destination = mods.resolve("HugoUtils-1.1.0.jar")
        Files.writeString(current, "old")
        Files.writeString(staged, "new")
        val hash = UpdateHelper.sha256(staged)
        val code = UpdateHelper.run(
            arrayOf(
                "--pid", "0",
                "--target", current.toString(),
                "--staged", staged.toString(),
                "--destination", destination.toString(),
                "--sha256", hash
            )
        )
        assertEquals(0, code)
        assertEquals("new", Files.readString(destination))
        assertFalse(Files.exists(current))
        assertEquals(1, Files.list(mods).use { stream -> stream.filter { path -> path.fileName.toString().endsWith(".jar") }.count() })
    }
}
