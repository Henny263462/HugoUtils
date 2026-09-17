package dev.henny.hugoutils.sky

import dev.henny.hugoutils.sky.mixin.SpriteAtlasTextureAccessor
import dev.henny.hugoutils.sky.mixin.SpriteContentsAccessor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.util.Identifier
import java.io.ByteArrayInputStream

object CelestialAtlasPatcher {
    fun apply(images: Map<String, ByteArray>) {
        val atlas = MinecraftClient.getInstance().atlasManager
            .getAtlasTexture(Identifier.ofVanilla("celestials"))
        var changed = false
        images.forEach { (path, bytes) ->
            val spritePath = spritePath(path) ?: return@forEach
            NativeImage.read(ByteArrayInputStream(bytes)).use { source ->
                patchSprite(atlas, spritePath, source, 0, 0, source.width, source.height)
                changed = true
            }
        }
        images["legacy:moon_phases"]?.let { bytes ->
            NativeImage.read(ByteArrayInputStream(bytes)).use { sheet ->
                val cellWidth = sheet.width / 4
                val cellHeight = sheet.height / 2
                if (cellWidth > 0 && cellHeight > 0) {
                    MOON_PHASES.forEachIndexed { index, phase ->
                        patchSprite(
                            atlas,
                            "moon/$phase",
                            sheet,
                            (index % 4) * cellWidth,
                            (index / 4) * cellHeight,
                            cellWidth,
                            cellHeight
                        )
                    }
                    changed = true
                }
            }
        }
        if (changed) {
            (atlas as SpriteAtlasTextureAccessor).`hugoutils$upload`()
        }
    }

    private fun patchSprite(
        atlas: net.minecraft.client.texture.SpriteAtlasTexture,
        spritePath: String,
        source: NativeImage,
        sourceX: Int,
        sourceY: Int,
        sourceWidth: Int,
        sourceHeight: Int
    ) {
        val sprite = atlas.getSprite(Identifier.ofVanilla(spritePath))
        val contents = sprite.contents
        val access = contents as SpriteContentsAccessor
        val target = access.`hugoutils$getImage`()
        if (sourceX == 0 && sourceY == 0 && sourceWidth == target.width && sourceHeight == target.height) {
            target.copyFrom(source)
        } else {
            source.resizeSubRectTo(sourceX, sourceY, sourceWidth, sourceHeight, target)
        }
        access.`hugoutils$getMipmapLevelsImages`()
            .drop(1)
            .forEach { runCatching { it.close() } }
        access.`hugoutils$setMipmapLevelsImages`(arrayOf(target))
        contents.generateMipmaps((atlas as SpriteAtlasTextureAccessor).`hugoutils$getMipLevel`())
    }

    private fun spritePath(resourcePath: String): String? {
        val prefix = "assets/minecraft/textures/environment/celestial/"
        if (!resourcePath.startsWith(prefix) || !resourcePath.endsWith(".png")) return null
        return resourcePath.removePrefix(prefix).removeSuffix(".png")
    }

    private val MOON_PHASES = listOf(
        "full_moon",
        "waning_gibbous",
        "third_quarter",
        "waning_crescent",
        "new_moon",
        "waxing_crescent",
        "first_quarter",
        "waxing_gibbous"
    )
}
