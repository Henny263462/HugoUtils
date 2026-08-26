package cn.noryea.fastitems.mixin;

import cn.noryea.fastitems.FastItemsItemCapture;
import cn.noryea.fastitems.config.FastItemsConfig;
import dev.henny.hugoutils.client.DroppedItemRenderHooks;
import dev.henny.hugoutils.client.config.ItemGlowFilter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemEntityRenderer.class, priority = 900)
@Environment(EnvType.CLIENT)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {

    private static final float FLAT_ITEM_DEPTH_THRESHOLD = 0.0625F;

    @Final
    @Shadow
    private Random random;

    protected ItemEntityRendererMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void fastitems$billboardSubmit(ItemEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (!FastItemsConfig.isActive() || state.itemRenderState.isEmpty()) {
            return;
        }
        if (state instanceof FastItemsItemCapture capture &&
            !ItemGlowFilter.allows(FastItemsConfig.filter, capture.fastitems$getItem())) {
            return;
        }

        Box box = state.itemRenderState.getModelBoundingBox();
        boolean is3D = box.getLengthZ() > FLAT_ITEM_DEPTH_THRESHOLD;
        if (is3D && !FastItemsConfig.affect3DModels) {
            return;
        }

        matrices.push();
        float f = -((float) box.minY) + FLAT_ITEM_DEPTH_THRESHOLD;
        float g = MathHelper.sin(state.age / 10.0F + state.uniqueOffset) * 0.1F + 0.1F;
        matrices.translate(0.0F, g + f, 0.0F);
        matrices.multiply(cameraState.orientation);
        DroppedItemRenderHooks.begin(state);
        try {
            ItemEntityRenderer.render(matrices, queue, state.light, state, this.random, box);
        } finally {
            DroppedItemRenderHooks.end();
        }
        matrices.pop();

        super.render(state, matrices, queue, cameraState);
        ci.cancel();
    }

    @Override
    protected float getShadowRadius(ItemEntityRenderState state) {
        boolean active = FastItemsConfig.isActive() &&
            (!(state instanceof FastItemsItemCapture capture) ||
                ItemGlowFilter.allows(FastItemsConfig.filter, capture.fastitems$getItem()));
        return active && !FastItemsConfig.castShadows ? 0.0F : super.getShadowRadius(state);
    }
}
