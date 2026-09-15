package dev.henny.hugoutils.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface GuiCaptureHandledScreenAccessor {
    @Accessor("x")
    int hugoutils$guiCaptureX();

    @Accessor("y")
    int hugoutils$guiCaptureY();

    @Accessor("backgroundWidth")
    int hugoutils$guiCaptureBackgroundWidth();

    @Accessor("backgroundHeight")
    int hugoutils$guiCaptureBackgroundHeight();
}
