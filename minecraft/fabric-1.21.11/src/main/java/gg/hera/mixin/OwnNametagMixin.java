package gg.hera.mixin;
import gg.hera.features.Esp;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PlayerEntityRenderer.class)
public abstract class OwnNametagMixin {
    @Inject(method="hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z",at=@At("RETURN"),cancellable=true)
    private void hera$ownLabel(PlayerLikeEntity entity,double distance,CallbackInfoReturnable<Boolean> cir){
        var c=MinecraftClient.getInstance();var o=Esp.options();
        if(o.ownNametag&&entity==c.player&&!c.options.hudHidden&&!c.options.getPerspective().isFirstPerson()&&!entity.isInvisible()&&(o.nametagSneaking||!entity.isSneaking()))cir.setReturnValue(true);
    }
}
