package cn.noryea.fastitems.mixin;

import cn.noryea.fastitems.FastItemsItemCapture;
import cn.noryea.fastitems.config.FastItemsConfig;
import dev.henny.hugoutils.client.config.ItemGlowFilter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackEntityRenderState.class)
@Environment(EnvType.CLIENT)
public class ItemStackEntityRenderStateMixin implements FastItemsItemCapture {

    private static final float FLAT_ITEM_DEPTH_THRESHOLD = 0.0625F;

    @Unique
    private Item fastitems$item = Items.AIR;

    @Override
    public Item fastitems$getItem() {
        return fastitems$item;
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void fastitems$flattenQuads(Entity entity, ItemStack itemStack, ItemModelManager itemModelManager, CallbackInfo ci) {
        this.fastitems$item = itemStack.isEmpty() ? Items.AIR : itemStack.getItem();
        if (!FastItemsConfig.isActive() || FastItemsConfig.renderSidesOfItems || itemStack.isEmpty()) {
            return;
        }
        if (!ItemGlowFilter.allows(FastItemsConfig.filter, this.fastitems$item)) {
            return;
        }

        ItemRenderState item = ((ItemStackEntityRenderState) (Object) this).itemRenderState;
        if (item.isEmpty() || item.getModelBoundingBox().getLengthZ() > FLAT_ITEM_DEPTH_THRESHOLD) {
            return;
        }

        ItemRenderState.LayerRenderState[] layers = ((ItemStackRenderStateAccessor) item).fastitems$getLayers();
        int layerCount = ((ItemStackRenderStateAccessor) item).fastitems$getActiveLayerCount();

        for (int i = 0; i < layerCount; i++) {
            layers[i].getQuads().removeIf(quad -> quad.face() != Direction.SOUTH);
        }
    }
}
