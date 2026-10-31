package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.MinecraftClient;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(InGameOverlayRenderer.class)
public abstract class FireOverlayMixin {
 @WrapMethod(method="renderFireOverlay")
 private static void hera$fire(MatrixStack matrices,VertexConsumerProvider consumers,Sprite sprite,Operation<Void> original){
  var c=MinecraftClient.getInstance();if(CameraController.controlsView()&&c.gameRenderer.getCamera().isThirdPerson())return;
  if(Presentation.enabled("Clear Lava")&&c.player!=null&&c.player.isInLava())return;
  if(!Presentation.enabled("Low Fire")){original.call(matrices,consumers,sprite);return;}
  matrices.push();try{matrices.translate(0,-Esp.options().fireHeight,0);original.call(matrices,consumers,sprite);}finally{matrices.pop();}
 }
}
