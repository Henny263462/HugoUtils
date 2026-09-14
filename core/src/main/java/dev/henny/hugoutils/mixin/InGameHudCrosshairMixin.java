package dev.henny.hugoutils.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.henny.hugoutils.client.input.PerspectiveController;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameHud.class)
public class InGameHudCrosshairMixin {
    @ModifyExpressionValue(
        method = "renderCrosshair",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z"
        )
    )
    private boolean hugoutils$showCrosshairInThirdPerson(boolean original) {
        return PerspectiveController.shouldShowCrosshair(original);
    }
}
