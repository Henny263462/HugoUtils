package dev.henny.hugoutils.itemglow.mixin;

import dev.henny.hugoutils.itemglow.ItemGlowRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerHeldItemFeatureRenderer.class)
public class PlayerHeldItemFeatureRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void hugoutils$heldItemGlint(
        PlayerEntityRenderState state,
        ItemRenderState itemState,
        ItemStack stack,
        Arm arm,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        int light,
        CallbackInfo ci
    ) {
        ItemGlowRenderer.applyHeldGlint(stack, itemState, state.spectator);
        ItemGlowRenderer.beginHeldItem(stack, state.spectator);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void hugoutils$heldItemEnd(
        PlayerEntityRenderState state,
        ItemRenderState itemState,
        ItemStack stack,
        Arm arm,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        int light,
        CallbackInfo ci
    ) {
        ItemGlowRenderer.endHeldItem();
    }
}
