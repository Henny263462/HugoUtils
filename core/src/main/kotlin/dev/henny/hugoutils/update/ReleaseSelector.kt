package dev.henny.hugoutils.update

object ReleaseSelector {
    private val rejected = listOf(
        "sources",
        "javadoc",
        "-dev",
        "itemglow",
        "playerglow",
        "fastitems",
        "hugoutils-core"
    )

    fun latest(
        releases: List<GitHubRelease>,
        installed: SemVer,
        includePrereleases: Boolean
    ): GitHubRelease? {
        return releases
            .asSequence()
            .filter { !it.draft }
            .filter { includePrereleases || !it.prerelease }
            .mapNotNull { release -> release.version?.let { release to it } }
            .filter { (_, version) -> version > installed }
            .maxWithOrNull { a, b -> a.second.compareTo(b.second) }
            ?.first
    }

    fun selectJar(release: GitHubRelease): GitHubAsset? {
        val version = release.version?.original ?: release.tagName.removePrefix("v").removePrefix("V")
        val preferred = listOf(
            "HugoUtils-$version.jar",
            "hugoutils-$version.jar"
        )
        val candidates = release.assets.filter(::isDistributableJar)
        preferred.forEach { name ->
            candidates.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        }
        return candidates.singleOrNull()
    }

    fun isDistributableJar(asset: GitHubAsset): Boolean {
        val name = asset.name
        if (!name.endsWith(".jar", ignoreCase = true)) return false
        val lower = name.lowercase()
        if (rejected.any { lower.contains(it) }) return false
        if (!lower.startsWith("hugoutils-")) return false
        return true
    }
}
