package gg.hera.mixin;
import gg.hera.features.Presentation;
import net.minecraft.client.toast.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ToastManager.class)
public abstract class AdvancementToastMixin {
 @Inject(method="add",at=@At("HEAD"),cancellable=true) private void hera$toast(Toast toast,CallbackInfo ci){if(Presentation.enabled("No Advancement")&&toast instanceof AdvancementToast)ci.cancel();}
}
