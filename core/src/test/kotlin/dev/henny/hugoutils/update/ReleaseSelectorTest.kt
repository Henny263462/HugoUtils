package dev.henny.hugoutils.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReleaseSelectorTest {
    private val installed = SemVer.parse("1.2.3")!!

    @Test
    fun ignoresDraftsAndOlderReleases() {
        val latest = ReleaseSelector.latest(
            listOf(
                release("v1.2.4", draft = true),
                release("v1.2.3"),
                release("v1.2.2"),
                release("v1.3.0")
            ),
            installed,
            includePrereleases = false
        )
        assertEquals("v1.3.0", latest?.tagName)
    }

    @Test
    fun hidesPrereleasesUnlessEnabled() {
        val prerelease = release("v1.4.0-rc.1", prerelease = true)
        assertNull(ReleaseSelector.latest(listOf(prerelease), installed, includePrereleases = false))
        assertEquals("v1.4.0-rc.1", ReleaseSelector.latest(listOf(prerelease), installed, includePrereleases = true)?.tagName)
    }

    @Test
    fun selectsPreferredFabricJar() {
        val release = GitHubRelease(
            tagName = "v1.3.0",
            name = "HugoUtils 1.3.0",
            draft = false,
            prerelease = false,
            assets = listOf(
                GitHubAsset("HugoUtils-1.3.0-sources.jar", "https://example.invalid/sources", 1),
                GitHubAsset("hugoutils-itemglow-1.3.0.jar", "https://example.invalid/itemglow", 1),
                GitHubAsset("hugoutils-core-1.3.0.jar", "https://example.invalid/core", 1),
                GitHubAsset("fastitems-1.3.0.jar", "https://example.invalid/fastitems", 1),
                GitHubAsset("HugoUtils-1.3.0.jar", "https://example.invalid/mod", 10)
            )
        )
        assertEquals("HugoUtils-1.3.0.jar", ReleaseSelector.selectJar(release)?.name)
    }

    @Test
    fun acceptsLegacyLowercaseJarName() {
        val release = GitHubRelease(
            tagName = "v1.3.0",
            name = null,
            draft = false,
            prerelease = false,
            assets = listOf(GitHubAsset("hugoutils-1.3.0.jar", "https://example.invalid/mod", 10))
        )
        assertEquals("hugoutils-1.3.0.jar", ReleaseSelector.selectJar(release)?.name)
    }

    @Test
    fun rejectsModuleAndDevJars() {
        assertTrue(
            ReleaseSelector.selectJar(
                GitHubRelease(
                    tagName = "v1.3.0",
                    name = null,
                    draft = false,
                    prerelease = false,
                    assets = listOf(
                        GitHubAsset("hugoutils-playerglow-1.3.0.jar", "https://example.invalid/player", 1),
                        GitHubAsset("HugoUtils-1.3.0-dev.jar", "https://example.invalid/dev", 1)
                    )
                )
            ) == null
        )
    }

    private fun release(tag: String, draft: Boolean = false, prerelease: Boolean = false) = GitHubRelease(
        tagName = tag,
        name = tag,
        draft = draft,
        prerelease = prerelease,
        assets = listOf(GitHubAsset("HugoUtils-${tag.removePrefix("v")}.jar", "https://example.invalid/mod", 1))
    )
}
