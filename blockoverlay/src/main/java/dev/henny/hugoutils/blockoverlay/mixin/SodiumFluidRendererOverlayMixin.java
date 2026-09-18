package dev.henny.hugoutils.blockoverlay.mixin;

import dev.henny.hugoutils.blockoverlay.BlockOverlayTinter;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer",
    remap = false
)
public abstract class SodiumFluidRendererOverlayMixin {
    @Shadow(remap = false)
    private int[] quadColors;

    @Inject(method = "updateQuad", at = @At("RETURN"), remap = false)
    private void hugoutils$overlay(
        Object quad,
        Object level,
        Object pos,
        Object lighter,
        Object dir,
        Object facing,
        float brightness,
        Object colorProvider,
        FluidState fluidState,
        CallbackInfo ci
    ) {
        if (this.quadColors != null && fluidState != null) {
            BlockOverlayTinter.tintAbgrColors(this.quadColors, fluidState.getBlockState());
        }
    }
}
