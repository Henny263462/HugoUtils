package dev.henny.hugoutils.mixin;

import dev.henny.hugoutils.client.gui.capture.GuiCaptureHooks;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(Keyboard.class)
public class GuiCaptureScreenInputMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void hugoutils$handleGuiCaptureKey(
        long window,
        int action,
        KeyInput input,
        org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        if ((action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)
            && client.currentScreen instanceof HandledScreen<?> handled
            && GuiCaptureHooks.keyPressed(handled, input)
        ) {
            ci.cancel();
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void hugoutils$handleGuiCaptureChar(
        long window,
        CharInput input,
        org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        if (client.currentScreen instanceof HandledScreen<?> handled
            && GuiCaptureHooks.charTyped(handled, input)
        ) {
            ci.cancel();
        }
    }
}
