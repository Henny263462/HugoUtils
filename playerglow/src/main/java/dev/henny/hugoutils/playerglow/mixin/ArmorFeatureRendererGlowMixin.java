package dev.henny.hugoutils.playerglow.mixin;

import dev.henny.hugoutils.playerglow.PlayerGlowRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererGlowMixin {
    @Final
    @Shadow
    private EquipmentRenderer equipmentRenderer;

    @Invoker("getModel")
    public abstract BipedEntityModel<?> hugoutils$getModel(BipedEntityRenderState state, EquipmentSlot slot);

    @Invoker("usesInnerModel")
    public abstract boolean hugoutils$usesInnerModel(EquipmentSlot slot);

    @Inject(method = "renderArmor", at = @At("HEAD"))
    private void hugoutils$armorGlow(
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        ItemStack stack,
        EquipmentSlot slot,
        int light,
        BipedEntityRenderState state,
        CallbackInfo ci
    ) {
        EquipmentModel.LayerType layerType = hugoutils$usesInnerModel(slot)
            ? EquipmentModel.LayerType.HUMANOID_LEGGINGS
            : EquipmentModel.LayerType.HUMANOID;
        PlayerGlowRenderer.submitArmor(
            state,
            hugoutils$getModel(state, slot),
            matrices,
            queue,
            stack,
            layerType,
            ((EquipmentRendererAccessor) equipmentRenderer).hugoutils$getEquipmentModelLoader()
        );
    }
}
