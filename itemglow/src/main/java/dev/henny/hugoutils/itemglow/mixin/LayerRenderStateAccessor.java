package dev.henny.hugoutils.itemglow.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.json.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("transform")
    Transformation hugoutils$getTransform();

    @Accessor("glint")
    ItemRenderState.Glint hugoutils$getGlint();

    @Accessor("renderLayer")
    RenderLayer hugoutils$getRenderLayer();
}
