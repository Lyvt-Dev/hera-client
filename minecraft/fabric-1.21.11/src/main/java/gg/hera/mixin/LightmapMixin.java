package gg.hera.mixin;
import gg.hera.features.Esp;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LightmapTextureManager.class)
public abstract class LightmapMixin {
    @Redirect(method="update",at=@At(value="INVOKE",target="Lnet/minecraft/world/dimension/DimensionType;ambientLight()F"))
    private float hera$bright(DimensionType dimension){return Esp.options().fullBright?Esp.options().brightness:dimension.ambientLight();}
    @Inject(method="getDarkness",at=@At("HEAD"),cancellable=true)
    private void hera$dark(net.minecraft.entity.LivingEntity player,float factor,float delta,CallbackInfoReturnable<Float> cir){if(Esp.options().fullBright)cir.setReturnValue(0f);}
}
