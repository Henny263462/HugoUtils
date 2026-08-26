package cn.noryea.fastitems.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
@Environment(EnvType.CLIENT)
public interface ItemStackRenderStateAccessor {

    @Accessor("layers")
    ItemRenderState.LayerRenderState[] fastitems$getLayers();

    @Accessor("layerCount")
    int fastitems$getActiveLayerCount();
}
