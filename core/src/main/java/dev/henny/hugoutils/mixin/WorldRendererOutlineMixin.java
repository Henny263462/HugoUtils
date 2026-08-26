package dev.henny.hugoutils.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererOutlineMixin {
    @Final
    @Shadow
    private MinecraftClient client;

    @Shadow
    private Framebuffer entityOutlineFramebuffer;

    @Inject(
        method = "method_62214",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V",
            shift = At.Shift.AFTER
        )
    )
    private void hugoutils$copyTerrainDepthBeforeEntities(CallbackInfo ci) {
        if (entityOutlineFramebuffer != null) {
            entityOutlineFramebuffer.copyDepthFrom(client.getFramebuffer());
        }
    }
}
