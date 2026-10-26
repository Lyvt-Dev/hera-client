package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GameRenderer.class)
public abstract class ZoomMixin {
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true)
    private void hera$zoom(Camera camera,float delta,boolean changing,CallbackInfoReturnable<Float> cir){if(CameraController.zooming())cir.setReturnValue(cir.getReturnValue()/Esp.options().zoomFactor);}
}
