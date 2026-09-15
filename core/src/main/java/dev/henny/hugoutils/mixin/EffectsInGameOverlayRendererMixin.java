package dev.henny.hugoutils.mixin;

import dev.henny.hugoutils.client.effects.EffectsController;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(InGameOverlayRenderer.class)
public class EffectsInGameOverlayRendererMixin {
    @ModifyConstant(
        method = "renderFireOverlay",
        constant = @Constant(floatValue = 0.9F),
        expect = 4
    )
    private static float hugoutils$scaleFireOpacity(float vanillaOpacity) {
        return vanillaOpacity * EffectsController.fireScale();
    }
}
