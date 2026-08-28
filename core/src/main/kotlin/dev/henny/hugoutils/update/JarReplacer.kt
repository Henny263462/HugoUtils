package dev.henny.hugoutils.update

import java.nio.file.Path

object JarReplacer {
    fun install(
        currentJar: Path,
        stagedJar: Path,
        destination: Path,
        expectedSha256: String
    ) {
        UpdateHelper.install(currentJar, stagedJar, destination, expectedSha256)
    }

    fun uniqueBackup(directory: Path, originalName: String): Path =
        UpdateHelper.uniqueBackup(directory, originalName)
}
