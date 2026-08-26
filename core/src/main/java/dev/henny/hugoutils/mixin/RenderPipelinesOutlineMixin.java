package dev.henny.hugoutils.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.henny.hugoutils.client.render.DepthTestedOutlinePipelines;
import net.minecraft.client.gl.RenderPipelines;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPipelines.class)
public class RenderPipelinesOutlineMixin {
    @Mutable
    @Final
    @Shadow
    public static RenderPipeline OUTLINE_CULL;

    @Mutable
    @Final
    @Shadow
    public static RenderPipeline OUTLINE_NO_CULL;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void hugoutils$useDepthTestedOutlines(CallbackInfo ci) {
        OUTLINE_CULL = DepthTestedOutlinePipelines.CULL;
        OUTLINE_NO_CULL = DepthTestedOutlinePipelines.NO_CULL;
    }
}
