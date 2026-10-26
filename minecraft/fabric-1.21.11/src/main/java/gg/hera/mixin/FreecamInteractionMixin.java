package gg.hera.mixin;
import gg.hera.features.CameraController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(MinecraftClient.class)
public abstract class FreecamInteractionMixin {
    @Inject(method="doAttack",at=@At("HEAD"),cancellable=true)
    private void hera$attack(CallbackInfoReturnable<Boolean> cir){if(CameraController.detached())cir.setReturnValue(false);}
    @Inject(method="doItemUse",at=@At("HEAD"),cancellable=true)
    private void hera$use(CallbackInfo ci){if(CameraController.detached())ci.cancel();}
    @Inject(method="handleBlockBreaking",at=@At("HEAD"),cancellable=true)
    private void hera$break(boolean breaking,CallbackInfo ci){if(CameraController.detached())ci.cancel();}
}
