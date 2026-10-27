package gg.hera.mixin;
import gg.hera.cosmetics.LocalCape;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractClientPlayerEntity.class)
public abstract class LocalCapeMixin {
    @Inject(method="getSkin",at=@At("RETURN"),cancellable=true)
    private void hera$cape(CallbackInfoReturnable<SkinTextures> ci){ci.setReturnValue(gg.hera.cosmetics.SharedCapes.apply((AbstractClientPlayerEntity)(Object)this,LocalCape.apply((AbstractClientPlayerEntity)(Object)this,ci.getReturnValue())));}
}
