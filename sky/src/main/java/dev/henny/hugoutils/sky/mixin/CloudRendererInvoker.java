package dev.henny.hugoutils.sky.mixin;

import net.minecraft.client.render.CloudRenderer;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(CloudRenderer.class)
public interface CloudRendererInvoker {
    @Invoker("prepare")
    Optional<CloudRenderer.CloudCells> hugoutils$prepare(ResourceManager manager, Profiler profiler);

    @Invoker("apply")
    void hugoutils$apply(
        Optional<CloudRenderer.CloudCells> cells,
        ResourceManager manager,
        Profiler profiler
    );
}
