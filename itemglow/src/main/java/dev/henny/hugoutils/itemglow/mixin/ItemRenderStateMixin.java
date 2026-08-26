package dev.henny.hugoutils.itemglow.mixin;

import dev.henny.hugoutils.itemglow.ItemGlowRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderState.class)
public class ItemRenderStateMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V",
        at = @At("HEAD")
    )
    private void hugoutils$heldGlow(
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        int light,
        int overlay,
        int outlineColor,
        CallbackInfo ci
    ) {
        ItemGlowRenderer.submitHeldExtrasFromContext(
            (ItemRenderState) (Object) this,
            matrices,
            queue,
            light,
            overlay
        );
    }
}
