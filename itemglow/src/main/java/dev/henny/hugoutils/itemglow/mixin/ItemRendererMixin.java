package dev.henny.hugoutils.itemglow.mixin;

import dev.henny.hugoutils.itemglow.ItemGlowRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private static void hugoutils$beginColoredGlint(
        ItemDisplayContext displayContext,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        int overlay,
        int[] tints,
        List<BakedQuad> quads,
        RenderLayer renderLayer,
        ItemRenderState.Glint glint,
        CallbackInfo ci
    ) {
        ItemGlowRenderer.beginItemRender(displayContext, glint);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private static void hugoutils$endColoredGlint(
        ItemDisplayContext displayContext,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        int overlay,
        int[] tints,
        List<BakedQuad> quads,
        RenderLayer renderLayer,
        ItemRenderState.Glint glint,
        CallbackInfo ci
    ) {
        ItemGlowRenderer.endItemRender();
    }

    @Redirect(
        method = "getSpecialItemGlintConsumer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayers;glint()Lnet/minecraft/client/render/RenderLayer;")
    )
    private static RenderLayer hugoutils$coloredGlint() {
        return ItemGlowRenderer.overrideGlintLayer(RenderLayers.glint(), false);
    }

    @Redirect(
        method = "getSpecialItemGlintConsumer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayers;glintTranslucent()Lnet/minecraft/client/render/RenderLayer;")
    )
    private static RenderLayer hugoutils$coloredTranslucentGlint() {
        return ItemGlowRenderer.overrideGlintLayer(RenderLayers.glintTranslucent(), true);
    }
}
