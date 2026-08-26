package dev.henny.hugoutils.itemglow.mixin;

import dev.henny.hugoutils.itemglow.HeldGlintTexture;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.SequencedMap;

@Mixin(BufferBuilderStorage.class)
public abstract class BufferBuilderStorageMixin {
    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/VertexConsumerProvider;immediate(Ljava/util/SequencedMap;Lnet/minecraft/client/util/BufferAllocator;)Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"
        ),
        index = 0
    )
    private SequencedMap<RenderLayer, BufferAllocator> hugoutils$registerColoredGlintBuffers(
        SequencedMap<RenderLayer, BufferAllocator> buffers
    ) {
        return HeldGlintTexture.registerBuffers(buffers);
    }
}
