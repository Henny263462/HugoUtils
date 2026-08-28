package dev.henny.hugoutils.update

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: List<PrereleasePart> = emptyList(),
    val original: String
) : Comparable<SemVer> {
    val isPrerelease: Boolean get() = prerelease.isNotEmpty()

    override fun compareTo(other: SemVer): Int {
        val core = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
        if (core != 0) return core
        if (prerelease.isEmpty() && other.prerelease.isEmpty()) return 0
        if (prerelease.isEmpty()) return 1
        if (other.prerelease.isEmpty()) return -1
        val limit = minOf(prerelease.size, other.prerelease.size)
        for (index in 0 until limit) {
            val cmp = prerelease[index].compareTo(other.prerelease[index])
            if (cmp != 0) return cmp
        }
        return prerelease.size.compareTo(other.prerelease.size)
    }

    override fun toString(): String = original

    companion object {
        fun parse(raw: String): SemVer? {
            val trimmed = raw.trim().removePrefix("v").removePrefix("V")
            if (trimmed.isEmpty()) return null
            val plus = trimmed.indexOf('+')
            val withoutBuild = if (plus >= 0) trimmed.substring(0, plus) else trimmed
            val dash = withoutBuild.indexOf('-')
            val core = if (dash >= 0) withoutBuild.substring(0, dash) else withoutBuild
            val pre = if (dash >= 0) withoutBuild.substring(dash + 1) else ""
            val parts = core.split('.')
            if (parts.isEmpty() || parts.size > 3) return null
            val numbers = parts.map { part ->
                part.toIntOrNull()?.takeIf { it >= 0 } ?: return null
            }
            val prerelease = if (pre.isEmpty()) {
                emptyList()
            } else {
                pre.split('.').map { token ->
                    if (token.isEmpty()) return null
                    val numeric = token.toLongOrNull()
                    if (numeric != null) PrereleasePart.Numeric(numeric) else PrereleasePart.Text(token)
                }
            }
            return SemVer(
                major = numbers.getOrElse(0) { 0 },
                minor = numbers.getOrElse(1) { 0 },
                patch = numbers.getOrElse(2) { 0 },
                prerelease = prerelease,
                original = trimmed
            )
        }
    }

    sealed class PrereleasePart : Comparable<PrereleasePart> {
        data class Numeric(val value: Long) : PrereleasePart()
        data class Text(val value: String) : PrereleasePart()

        override fun compareTo(other: PrereleasePart): Int = when {
            this is Numeric && other is Numeric -> value.compareTo(other.value)
            this is Numeric && other is Text -> -1
            this is Text && other is Numeric -> 1
            this is Text && other is Text -> value.compareTo(other.value)
            else -> 0
        }
    }
}
