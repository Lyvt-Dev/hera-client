package gg.hera.mixin;
import gg.hera.features.Presentation;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayNetworkHandler.class)
public abstract class PresentationNetworkMixin {
 @Inject(method="onWorldTimeUpdate",at=@At("TAIL")) private void hera$time(WorldTimeUpdateS2CPacket packet,CallbackInfo ci){Presentation.time(packet.time(),packet.timeOfDay());}
 @Inject(method="onEntityDamage",at=@At("TAIL")) private void hera$damage(EntityDamageS2CPacket packet,CallbackInfo ci){Presentation.damage(packet.entityId(),packet.sourceCauseId());}
}
