package dev.henny.hugoutils.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class HelperJarExtractorTest {
    @TempDir
    lateinit var temp: Path

    @Test
    fun prefersRealCoreJarOverOuterJar() {
        val core = temp.resolve("hugoutils-core-0.1.1.jar")
        Files.writeString(core, "core-bytes")
        val outer = temp.resolve("HugoUtils-0.1.1.jar")
        Files.writeString(outer, "outer")
        val destination = temp.resolve("update-helper.jar")
        HelperJarExtractor.materialize(destination, listOf(outer, core), outer)
        assertEquals("core-bytes", Files.readString(destination))
    }

    @Test
    fun extractsNestedCoreFromFatJar() {
        val outer = temp.resolve("HugoUtils-0.1.1.jar")
        JarOutputStream(Files.newOutputStream(outer)).use { jar ->
            jar.putNextEntry(JarEntry("META-INF/jars/hugoutils-core-0.1.1.jar"))
            jar.write("nested-core".toByteArray())
            jar.closeEntry()
            jar.putNextEntry(JarEntry("META-INF/jars/hugoutils-itemglow-0.1.1.jar"))
            jar.write("itemglow".toByteArray())
            jar.closeEntry()
        }
        val destination = temp.resolve("update-helper.jar")
        HelperJarExtractor.materialize(destination, emptyList(), outer)
        assertEquals("nested-core", Files.readString(destination))
        assertTrue(Files.size(destination) > 0)
    }

    @Test
    fun failsWhenNestedCoreMissing() {
        val outer = temp.resolve("HugoUtils-0.1.1.jar")
        JarOutputStream(Files.newOutputStream(outer)).use { jar ->
            jar.putNextEntry(JarEntry("META-INF/jars/hugoutils-itemglow-0.1.1.jar"))
            jar.write("itemglow".toByteArray())
            jar.closeEntry()
        }
        assertThrows<IllegalStateException> {
            HelperJarExtractor.materialize(temp.resolve("helper.jar"), emptyList(), outer)
        }
    }
}
