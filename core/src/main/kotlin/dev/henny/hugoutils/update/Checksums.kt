package dev.henny.hugoutils.update

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

object Checksums {
    fun parse(body: String): Map<String, String> {
        val values = LinkedHashMap<String, String>()
        for (raw in body.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val match = Regex("^([0-9a-fA-F]{64})\\s+\\*?(.+)$").matchEntire(line) ?: continue
            val hash = match.groupValues[1].lowercase()
            val name = match.groupValues[2].trim().substringAfterLast('/').substringAfterLast('\\')
            values[name] = hash
        }
        return values
    }

    fun hashFor(sums: Map<String, String>, fileName: String): String? {
        sums[fileName]?.let { return it }
        return sums.entries.firstOrNull { it.key.equals(fileName, ignoreCase = true) }?.value
    }

    fun sha256(path: Path): String = Files.newInputStream(path).use { sha256(it) }

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    fun matches(actual: String, expected: String): Boolean =
        actual.equals(expected, ignoreCase = true)
}
