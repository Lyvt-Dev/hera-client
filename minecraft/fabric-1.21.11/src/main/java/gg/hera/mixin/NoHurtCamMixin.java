package gg.hera.mixin;
import gg.hera.features.Presentation;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class NoHurtCamMixin {
 @Inject(method="tiltViewWhenHurt",at=@At("HEAD"),cancellable=true) private void hera$hurt(MatrixStack m,float delta,CallbackInfo ci){if(Presentation.enabled("No Hurt Cam")||gg.hera.features.CameraController.detached())ci.cancel();}
 @Inject(method="bobView",at=@At("HEAD"),cancellable=true) private void hera$bob(MatrixStack m,float delta,CallbackInfo ci){if(gg.hera.features.CameraController.detached())ci.cancel();}
 @Inject(method="shouldRenderBlockOutline",at=@At("HEAD"),cancellable=true) private void hera$outline(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){if(Presentation.enabled("Block Outlines")||gg.hera.features.CameraController.detached())ci.setReturnValue(false);}
}
