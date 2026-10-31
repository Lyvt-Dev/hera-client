package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(HeldItemRenderer.class)
public abstract class ItemModelMixin {
 @WrapMethod(method="renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V")
 private void hera$model(LivingEntity entity,ItemStack item,ItemDisplayContext context,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,Operation<Void> original){
  boolean model=Presentation.enabled("Item Model"),legacy=Presentation.enabled("Old Animations");
  if(!context.isFirstPerson()||!model&&!legacy){original.call(entity,item,context,matrices,queue,light);return;}
  var o=Esp.options();float side=context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND?-1:1;
  matrices.push();try{
   if(model){matrices.translate(o.itemX*(o.itemMirrorLeft?side:1),o.itemY,o.itemZ);matrices.scale(o.itemScale,o.itemScale,o.itemScale);}
   if(legacy&&o.legacyStrength>0){float swing=Math.clamp(entity.getHandSwingProgress(net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickProgress()),0,1);if(swing>0){matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees((float)Math.sin(Math.sqrt(swing)*Math.PI)*-25*side*o.legacyStrength));matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float)Math.sin(swing*swing*Math.PI)*-20*side*o.legacyStrength));}}
   original.call(entity,item,context,matrices,queue,light);
  }finally{matrices.pop();}
 }
}
