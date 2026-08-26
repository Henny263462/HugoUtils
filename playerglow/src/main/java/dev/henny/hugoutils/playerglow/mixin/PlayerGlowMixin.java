package dev.henny.hugoutils.playerglow.mixin;

import dev.henny.hugoutils.playerglow.PlayerGlowRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class PlayerGlowMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void hugoutils$visiblePlayerGlow(
        Entity entity,
        EntityRenderState state,
        float tickProgress,
        CallbackInfo ci
    ) {
        if (entity instanceof PlayerEntity player && state instanceof PlayerEntityRenderState playerState) {
            PlayerGlowRenderer.applyOutline(playerState, player.getName().getString(), player.getUuid());
        }
    }
}
