package dev.henny.hugoutils.sky

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

object SkyExtractor {
    const val FOLDER_PREFIX = "HugoSky-"

    fun isExtractFolder(path: Path): Boolean =
        path.fileName.toString().startsWith(FOLDER_PREFIX, ignoreCase = true)

    fun folderName(sourceName: String): String = FOLDER_PREFIX + sanitize(sourceName)

    fun packProfileId(folderName: String): String = "file/$folderName"

    fun sourceProfileId(pack: SkyPack): String = "file/${pack.path.fileName}"

    fun extract(source: SkyPack, resourcePacks: Path, maxBytes: Int): SkyPack {
        val dest = resourcePacks.resolve(folderName(source.name))
        deleteRecursively(dest)
        Files.createDirectories(dest)
        writePackMcmeta(dest, source.name)
        val written = linkedSetOf<String>()
        source.files.forEach { relative ->
            val bytes = SkyPackScanner.readPath(source, relative, maxBytes) ?: return@forEach
            writeAsset(dest, relative, bytes)
            written += relative
        }
        if (SkyTexture.SUN.resourcePath !in written && SkyTexture.LEGACY_SUN in written) {
            SkyPackScanner.readPath(source, SkyTexture.LEGACY_SUN, maxBytes)?.let { bytes ->
                writeAsset(dest, SkyTexture.SUN.resourcePath, bytes)
            }
        }
        return SkyPackScanner.inspect(dest)
            ?: error("Im Texturepack wurden keine Sky-Dateien gefunden.")
    }

    private fun writeAsset(root: Path, relative: String, bytes: ByteArray) {
        val file = root.resolve(relative)
        Files.createDirectories(file.parent)
        Files.write(file, bytes)
    }

    private fun writePackMcmeta(root: Path, sourceName: String) {
        val description = "HugoUtils Sky-Extrakt: ${jsonEscape(sourceName)} (nur Himmel)"
        val json = """
            {
              "pack": {
                "description": "$description",
                "min_format": 65,
                "max_format": 99
              }
            }
        """.trimIndent() + "\n"
        Files.writeString(root.resolve("pack.mcmeta"), json)
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { walk ->
            walk.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private fun sanitize(name: String): String {
        val cleaned = name.replace(Regex("""[<>:"/\\|?*]"""), "_").trim().ifBlank { "Sky" }
        return cleaned.take(80)
    }

    private fun jsonEscape(text: String): String =
        text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "")
}
