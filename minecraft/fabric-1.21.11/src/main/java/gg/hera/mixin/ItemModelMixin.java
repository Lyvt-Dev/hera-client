package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HeldItemRenderer.class)
public abstract class ItemModelMixin {
 @Inject(method="renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",at=@At("HEAD"))
 private void hera$model(LivingEntity entity,ItemStack item,ItemDisplayContext context,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,CallbackInfo ci){if(!context.isFirstPerson())return;if(Presentation.enabled("Item Model")||Presentation.enabled("Old Animations")){matrices.push();var o=Esp.options();if(Presentation.enabled("Item Model")){matrices.translate(o.itemX,o.itemY,o.itemZ);matrices.scale(o.itemScale,o.itemScale,o.itemScale);}if(Presentation.enabled("Old Animations")){float swing=entity.getHandSwingProgress(net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera().getLastTickProgress());matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees((float)Math.sin(Math.sqrt(swing)*Math.PI)*-25));matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float)Math.sin(swing*swing*Math.PI)*-20));}}}
 @Inject(method="renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",at=@At("RETURN"))
 private void hera$restore(LivingEntity entity,ItemStack item,ItemDisplayContext context,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,CallbackInfo ci){if(context.isFirstPerson()&&(Presentation.enabled("Item Model")||Presentation.enabled("Old Animations")))matrices.pop();}
}
