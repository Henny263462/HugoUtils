package dev.henny.hugoutils.blockoverlay.mixin;

import dev.henny.hugoutils.blockoverlay.BlockOverlayTinter;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FluidRenderer.class)
public class FluidRendererOverlayMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private VertexConsumer hugoutils$wrap(
        VertexConsumer consumer,
        BlockRenderView world,
        BlockPos pos,
        VertexConsumer ignored,
        BlockState state,
        FluidState fluidState
    ) {
        return BlockOverlayTinter.wrap(consumer, state);
    }
}
