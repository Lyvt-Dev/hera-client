package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
public abstract class CrosshairMixin {
    @Inject(method="renderCrosshair",at=@At("HEAD"),cancellable=true)
    private void hera$crosshair(DrawContext context,RenderTickCounter ticks,CallbackInfo ci){
        var c=MinecraftClient.getInstance();var o=Esp.options();
        if(!o.customCrosshair||c.player==null||c.player.isSpectator()||!c.options.getPerspective().isFirstPerson()||c.options.hudHidden)return;
        CustomCrosshair.draw(context,context.getScaledWindowWidth()/2,context.getScaledWindowHeight()/2,o);ci.cancel();
    }
}
