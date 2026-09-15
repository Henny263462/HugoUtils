package dev.henny.hugoutils.mixin;

import net.minecraft.client.particle.BillboardParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BillboardParticle.class)
public interface EffectsParticleAlphaAccessor {
    @Accessor("alpha")
    float hugoutils$getEffectsAlpha();

    @Accessor("alpha")
    void hugoutils$setEffectsAlpha(float alpha);
}
