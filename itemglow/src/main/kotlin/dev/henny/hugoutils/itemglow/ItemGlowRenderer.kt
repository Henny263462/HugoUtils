package dev.henny.hugoutils.itemglow

import dev.henny.hugoutils.client.access.AccessFeature
import dev.henny.hugoutils.client.access.FeatureAccessManager
import dev.henny.hugoutils.client.config.ConfigManager
import dev.henny.hugoutils.client.config.GlintStyle
import dev.henny.hugoutils.client.config.GlowStyle
import dev.henny.hugoutils.client.config.ItemGlowFilter
import dev.henny.hugoutils.itemglow.mixin.ItemRenderStateAccessor
import dev.henny.hugoutils.itemglow.mixin.LayerRenderStateAccessor
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.entity.state.ItemEntityRenderState
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.item.ItemRenderState
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemDisplayContext
import net.minecraft.item.ItemStack

object ItemGlowRenderer {
    fun initialize() {
        ItemGlowRenderLayer.initialize()
        HeldGlintTexture.initialize()
    }

    @JvmStatic
    fun applyDroppedOutline(state: ItemEntityRenderState) {
        val style = ConfigManager.config.droppedItemGlow
        val item = (state as? ItemGlowItemHolder)?.`hugoutils$getGlowItem`()
        if (FeatureAccessManager.has(AccessFeature.DROPPED_ITEM_GLOW) &&
            style.enabled && !state.itemRenderState.isEmpty && ItemGlowFilter.allows(style.filter, item)
        ) {
            state.outlineColor = style.outlineArgb()
        }
    }

    @JvmStatic
    fun applyHeldGlint(stack: ItemStack, itemState: ItemRenderState, spectator: Boolean) {
        val glint = ConfigManager.config.heldGlint
        if (!FeatureAccessManager.has(AccessFeature.HELD_GLINT) ||
            !glint.enabled || spectator || stack.isEmpty || itemState.isEmpty || !ItemGlowFilter.allows(glint.filter, stack)
        ) {
            return
        }
        val accessor = itemState as ItemRenderStateAccessor
        val layers = accessor.`hugoutils$getLayers`()
        val layerCount = accessor.`hugoutils$getLayerCount`()
        for (index in 0 until layerCount) {
            val layer = layers[index]
            val current = (layer as LayerRenderStateAccessor).`hugoutils$getGlint`()
            when (glint.mode()) {
                GlintStyle.Mode.VANILLA -> if (current == ItemRenderState.Glint.NONE) {
                    layer.setGlint(ItemRenderState.Glint.STANDARD)
                }
                GlintStyle.Mode.STRONG -> if (current == ItemRenderState.Glint.NONE) {
                    layer.setGlint(ItemRenderState.Glint.SPECIAL)
                }
                GlintStyle.Mode.CUSTOM, GlintStyle.Mode.RAINBOW -> layer.setGlint(ItemRenderState.Glint.SPECIAL)
            }
        }
    }

    // Kontext des gerade rendernden Hand-Slots, gesetzt von den Held-Mixins,
    // damit der Inject in ItemRenderState.render weiß, welches Item gemeint ist.
    private var heldContextStack: ItemStack? = null
    private var heldContextSpectator: Boolean = false
    private var customGlintRenderActive = false

    @JvmStatic
    fun beginItemRender(displayContext: ItemDisplayContext, glint: ItemRenderState.Glint) {
        val held = displayContext.isFirstPerson ||
            displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ||
            displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
        val style = ConfigManager.config.heldGlint
        customGlintRenderActive = FeatureAccessManager.has(AccessFeature.HELD_GLINT) &&
            held && glint == ItemRenderState.Glint.SPECIAL && style.enabled &&
            (style.mode() == GlintStyle.Mode.CUSTOM || style.mode() == GlintStyle.Mode.RAINBOW)
    }

    @JvmStatic
    fun endItemRender() {
        customGlintRenderActive = false
    }

    @JvmStatic
    fun overrideGlintLayer(fallback: RenderLayer, translucent: Boolean): RenderLayer {
        if (!customGlintRenderActive) return fallback
        return HeldGlintTexture.layer(ConfigManager.config.heldGlint, translucent) ?: fallback
    }

    @JvmStatic
    fun beginHeldItem(stack: ItemStack, spectator: Boolean) {
        heldContextStack = stack
        heldContextSpectator = spectator
    }

    @JvmStatic
    fun endHeldItem() {
        heldContextStack = null
    }

    @JvmStatic
    fun overrideHeldOutline(outlineColor: Int): Int {
        return outlineColor
    }

    /** Keeps the actual item fully lit whenever its configured glow is active. */
    @JvmStatic
    fun overrideGlowLight(light: Int): Int {
        val held = heldContextStack
        if (held != null) {
            val style = ConfigManager.config.heldItemGlow
            if (FeatureAccessManager.has(AccessFeature.HELD_ITEM_GLOW) &&
                !heldContextSpectator && style.enabled && !held.isEmpty && ItemGlowFilter.allows(style.filter, held)
            ) {
                return LightmapTextureManager.MAX_LIGHT_COORDINATE
            }
        }
        return light
    }

    @JvmStatic
    fun submitHeldExtrasFromContext(
        itemState: ItemRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int
    ) {
        val stack = heldContextStack
        if (stack != null) {
            submitHeldExtras(stack, itemState, matrices, queue, light, overlay, heldContextSpectator)
            return
        }
    }

    private fun submitHeldExtras(
        stack: ItemStack,
        itemState: ItemRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int,
        spectator: Boolean
    ) {
        val glow = ConfigManager.config.heldItemGlow
        if (!FeatureAccessManager.has(AccessFeature.HELD_ITEM_GLOW) ||
            spectator || stack.isEmpty || itemState.isEmpty || !glow.enabled || !ItemGlowFilter.allows(glow.filter, stack)
        ) {
            return
        }
        emitGlowPass(itemState, matrices, queue, light, overlay, glow)
    }

    private fun emitGlowPass(
        itemState: ItemRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int,
        style: GlowStyle
    ) {
        val accessor = itemState as ItemRenderStateAccessor
        val layers = accessor.`hugoutils$getLayers`()
        val layerCount = accessor.`hugoutils$getLayerCount`()
        val leftHand = accessor.`hugoutils$getDisplayContext`().isLeftHand
        val glowLight = LightmapTextureManager.MAX_LIGHT_COORDINATE

        // Mehrere kreisförmige Samples überlagern sich zum weichen Vanilla-ähnlichen Rand.
        val color = style.glowArgb(0.3f)
        for (index in 0 until layerCount) {
            val layer = layers[index]
            val quads = layer.quads
            if (quads.isEmpty()) continue
            val layerAccessor = layer as LayerRenderStateAccessor

            matrices.push()
            layerAccessor.`hugoutils$getTransform`().apply(leftHand, matrices.peek())
            for (direction in 0 until ItemGlowRenderLayer.directionCount()) {
                val renderLayer = ItemGlowRenderLayer.forItemLayer(
                    layerAccessor.`hugoutils$getRenderLayer`(),
                    direction,
                    style.thicknessPixels
                ) ?: continue
                queue.submitCustom(matrices, renderLayer) { entry, consumer ->
                    for (quad in quads) {
                        ItemGlowGeometry.emit(consumer, entry, quad, color, glowLight, overlay)
                    }
                }
            }
            matrices.pop()
        }
    }

}
