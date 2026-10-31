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
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(ItemEntityRenderer.class)
public abstract class ItemPhysicsMixin {
 @Inject(method="updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V",at=@At("RETURN"))
 private void hera$state(ItemEntity entity,ItemEntityRenderState state,float delta,CallbackInfo ci){((GroundedItemState)state).hera$grounded(entity.isOnGround());if(Presentation.enabled("Item Physics")&&entity.isOnGround()&&Esp.options().itemPhysicsFlat)state.age=0;}
 @WrapMethod(method="render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V")
 private void hera$physics(ItemEntityRenderState state,MatrixStack matrices,OrderedRenderCommandQueue queue,CameraRenderState camera,Operation<Void> original){
  if(!Presentation.enabled("Item Physics")){original.call(state,matrices,queue,camera);return;}
  var o=Esp.options();boolean grounded=((GroundedItemState)state).hera$grounded();
  matrices.push();try{if(grounded&&o.itemPhysicsFlat)matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(90));else if(!grounded&&o.itemPhysicsSpeed>0)matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(state.age*7*o.itemPhysicsSpeed));original.call(state,matrices,queue,camera);}finally{matrices.pop();}
 }
}
