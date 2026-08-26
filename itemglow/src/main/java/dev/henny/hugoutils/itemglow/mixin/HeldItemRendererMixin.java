package dev.henny.hugoutils.itemglow.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.henny.hugoutils.itemglow.ItemGlowRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @WrapOperation(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V"
        )
    )
    private void hugoutils$heldItemGlint(
        ItemRenderState state,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        int light,
        int overlay,
        int outlineColor,
        Operation<Void> original,
        LivingEntity entity,
        ItemStack stack,
        ItemDisplayContext displayContext,
        MatrixStack unusedMatrices,
        OrderedRenderCommandQueue unusedQueue,
        int unusedLight
    ) {
        boolean spectator = entity.isSpectator();
        ItemGlowRenderer.applyHeldGlint(stack, state, spectator);
        ItemGlowRenderer.beginHeldItem(stack, spectator);
        try {
            original.call(state, matrices, queue, light, overlay, outlineColor);
        } finally {
            ItemGlowRenderer.endHeldItem();
        }
    }
}
