package gg.hera.mixin;
import gg.hera.features.Esp;
import net.minecraft.world.World;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(World.class)
public abstract class WorldVisualMixin {
    private boolean hera$client(){return (Object)this instanceof ClientWorld;}
    @Inject(method="getTimeOfDay",at=@At("HEAD"),cancellable=true)
    private void hera$time(CallbackInfoReturnable<Long> cir){if(hera$client() && Esp.options().timeChanger)cir.setReturnValue((long)Esp.options().worldTime);}
    @Inject(method="getRainGradient",at=@At("HEAD"),cancellable=true)
    private void hera$rain(float delta,CallbackInfoReturnable<Float> cir){if(hera$client() && Esp.options().weatherChanger)cir.setReturnValue(Esp.options().weather==0?0f:1f);}
    @Inject(method="getThunderGradient",at=@At("HEAD"),cancellable=true)
    private void hera$thunder(float delta,CallbackInfoReturnable<Float> cir){if(hera$client() && Esp.options().weatherChanger)cir.setReturnValue(Esp.options().weather==2?1f:0f);}
}
