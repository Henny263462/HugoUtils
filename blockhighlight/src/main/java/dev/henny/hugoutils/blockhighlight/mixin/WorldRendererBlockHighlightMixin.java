package dev.henny.hugoutils.blockhighlight.mixin;

import dev.henny.hugoutils.blockhighlight.BlockHighlightRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererBlockHighlightMixin {
    @Inject(method = "pushEntityRenders", at = @At("TAIL"))
    private void hugoutils$submitTargetBlockHighlight(
        MatrixStack matrices,
        WorldRenderState renderStates,
        OrderedRenderCommandQueue queue,
        CallbackInfo ci
    ) {
        BlockHighlightRenderer.submitTarget(matrices, renderStates, queue);
    }
}
