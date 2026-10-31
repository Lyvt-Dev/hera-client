package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
public abstract class CrosshairMixin {
 @Shadow @Final private static Identifier CROSSHAIR_TEXTURE;
 @Inject(method="renderCrosshair",at=@At("HEAD"),cancellable=true)
 private void hera$camera(DrawContext context,RenderTickCounter ticks,CallbackInfo ci){var c=MinecraftClient.getInstance();if(c.world!=null&&CameraController.controlsView()&&c.gameRenderer.getCamera().isThirdPerson())ci.cancel();}
 @WrapOperation(method="renderCrosshair",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
 private void hera$crosshair(DrawContext context,RenderPipeline pipeline,Identifier texture,int x,int y,int width,int height,Operation<Void> original){
  var c=MinecraftClient.getInstance();var o=Esp.options();if(CROSSHAIR_TEXTURE.equals(texture)&&o.customCrosshair&&c.player!=null&&!c.player.isSpectator()&&!c.options.hudHidden&&!c.gameRenderer.getCamera().isThirdPerson())CustomCrosshair.draw(context,context.getScaledWindowWidth()/2,context.getScaledWindowHeight()/2,o);else original.call(context,pipeline,texture,x,y,width,height);
 }
}
