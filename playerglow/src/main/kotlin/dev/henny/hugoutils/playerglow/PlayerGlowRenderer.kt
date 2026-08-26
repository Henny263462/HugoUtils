package dev.henny.hugoutils.playerglow

import dev.henny.hugoutils.client.access.AccessFeature
import dev.henny.hugoutils.client.access.FeatureAccessManager
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.GlowStyle
import net.minecraft.client.model.Model
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.entity.equipment.EquipmentModel
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader
import net.minecraft.client.render.entity.model.BipedEntityModel
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.render.entity.state.BipedEntityRenderState
import net.minecraft.client.render.entity.state.PlayerEntityRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import java.util.UUID

object PlayerGlowRenderer {
    fun initialize() = Unit

    @JvmStatic
    fun applyOutline(state: PlayerEntityRenderState, name: String, uuid: UUID) {
        val style = ConfigManager.config.playerGlow
        if (FeatureAccessManager.has(AccessFeature.PLAYER_GLOW) &&
            style.enabled && !state.spectator && !state.invisible && !state.invisibleToPlayer &&
            style.playerFilter.allows(name, uuid)
        ) {
            state.outlineColor = style.outlineArgb()
        }
    }

    /**
     * Renders glow shells for an armor piece. Called at the head of
     * ArmorFeatureRenderer#renderArmor, so the actual armor piece is submitted
     * afterwards and covers the shell's center, just like the body does.
     */
    @JvmStatic
    fun submitArmor(
        state: BipedEntityRenderState,
        model: BipedEntityModel<*>,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        stack: ItemStack,
        layerType: EquipmentModel.LayerType,
        loader: EquipmentModelLoader
    ) {
        val style = ConfigManager.config.playerGlow
        if (!FeatureAccessManager.has(AccessFeature.PLAYER_GLOW) ||
            !style.enabled || state.invisible || state.invisibleToPlayer
        ) return
        if (state !is PlayerEntityRenderState || state.spectator) return

        val equippable = stack.get(DataComponentTypes.EQUIPPABLE) ?: return
        val assetKey = equippable.assetId().orElse(null) ?: return
        val layers = loader.get(assetKey).getLayers(layerType)
        if (layers.isEmpty()) return

        val dyeColor = DyedColorComponent.getColor(stack, 0)
        for (equipmentLayer in layers) {
            // Same rule as vanilla: undyed dyeable layers are skipped entirely.
            if (equipmentLayer.dyeable.isPresent && dyeColor == 0) continue
            val texture = if (equipmentLayer.usePlayerTexture) {
                state.skinTextures?.body()?.texturePath() ?: continue
            } else {
                equipmentLayer.getFullTextureId(layerType)
            }
            @Suppress("UNCHECKED_CAST")
            submitShells(model as Model<in PlayerEntityRenderState>, state, matrices, queue, texture, style)
        }
    }

    private fun <S : BipedEntityRenderState> submitShells(
        model: Model<in S>,
        state: S,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        texture: Identifier,
        style: GlowStyle
    ) {
        val layer = PlayerGlowRenderLayer.forTexture(texture)
        val shells = style.thicknessPixels.coerceIn(1, 4)
        val extra = 0.006f + style.thicknessPixels * 0.012f

        // The vanilla player/armor model is submitted immediately afterwards and covers the
        // shell's center. Only back faces outside its silhouette remain, with world depth active.
        for (shell in shells downTo 1) {
            val t = shell / shells.toFloat()
            val scale = 1f + extra * t
            val color = style.glowArgb()

            matrices.push()
            matrices.translate(0f, 0.75f, 0f)
            matrices.scale(scale, scale, scale)
            matrices.translate(0f, -0.75f, 0f)
            queue.submitModel(
                model,
                state,
                matrices,
                layer,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                color,
                null
            )
            matrices.pop()
        }
    }
}
