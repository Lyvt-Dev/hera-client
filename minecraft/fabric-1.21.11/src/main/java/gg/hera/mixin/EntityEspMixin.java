package gg.hera.mixin;

import gg.hera.features.Esp;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityEspMixin {
    @Inject(method="isGlowing",at=@At("HEAD"),cancellable=true)
    private void hera$glow(CallbackInfoReturnable<Boolean> cir){if(Esp.glowing((Entity)(Object)this))cir.setReturnValue(true);}
    @Inject(method="getTeamColorValue",at=@At("HEAD"),cancellable=true)
    private void hera$color(CallbackInfoReturnable<Integer> cir){if(Esp.glowing((Entity)(Object)this))cir.setReturnValue(Esp.options().espColor & 0xFFFFFF);}
}
