package dev.henny.hugoutils.playerglow

import dev.henny.hugoutils.client.config.ConfigManager
import net.minecraft.client.model.Model
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.entity.equipment.EquipmentModel
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader
import net.minecraft.client.render.entity.model.BipedEntityModel
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
        val holder = state as? PlayerGlowColorHolder
        val style = ConfigManager.config.playerGlow
        if (style.enabled && !state.spectator && !state.invisible && !state.invisibleToPlayer &&
            style.playerFilter.allows(name, uuid)
        ) {
            state.outlineColor = style.playerFilter.outlineArgb(name, uuid, style)
            holder?.`hugoutils$setGlowColor`(style.playerFilter.glowArgb(name, uuid, style))
        } else {
            holder?.`hugoutils$setGlowColor`(0)
        }
    }

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
        if (state !is PlayerEntityRenderState || state.spectator) return
        if (state.invisible || state.invisibleToPlayer) return
        val color = (state as? PlayerGlowColorHolder)?.`hugoutils$getGlowColor`() ?: 0
        if (color == 0) return

        val equippable = stack.get(DataComponentTypes.EQUIPPABLE) ?: return
        val assetKey = equippable.assetId().orElse(null) ?: return
        val layers = loader.get(assetKey).getLayers(layerType)
        if (layers.isEmpty()) return

        val style = ConfigManager.config.playerGlow
        val dyeColor = DyedColorComponent.getColor(stack, 0)
        for (equipmentLayer in layers) {
            if (equipmentLayer.dyeable.isPresent && dyeColor == 0) continue
            val texture = if (equipmentLayer.usePlayerTexture) {
                state.skinTextures?.body()?.texturePath() ?: continue
            } else {
                equipmentLayer.getFullTextureId(layerType)
            }
            @Suppress("UNCHECKED_CAST")
            submitShells(model as Model<in PlayerEntityRenderState>, state, matrices, queue, texture, style.thicknessPixels, color)
        }
    }

    private fun <S : BipedEntityRenderState> submitShells(
        model: Model<in S>,
        state: S,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        texture: Identifier,
        thicknessPixels: Int,
        color: Int
    ) {
        val layer = PlayerGlowRenderLayer.forTexture(texture)
        val shells = thicknessPixels.coerceIn(1, 4)
        val extra = 0.006f + thicknessPixels * 0.012f

        for (shell in shells downTo 1) {
            val t = shell / shells.toFloat()
            val scale = 1f + extra * t

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
