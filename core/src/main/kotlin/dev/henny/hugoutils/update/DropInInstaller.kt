package dev.henny.hugoutils.update

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale

/**
 * Installs a verified HugoUtils JAR into the mods folder and disables older main JARs
 * by renaming them to `*.jar.disabled`. On Windows/Linux a running JAR can usually be
 * renamed even while locked; Fabric then loads only the new JAR on the next launch.
 */
object DropInInstaller {
    fun install(
        verifiedJar: Path,
        destination: Path,
        expectedSha256: String,
        currentJar: Path?
    ): Result {
        if (!Files.isRegularFile(verifiedJar)) {
            throw IllegalStateException("verified update JAR is missing")
        }
        val actual = Checksums.sha256(verifiedJar)
        if (!Checksums.matches(actual, expectedSha256)) {
            throw IllegalStateException("update JAR checksum mismatch")
        }
        Files.createDirectories(destination.parent)
        if (destination != verifiedJar) {
            Files.copy(verifiedJar, destination, StandardCopyOption.REPLACE_EXISTING)
        }
        val installedHash = Checksums.sha256(destination)
        if (!Checksums.matches(installedHash, expectedSha256)) {
            Files.deleteIfExists(destination)
            throw IllegalStateException("installed update checksum mismatch")
        }

        val disabled = ArrayList<Path>()
        val failed = ArrayList<Path>()
        val modsDir = destination.parent
        if (modsDir != null && Files.isDirectory(modsDir)) {
            Files.list(modsDir).use { stream ->
                stream.filter { Files.isRegularFile(it) }.forEach { path ->
                    if (path.toAbsolutePath().normalize() == destination.toAbsolutePath().normalize()) {
                        return@forEach
                    }
                    if (!isMainHugoJar(path.fileName.toString())) {
                        return@forEach
                    }
                    when (val renamed = disableJar(path)) {
                        null -> failed.add(path)
                        else -> disabled.add(renamed)
                    }
                }
            }
        }
        // Ensure the currently loaded JAR is disabled even if its name did not match.
        if (currentJar != null &&
            Files.isRegularFile(currentJar) &&
            currentJar.toAbsolutePath().normalize() != destination.toAbsolutePath().normalize()
        ) {
            if (disabled.none { it.fileName.toString().startsWith(currentJar.fileName.toString()) }) {
                when (val renamed = disableJar(currentJar)) {
                    null -> if (failed.none { it == currentJar }) failed.add(currentJar)
                    else -> if (disabled.none { it == renamed }) disabled.add(renamed)
                }
            }
        }
        return Result(destination, disabled, failed)
    }

    fun disableJar(path: Path): Path? {
        if (!Files.isRegularFile(path)) return null
        val name = path.fileName.toString()
        if (name.endsWith(".disabled", ignoreCase = true)) return path
        val target = path.resolveSibling("$name.disabled")
        return try {
            Files.move(path, target, StandardCopyOption.REPLACE_EXISTING)
            target
        } catch (_: Exception) {
            try {
                val fallback = path.resolveSibling("$name.old")
                Files.move(path, fallback, StandardCopyOption.REPLACE_EXISTING)
                fallback
            } catch (_: Exception) {
                null
            }
        }
    }

    fun isMainHugoJar(fileName: String): Boolean {
        val lower = fileName.lowercase(Locale.ROOT)
        if (!lower.endsWith(".jar")) return false
        if (lower.endsWith(".jar.disabled") || lower.endsWith(".jar.old")) return false
        if (lower.contains("hugoutils-core") ||
            lower.contains("itemglow") ||
            lower.contains("playerglow") ||
            lower.contains("fastitems") ||
            lower.contains("sources") ||
            lower.contains("javadoc") ||
            lower.contains("-dev")
        ) {
            return false
        }
        return lower.startsWith("hugoutils-") || lower == "hugoutils.jar"
    }

    data class Result(
        val installed: Path,
        val disabled: List<Path>,
        val failedToDisable: List<Path>
    )
}
