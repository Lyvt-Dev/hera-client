package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntityRenderer.class)
public abstract class HitColorMixin {
 @Inject(method="getOverlay",at=@At("RETURN"),cancellable=true) private static void hera$overlay(LivingEntityRenderState state,float white,CallbackInfoReturnable<Integer> ci){if(Presentation.enabled("Hit Color")&&state.hurt)ci.setReturnValue(net.minecraft.client.render.OverlayTexture.DEFAULT_UV);}
 @Inject(method="getMixColor",at=@At("RETURN"),cancellable=true) private void hera$color(LivingEntityRenderState state,CallbackInfoReturnable<Integer> ci){if(Presentation.enabled("Hit Color")&&state.hurt)ci.setReturnValue(Esp.options().hitColor);}
}
