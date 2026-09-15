package dev.henny.hugoutils.mixin;

import dev.henny.hugoutils.client.input.PerspectiveController;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererNameMixin {
    @Inject(method = "hasLabel", at = @At("RETURN"), cancellable = true)
    private void hugoutils$showOwnNameInThirdPerson(
        PlayerLikeEntity player,
        double squaredDistanceToCamera,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (PerspectiveController.shouldShowOwnName(player)) {
            cir.setReturnValue(true);
        }
    }
}
