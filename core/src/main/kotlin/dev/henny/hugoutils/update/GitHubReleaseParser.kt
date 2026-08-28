package dev.henny.hugoutils.update

import com.google.gson.JsonParser

object GitHubReleaseParser {
    fun parseList(body: String): List<GitHubRelease> {
        val root = JsonParser.parseString(body)
        if (!root.isJsonArray) {
            throw IllegalArgumentException("malformed release JSON")
        }
        return root.asJsonArray.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val obj = element.asJsonObject
            val tag = obj.get("tag_name")?.takeIf { it.isJsonPrimitive }?.asString ?: return@mapNotNull null
            val assets = obj.getAsJsonArray("assets")?.mapNotNull { assetEl ->
                if (!assetEl.isJsonObject) return@mapNotNull null
                val asset = assetEl.asJsonObject
                val name = asset.get("name")?.takeIf { it.isJsonPrimitive }?.asString ?: return@mapNotNull null
                val url = asset.get("browser_download_url")?.takeIf { it.isJsonPrimitive }?.asString
                    ?: return@mapNotNull null
                val size = asset.get("size")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0L
                GitHubAsset(name, url, size)
            } ?: emptyList()
            GitHubRelease(
                tagName = tag,
                name = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString,
                draft = obj.get("draft")?.takeIf { it.isJsonPrimitive }?.asBoolean == true,
                prerelease = obj.get("prerelease")?.takeIf { it.isJsonPrimitive }?.asBoolean == true,
                assets = assets
            )
        }
    }
}
