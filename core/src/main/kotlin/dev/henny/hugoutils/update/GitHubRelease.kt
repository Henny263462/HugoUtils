package dev.henny.hugoutils.update

data class GitHubAsset(
    val name: String,
    val browserDownloadUrl: String,
    val size: Long
)

data class GitHubRelease(
    val tagName: String,
    val name: String?,
    val draft: Boolean,
    val prerelease: Boolean,
    val assets: List<GitHubAsset>
) {
    val version: SemVer? get() = SemVer.parse(tagName)
}
