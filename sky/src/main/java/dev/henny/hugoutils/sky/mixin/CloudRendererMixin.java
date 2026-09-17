package dev.henny.hugoutils.sky.mixin;

import dev.henny.hugoutils.sky.SkyTextureOverrides;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;

@Mixin(CloudRenderer.class)
public class CloudRendererMixin {
    @Redirect(
        method = "prepare",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/resource/ResourceManager;open(Lnet/minecraft/util/Identifier;)Ljava/io/InputStream;"
        )
    )
    private InputStream hugoutils$openCloudTexture(ResourceManager manager, Identifier id) throws IOException {
        InputStream override = SkyTextureOverrides.cloudStreamOrNull();
        return override != null ? override : manager.open(id);
    }
}
