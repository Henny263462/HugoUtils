package dev.henny.hugoutils.itemglow.mixin;

import dev.henny.hugoutils.itemglow.ItemGlowItemHolder;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackEntityRenderState.class)
public class ItemStackEntityRenderStateMixin implements ItemGlowItemHolder {
    @Unique
    private Item hugoutils$glowItem;

    @Inject(method = "update", at = @At("TAIL"))
    private void hugoutils$captureItem(Entity entity, ItemStack stack, ItemModelManager itemModelManager, CallbackInfo ci) {
        this.hugoutils$glowItem = stack.isEmpty() ? null : stack.getItem();
    }

    @Override
    public @Nullable Item hugoutils$getGlowItem() {
        return this.hugoutils$glowItem;
    }
}
