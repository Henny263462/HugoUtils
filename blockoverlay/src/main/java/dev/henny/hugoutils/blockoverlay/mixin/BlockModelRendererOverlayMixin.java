package dev.henny.hugoutils.blockoverlay.mixin;

import dev.henny.hugoutils.blockoverlay.BlockOverlayTinter;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(BlockModelRenderer.class)
public class BlockModelRendererOverlayMixin {
    @ModifyVariable(
        method = "render(Lnet/minecraft/world/BlockRenderView;Ljava/util/List;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZI)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private VertexConsumer hugoutils$wrap(
        VertexConsumer consumer,
        BlockRenderView world,
        List<?> parts,
        BlockState state,
        BlockPos pos,
        MatrixStack matrices,
        VertexConsumer ignored,
        boolean cull,
        int overlay
    ) {
        return BlockOverlayTinter.wrap(consumer, state);
    }
}
