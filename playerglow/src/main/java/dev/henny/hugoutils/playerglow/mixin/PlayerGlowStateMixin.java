package dev.henny.hugoutils.playerglow.mixin;

import dev.henny.hugoutils.playerglow.PlayerGlowColorHolder;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public class PlayerGlowStateMixin implements PlayerGlowColorHolder {
    @Unique
    private int hugoutils$glowColor;

    @Override
    public int hugoutils$getGlowColor() {
        return this.hugoutils$glowColor;
    }

    @Override
    public void hugoutils$setGlowColor(int color) {
        this.hugoutils$glowColor = color;
    }
}
