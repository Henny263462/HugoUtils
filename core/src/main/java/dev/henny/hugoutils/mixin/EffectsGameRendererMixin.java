package dev.henny.hugoutils.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.henny.hugoutils.client.config.EffectsConfig;
import dev.henny.hugoutils.client.effects.EffectsController;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class EffectsGameRendererMixin {
    @ModifyExpressionValue(
        method = "renderWorld",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;lastNauseaIntensity:F"
        )
    )
    private float hugoutils$scalePreviousPortalEffect(float original) {
        return original * EffectsConfig.INSTANCE.getPortalScale();
    }

    @ModifyExpressionValue(
        method = "renderWorld",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;nauseaIntensity:F"
        )
    )
    private float hugoutils$scalePortalEffect(float original) {
        return original * EffectsConfig.INSTANCE.getPortalScale();
    }

    @ModifyExpressionValue(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F"
        )
    )
    private float hugoutils$scaleNauseaEffect(float original) {
        return original * EffectsConfig.INSTANCE.getNauseaScale();
    }

    @ModifyExpressionValue(
        method = "renderWorld",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/GameRenderer;nauseaEffectTime:F"
        )
    )
    private float hugoutils$scaleNauseaWobbleTime(float original) {
        return original * EffectsController.nauseaWobbleScale();
    }

    @ModifyExpressionValue(
        method = "renderWorld",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/GameRenderer;nauseaEffectSpeed:F"
        )
    )
    private float hugoutils$scaleNauseaWobbleSpeed(float original) {
        return original * EffectsController.nauseaWobbleScale();
    }
}
