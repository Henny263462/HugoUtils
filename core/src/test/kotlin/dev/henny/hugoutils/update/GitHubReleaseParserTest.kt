package dev.henny.hugoutils.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GitHubReleaseParserTest {
    @Test
    fun parsesReleaseList() {
        val json = """
            [
              {
                "tag_name": "v1.2.3",
                "name": "HugoUtils 1.2.3",
                "draft": false,
                "prerelease": false,
                "assets": [
                  {
                    "name": "HugoUtils-1.2.3.jar",
                    "browser_download_url": "https://github.com/Henny263462/HugoUtils/releases/download/v1.2.3/HugoUtils-1.2.3.jar",
                    "size": 12
                  },
                  {
                    "name": "SHA256SUMS.txt",
                    "browser_download_url": "https://github.com/Henny263462/HugoUtils/releases/download/v1.2.3/SHA256SUMS.txt",
                    "size": 80
                  }
                ]
              }
            ]
        """.trimIndent()
        val releases = GitHubReleaseParser.parseList(json)
        assertEquals(1, releases.size)
        assertEquals("v1.2.3", releases[0].tagName)
        assertEquals(2, releases[0].assets.size)
        assertEquals("HugoUtils-1.2.3.jar", ReleaseSelector.selectJar(releases[0])?.name)
    }

    @Test
    fun rejectsMalformedPayload() {
        assertThrows<IllegalArgumentException> {
            GitHubReleaseParser.parseList("""{"tag_name":"v1.0.0"}""")
        }
    }

    @Test
    fun checksumParserReadsSha256SumFormat() {
        val body = """
            abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd  HugoUtils-1.2.3.jar
            1111111111111111111111111111111111111111111111111111111111111111 *SHA256SUMS.txt
        """.trimIndent()
        val sums = Checksums.parse(body)
        assertEquals(
            "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd",
            Checksums.hashFor(sums, "HugoUtils-1.2.3.jar")
        )
        assertNull(Checksums.hashFor(sums, "missing.jar"))
        assertTrue(Checksums.matches("ABCD", "abcd"))
    }
}
