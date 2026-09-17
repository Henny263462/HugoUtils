package dev.henny.hugoutils.sky

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SkyExtractorTest {
    @Test
    fun `extracts only sky files and aliases legacy sun`(@TempDir root: Path) {
        val packs = root.resolve("resourcepacks")
        val source = packs.resolve("Faithful Mix")
        write(source, SkyTexture.LEGACY_SUN, byteArrayOf(1, 2))
        write(source, SkyTexture.CLOUDS.resourcePath, byteArrayOf(3, 4))
        write(source, "assets/minecraft/textures/block/stone.png", byteArrayOf(9, 9))
        write(source, "assets/minecraft/textures/environment/rain.png", byteArrayOf(8))

        val found = SkyPackScanner.scan(packs).single()
        assertEquals(setOf(SkyTexture.SUN, SkyTexture.CLOUDS), found.textures)

        val extracted = SkyExtractor.extract(found, packs, 64)
        assertTrue(SkyExtractor.isExtractFolder(extracted.path))
        assertEquals("HugoSky-Faithful Mix", extracted.path.fileName.toString())
        assertTrue(Files.isRegularFile(extracted.path.resolve("pack.mcmeta")))
        assertArrayEquals(byteArrayOf(3, 4), extracted.path.resolve(SkyTexture.CLOUDS.resourcePath).let(Files::readAllBytes))
        assertArrayEquals(byteArrayOf(1, 2), extracted.path.resolve(SkyTexture.SUN.resourcePath).let(Files::readAllBytes))
        assertFalse(Files.exists(extracted.path.resolve("assets/minecraft/textures/block/stone.png")))
        assertFalse(Files.exists(extracted.path.resolve("assets/minecraft/textures/environment/rain.png")))
        assertEquals("file/HugoSky-Faithful Mix", SkyExtractor.packProfileId(extracted.path.fileName.toString()))
        assertEquals("file/Faithful Mix", SkyExtractor.sourceProfileId(found))
    }

    @Test
    fun `scan skips previous extracts and keeps skybox files`(@TempDir root: Path) {
        val packs = root.resolve("resourcepacks")
        val source = packs.resolve("Cubemap Sky")
        write(source, "assets/minecraft/textures/environment/overworld_sky.png", byteArrayOf(5, 6, 7))
        val extractedDir = packs.resolve("HugoSky-Old")
        write(extractedDir, SkyTexture.CLOUDS.resourcePath, byteArrayOf(1))

        val found = SkyPackScanner.scan(packs)
        assertEquals(listOf("Cubemap Sky"), found.map { it.name })
        assertEquals(setOf(SkyTexture.SKYBOX), found.single().textures)
    }

    @Test
    fun `extracts sky files from zip packs`(@TempDir root: Path) {
        val packs = root.resolve("resourcepacks")
        Files.createDirectories(packs)
        val zip = packs.resolve("Packed Sky.zip")
        ZipOutputStream(Files.newOutputStream(zip)).use {
            it.putNextEntry(ZipEntry(SkyTexture.END.resourcePath))
            it.write(byteArrayOf(2, 2, 2))
            it.closeEntry()
            it.putNextEntry(ZipEntry("assets/minecraft/textures/item/diamond.png"))
            it.write(byteArrayOf(1))
            it.closeEntry()
        }

        val extracted = SkyExtractor.extract(SkyPackScanner.scan(packs).single(), packs, 64)
        assertArrayEquals(byteArrayOf(2, 2, 2), extracted.path.resolve(SkyTexture.END.resourcePath).let(Files::readAllBytes))
        assertFalse(Files.exists(extracted.path.resolve("assets/minecraft/textures/item/diamond.png")))
    }

    private fun write(root: Path, relative: String, bytes: ByteArray) {
        val file = root.resolve(relative)
        Files.createDirectories(file.parent)
        Files.write(file, bytes)
    }
}
