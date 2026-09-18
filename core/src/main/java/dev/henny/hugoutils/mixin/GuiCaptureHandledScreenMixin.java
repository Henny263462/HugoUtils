package dev.henny.hugoutils.mixin;

import dev.henny.hugoutils.client.gui.capture.GuiCaptureHooks;
import dev.henny.hugoutils.client.rtp.RtpShare;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

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

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void hugoutils$observeRtpClick(
        Click click,
        boolean doubled,
        CallbackInfoReturnable<Boolean> cir
    ) {
        RtpShare.observeHandledClick((HandledScreen<?>) (Object) this, click);
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void hugoutils$observeRtpSlot(
        Slot slot,
        int slotId,
        int button,
        SlotActionType actionType,
        CallbackInfo ci
    ) {
        RtpShare.observeSlotClick((HandledScreen<?>) (Object) this, slot);
    }

}
