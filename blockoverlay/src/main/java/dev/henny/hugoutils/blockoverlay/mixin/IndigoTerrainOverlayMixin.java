package dev.henny.hugoutils.blockoverlay.mixin;

import dev.henny.hugoutils.blockoverlay.BlockOverlayTinter;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric API's Indigo renderer replaces vanilla BlockRenderManager meshing.
 * VertexConsumer wrapping never runs there, so overlay colors are applied on
 * the terrain quads after biome/block tint.
 */
@Mixin(value = AbstractTerrainRenderContext.class, remap = false)
public abstract class IndigoTerrainOverlayMixin {
    @Shadow
    protected BlockRenderInfo blockInfo;

    @Inject(method = "tintQuad", at = @At("RETURN"), remap = false)
    private void hugoutils$overlayTint(MutableQuadViewImpl quad, CallbackInfo ci) {
        if (blockInfo != null && blockInfo.blockState != null) {
            BlockOverlayTinter.tintQuad(quad, blockInfo.blockState);
        }
    }
}
