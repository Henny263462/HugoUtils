package dev.henny.hugoutils.blockoverlay.mixin;

import dev.henny.hugoutils.blockoverlay.BlockOverlayTinter;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer",
    remap = false
)
public abstract class SodiumBlockRendererOverlayMixin {
    @Shadow(remap = false)
    protected BlockState state;

    @Inject(method = "tintQuad", at = @At("RETURN"), remap = false)
    private void hugoutils$overlay(Object quad, CallbackInfo ci) {
        if (this.state != null) {
            BlockOverlayTinter.tintSodiumQuad(quad, this.state);
        }
    }
}
