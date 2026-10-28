package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntityRenderer.class)
public abstract class ItemPhysicsMixin {
 @Inject(method="updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",at=@At("RETURN")) private void hera$state(ItemEntity entity,ItemEntityRenderState state,float delta,CallbackInfo ci){((GroundedItemState)state).hera$grounded(entity.isOnGround());if(Presentation.enabled("Item Physics")&&entity.isOnGround())state.age=0;}
 @Inject(method="render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",at=@At("HEAD")) private void hera$physics(ItemEntityRenderState state,MatrixStack matrices,OrderedRenderCommandQueue q,CameraRenderState camera,CallbackInfo ci){if(Presentation.enabled("Item Physics")){matrices.push();boolean ground=((GroundedItemState)state).hera$grounded();matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(ground?90:state.age*7));}}
 @Inject(method="render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",at=@At("RETURN")) private void hera$restore(ItemEntityRenderState state,MatrixStack matrices,OrderedRenderCommandQueue q,CameraRenderState camera,CallbackInfo ci){if(Presentation.enabled("Item Physics"))matrices.pop();}
}
