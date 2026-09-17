package dev.henny.hugoutils.sky

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

data class SkyPack(
    val key: String,
    val name: String,
    val path: Path,
    val zipped: Boolean,
    val textures: Set<SkyTexture>,
    val files: Set<String>
)

enum class SkyTexture(val resourcePaths: List<String>, val label: String) {
    SUN(
        listOf(
            "assets/minecraft/textures/environment/celestial/sun.png",
            "assets/minecraft/textures/environment/sun.png"
        ),
        "Sonne"
    ),
    MOON(
        listOf(
            "assets/minecraft/textures/environment/celestial/moon/full_moon.png",
            "assets/minecraft/textures/environment/celestial/moon/waning_gibbous.png",
            "assets/minecraft/textures/environment/celestial/moon/third_quarter.png",
            "assets/minecraft/textures/environment/celestial/moon/waning_crescent.png",
            "assets/minecraft/textures/environment/celestial/moon/new_moon.png",
            "assets/minecraft/textures/environment/celestial/moon/waxing_crescent.png",
            "assets/minecraft/textures/environment/celestial/moon/first_quarter.png",
            "assets/minecraft/textures/environment/celestial/moon/waxing_gibbous.png",
            "assets/minecraft/textures/environment/moon_phases.png"
        ),
        "Mond"
    ),
    CLOUDS(listOf("assets/minecraft/textures/environment/clouds.png"), "Wolken"),
    END(listOf("assets/minecraft/textures/environment/end_sky.png"), "End-Sky"),
    SKYBOX(emptyList(), "Skybox");

    val resourcePath: String get() = resourcePaths.first()

    companion object {
        val modernPaths: Set<String> = entries.flatMapTo(linkedSetOf()) { kind ->
            kind.resourcePaths.filterNot { it.endsWith("/sun.png") && "/celestial/" !in it }
                .filterNot { it.endsWith("/moon_phases.png") }
        }

        const val LEGACY_SUN = "assets/minecraft/textures/environment/sun.png"
        const val LEGACY_MOON = "assets/minecraft/textures/environment/moon_phases.png"
    }
}

object SkyPackScanner {
    fun scan(resourcePacks: Path): List<SkyPack> {
        if (!Files.isDirectory(resourcePacks)) return emptyList()
        return Files.list(resourcePacks).use { paths ->
            paths.iterator().asSequence()
                .filter { Files.isDirectory(it) || isZip(it) }
                .filterNot { SkyExtractor.isExtractFolder(it) }
                .mapNotNull(::inspect)
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                .toList()
        }
    }

    fun read(pack: SkyPack, texture: SkyTexture, maxBytes: Int): ByteArray? {
        val path = texture.resourcePaths.firstOrNull { it in pack.files } ?: return null
        return readPath(pack, path, maxBytes)
    }

    fun readPath(pack: SkyPack, path: String, maxBytes: Int): ByteArray? {
        if (path !in pack.files) return null
        return if (pack.zipped) {
            ZipFile(pack.path.toFile()).use { zip ->
                val entry = zip.getEntry(path) ?: zip.getEntry(path.replace('/', '\\')) ?: return null
                if (entry.size > maxBytes) return null
                zip.getInputStream(entry).use { limited(it, maxBytes) }
            }
        } else {
            val file = pack.path.resolve(path)
            if (!Files.isRegularFile(file) || Files.size(file) > maxBytes) null
            else Files.newInputStream(file).use { limited(it, maxBytes) }
        }
    }

    internal fun inspect(path: Path): SkyPack? {
        val zipped = isZip(path)
        val files = runCatching { listSkyFiles(path, zipped) }.getOrDefault(emptySet())
        if (files.isEmpty()) return null
        val found = files.mapNotNullTo(linkedSetOf(), ::classify)
        val fileName = path.fileName.toString()
        return SkyPack(
            key = path.toAbsolutePath().normalize().toString(),
            name = fileName.removeSuffix(".zip"),
            path = path,
            zipped = zipped,
            textures = found,
            files = files
        )
    }

    internal fun classify(path: String): SkyTexture? {
        val normalized = normalize(path)
        SkyTexture.entries.firstOrNull { kind ->
            kind != SkyTexture.SKYBOX && kind.resourcePaths.any { it.equals(normalized, ignoreCase = true) }
        }?.let { return it }
        return if (isSkyAsset(normalized)) SkyTexture.SKYBOX else null
    }

    internal fun isSkyAsset(path: String): Boolean {
        val normalized = normalize(path).lowercase()
        if (normalized.startsWith("__macosx") || "/." in normalized) return false
        val inSkyDir = normalized.contains("/textures/environment/") ||
            normalized.contains("/optifine/sky/") ||
            normalized.contains("/mcpatcher/sky/") ||
            normalized.contains("/textures/skybox") ||
            normalized.contains("/textures/sky/")
        if (!inSkyDir) return false
        if (
            !normalized.endsWith(".png") &&
            !normalized.endsWith(".mcmeta") &&
            !normalized.endsWith(".properties")
        ) return false
        val file = normalized.substringAfterLast('/')
        return !file.startsWith("rain") && !file.startsWith("snow") && "weather" !in file
    }

    private fun listSkyFiles(path: Path, zipped: Boolean): Set<String> {
        val found = linkedSetOf<String>()
        if (zipped) {
            ZipFile(path.toFile()).use { zip ->
                zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .map { normalize(it.name) }
                    .filter(::isSkyAsset)
                    .take(MAX_SKY_FILES)
                    .forEach(found::add)
            }
        } else {
            if (!Files.isDirectory(path)) return emptySet()
            Files.walk(path).use { walk ->
                walk.filter { Files.isRegularFile(it) }
                    .map { normalize(path.relativize(it).toString()) }
                    .filter(::isSkyAsset)
                    .limit(MAX_SKY_FILES.toLong())
                    .forEach { found.add(it) }
            }
        }
        return found
    }

    private fun isZip(path: Path): Boolean =
        Files.isRegularFile(path) && path.fileName.toString().endsWith(".zip", ignoreCase = true)

    private fun limited(input: java.io.InputStream, maxBytes: Int): ByteArray? {
        val bytes = input.readNBytes(maxBytes + 1)
        return bytes.takeIf { it.size <= maxBytes }
    }

    internal fun normalize(path: String): String = path.replace('\\', '/').trimStart('/')

    private const val MAX_SKY_FILES = 512
}
