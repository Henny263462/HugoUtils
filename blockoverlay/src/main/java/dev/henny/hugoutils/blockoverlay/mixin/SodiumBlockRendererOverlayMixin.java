package dev.henny.hugoutils.blockoverlay.mixin;

import dev.henny.hugoutils.blockoverlay.BlockOverlayTinter;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer",
    remap = false
)
public abstract class SodiumBlockRendererOverlayMixin {
    @Unique
    private BlockState hugoutils$state;

    @Inject(method = "renderModel", at = @At("HEAD"), remap = false)
    private void hugoutils$capture(
        @Coerce Object model,
        BlockState state,
        @Coerce Object pos,
        @Coerce Object origin,
        CallbackInfo ci
    ) {
        this.hugoutils$state = state;
    }

    @Inject(method = "bufferQuad", at = @At("HEAD"), remap = false)
    private void hugoutils$overlay(@Coerce Object quad, float[] brightnesses, @Coerce Object material, CallbackInfo ci) {
        if (this.hugoutils$state != null) {
            BlockOverlayTinter.tintSodiumQuad(quad, this.hugoutils$state);
        }
    }
}
