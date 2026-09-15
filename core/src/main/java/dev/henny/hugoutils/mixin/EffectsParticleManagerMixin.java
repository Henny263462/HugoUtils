package dev.henny.hugoutils.mixin;

import dev.henny.hugoutils.client.effects.EffectsController;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class EffectsParticleManagerMixin {
    @Inject(
        method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hugoutils$filterEffectsParticle(
        ParticleEffect effect,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ,
        CallbackInfoReturnable<Particle> cir
    ) {
        if (!EffectsController.shouldCreateParticle(effect)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(
        method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("RETURN")
    )
    private void hugoutils$configureEffectsParticle(
        ParticleEffect effect,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ,
        CallbackInfoReturnable<Particle> cir
    ) {
        Particle particle = cir.getReturnValue();
        if (particle != null) {
            EffectsController.applyParticleSettings(particle, effect);
        }
    }
}
