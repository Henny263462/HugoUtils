package dev.henny.hugoutils.sky

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SkyPackScannerTest {
    @Test
    fun `finds sky textures in folders and zip packs`(@TempDir root: Path) {
        val packs = root.resolve("resourcepacks")
        val folder = packs.resolve("Folder Sky")
        val sun = folder.resolve(SkyTexture.SUN.resourcePath)
        Files.createDirectories(sun.parent)
        Files.write(sun, byteArrayOf(1, 2, 3))

        Files.createDirectories(packs)
        val zip = packs.resolve("Moon Sky.zip")
        ZipOutputStream(Files.newOutputStream(zip)).use {
            it.putNextEntry(ZipEntry(SkyTexture.MOON.resourcePath))
            it.write(byteArrayOf(4, 5, 6))
            it.closeEntry()
        }

        val found = SkyPackScanner.scan(packs)
        assertEquals(listOf("Folder Sky", "Moon Sky"), found.map { it.name })
        assertEquals(setOf(SkyTexture.SUN), found[0].textures)
        assertEquals(setOf(SkyTexture.MOON), found[1].textures)
        assertArrayEquals(byteArrayOf(4, 5, 6), SkyPackScanner.read(found[1], SkyTexture.MOON, 64))
    }

    @Test
    fun `recognizes all modern celestial moon files`(@TempDir root: Path) {
        val pack = root.resolve("resourcepacks").resolve("Modern Sky")
        val relative = "assets/minecraft/textures/environment/celestial/moon/full_moon.png"
        val moon = pack.resolve(relative)
        Files.createDirectories(moon.parent)
        Files.write(moon, byteArrayOf(7, 8))

        val found = SkyPackScanner.scan(root.resolve("resourcepacks")).single()
        assertEquals(setOf(SkyTexture.MOON), found.textures)
        assertEquals(setOf(relative), found.files)
    }

    @Test
    fun `ignores packs without sky changes`(@TempDir root: Path) {
        val pack = root.resolve("resourcepacks").resolve("Normal Pack")
        Files.createDirectories(pack.resolve("assets/minecraft/textures/block"))
        Files.write(pack.resolve("assets/minecraft/textures/block/stone.png"), byteArrayOf(1))
        assertEquals(emptyList<SkyPack>(), SkyPackScanner.scan(root.resolve("resourcepacks")))
    }
}
