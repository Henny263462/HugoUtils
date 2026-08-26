package dev.henny.hugoutils.itemglow.mixin;

import dev.henny.hugoutils.itemglow.ItemGlowRenderer;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void hugoutils$droppedItemGlow(
        ItemEntity entity,
        ItemEntityRenderState state,
        float tickProgress,
        CallbackInfo ci
    ) {
        ItemGlowRenderer.applyDroppedOutline(state);
    }
}
