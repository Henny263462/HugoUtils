package dev.henny.hugoutils.mixin;

import dev.henny.hugoutils.client.gui.capture.GuiCaptureHooks;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class GuiCaptureHandledScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void hugoutils$renderGuiCapture(
        DrawContext context,
        int mouseX,
        int mouseY,
        float deltaTicks,
        CallbackInfo ci
    ) {
        GuiCaptureHooks.render((HandledScreen<?>) (Object) this, context, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void hugoutils$handleGuiCaptureClick(
        Click click,
        boolean doubled,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (GuiCaptureHooks.mouseClicked((HandledScreen<?>) (Object) this, click)) {
            cir.setReturnValue(true);
        }
    }

}
