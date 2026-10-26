package gg.hera.fastitems.mixin;

import net.minecraft.client.util.math.MatrixStack;
import gg.hera.fastitems.FastItemsRenderer;
import gg.hera.fastitems.FastItemsState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.entity.ItemEntity;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void fastitems$prepare(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
        FastItemsRenderer.prepare(entity.getStack(), state);
    }

    /**
     * 1.21.11 uses updateRenderState -> submit -> deferred drawing. Replace only
     * vanilla's single spin rotation. Bobbing, stack count, lighting, foil, outlines
     * and the base renderer's submission remain untouched. Exact descriptors avoid
     * matching synthetic bridge methods or other renderer overloads.
     */
    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V"))
    private void fastitems$rotate(MatrixStack target, Quaternionfc vanillaSpin,
                                 ItemEntityRenderState state, MatrixStack poses,
                                 OrderedRenderCommandQueue collector, CameraRenderState camera) {
        if (((FastItemsState) state).fastitems$isBillboard()) {
            target.multiply(camera.orientation);
            // Camera.setRotation already includes PI - yaw in 1.21.11.
            // An extra Y-180 rotation would expose the culled back face.
        } else {
            target.multiply(vanillaSpin);
        }
    }
}
