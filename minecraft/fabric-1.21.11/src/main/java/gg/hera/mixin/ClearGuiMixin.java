package gg.hera.mixin;
import gg.hera.features.Presentation;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
public abstract class ClearGuiMixin {
 @Inject(method="renderInGameBackground",at=@At("HEAD"),cancellable=true) private void hera$background(DrawContext c,CallbackInfo ci){if(Presentation.enabled("Clear GUI")&&MinecraftClient.getInstance().world!=null)ci.cancel();}
}
