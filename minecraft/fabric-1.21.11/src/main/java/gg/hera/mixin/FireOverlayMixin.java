package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameOverlayRenderer.class)
public abstract class FireOverlayMixin {
 @Inject(method="renderFireOverlay",at=@At("HEAD"),cancellable=true) private static void hera$fire(MatrixStack m,VertexConsumerProvider v,Sprite sprite,CallbackInfo ci){var p=MinecraftClient.getInstance().player;if(Presentation.enabled("Clear Lava")&&p!=null&&p.isInLava()){ci.cancel();return;}if(Presentation.enabled("Low Fire")){m.push();m.translate(0,-Esp.options().fireHeight,0);}}
 @Inject(method="renderFireOverlay",at=@At("RETURN")) private static void hera$restore(MatrixStack m,VertexConsumerProvider v,Sprite sprite,CallbackInfo ci){if(Presentation.enabled("Low Fire"))m.pop();}
}
