package dev.henny.hugoutils.update

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile

object HelperJarExtractor {
    /**
     * Materializes a runnable update-helper JAR that contains [UpdateHelper].
     * The outer Fabric distribution nests hugoutils-core under META-INF/jars/, so
     * running `-cp HugoUtils-*.jar UpdateHelper` fails with ClassNotFoundException.
     */
    fun materialize(destination: Path, preferredSources: List<Path>, outerJar: Path?): Path {
        Files.createDirectories(destination.parent)
        preferredSources.firstOrNull { isCoreJarFile(it) }?.let { source ->
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            return destination
        }
        if (outerJar != null && Files.isRegularFile(outerJar)) {
            extractNestedCore(outerJar, destination)
            return destination
        }
        throw IllegalStateException("The update helper could not be copied.")
    }

    fun isCoreJarFile(path: Path): Boolean {
        if (!Files.isRegularFile(path)) return false
        val name = path.fileName.toString().lowercase()
        return name.startsWith("hugoutils-core-") && name.endsWith(".jar")
    }

    fun extractNestedCore(outerJar: Path, destination: Path) {
        JarFile(outerJar.toFile()).use { jar ->
            val entry = jar.entries().asSequence().firstOrNull { candidate ->
                !candidate.isDirectory &&
                    candidate.name.startsWith("META-INF/jars/hugoutils-core-") &&
                    candidate.name.endsWith(".jar")
            } ?: throw IllegalStateException("Nested hugoutils-core JAR is missing from ${outerJar.fileName}.")
            jar.getInputStream(entry).use { input ->
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
