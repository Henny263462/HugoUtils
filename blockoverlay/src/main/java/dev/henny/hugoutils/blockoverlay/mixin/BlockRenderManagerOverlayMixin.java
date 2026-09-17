package dev.henny.hugoutils.blockoverlay.mixin;

import dev.henny.hugoutils.blockoverlay.BlockOverlayTinter;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(BlockRenderManager.class)
public class BlockRenderManagerOverlayMixin {
    @ModifyVariable(
        method = "renderBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLjava/util/List;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private VertexConsumer hugoutils$tintSolid(
        VertexConsumer consumer,
        BlockState state,
        BlockPos pos,
        BlockRenderView world,
        MatrixStack matrices,
        VertexConsumer ignored,
        boolean cull,
        List<BlockModelPart> parts
    ) {
        return BlockOverlayTinter.wrap(consumer, state);
    }

    @ModifyVariable(
        method = "renderFluid(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private VertexConsumer hugoutils$tintFluid(
        VertexConsumer consumer,
        BlockPos pos,
        BlockRenderView world,
        VertexConsumer ignored,
        BlockState state,
        FluidState fluidState
    ) {
        return BlockOverlayTinter.wrap(consumer, state);
    }
}
