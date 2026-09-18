package dev.henny.hugoutils.blockoverlay.mixin;

import dev.henny.hugoutils.blockoverlay.BlockOverlayTinter;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer",
    remap = false
)
public abstract class SodiumFluidRendererOverlayMixin {
    @Shadow(remap = false)
    private int[] quadColors;

    @Unique
    private FluidState hugoutils$fluid;

    @Inject(method = "updateQuad", at = @At("HEAD"), remap = false)
    private void hugoutils$capture(
        @Coerce Object quad,
        @Coerce Object level,
        @Coerce Object pos,
        @Coerce Object lighter,
        @Coerce Object dir,
        @Coerce Object facing,
        float brightness,
        @Coerce Object colorProvider,
        @Coerce Object fluidState,
        CallbackInfo ci
    ) {
        this.hugoutils$fluid = fluidState instanceof FluidState fluid ? fluid : null;
    }

    @Inject(method = "updateQuad", at = @At("RETURN"), remap = false)
    private void hugoutils$overlay(CallbackInfo ci) {
        if (this.quadColors != null && this.hugoutils$fluid != null) {
            BlockOverlayTinter.tintAbgrColors(this.quadColors, this.hugoutils$fluid.getBlockState());
        }
    }
}
