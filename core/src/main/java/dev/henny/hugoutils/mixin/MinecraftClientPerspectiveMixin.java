package dev.henny.hugoutils.mixin;

import dev.henny.hugoutils.client.input.PerspectiveController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientPerspectiveMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void hugoutils$customPerspectiveCycle(CallbackInfo ci) {
        PerspectiveController.handlePendingPresses((MinecraftClient) (Object) this);
    }
}
