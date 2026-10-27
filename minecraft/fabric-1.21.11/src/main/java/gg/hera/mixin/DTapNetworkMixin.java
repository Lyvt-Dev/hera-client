package gg.hera.mixin;
import gg.hera.features.DTap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayNetworkHandler.class)
public abstract class DTapNetworkMixin {
    @Inject(method="onEntityDamage",at=@At("TAIL")) private void hera$damage(EntityDamageS2CPacket p,CallbackInfo ci){if(DTap.instance!=null)DTap.instance.damage(p.entityId(),p.sourceCauseId());}
    @Inject(method="onBlockUpdate",at=@At("TAIL")) private void hera$block(BlockUpdateS2CPacket p,CallbackInfo ci){if(DTap.instance!=null)DTap.instance.blockUpdate(p.getPos(),p.getState());}
    @Inject(method="onChunkDeltaUpdate",at=@At("TAIL")) private void hera$delta(ChunkDeltaUpdateS2CPacket p,CallbackInfo ci){if(DTap.instance!=null)p.visitUpdates(DTap.instance::blockUpdate);}
    @Inject(method="onEntityStatus",at=@At("TAIL")) private void hera$status(EntityStatusS2CPacket p,CallbackInfo ci){var c=MinecraftClient.getInstance();if(DTap.instance!=null && c.world!=null)DTap.instance.entityStatus(p.getEntity(c.world),p.getStatus());}
    @Inject(method="onEntitiesDestroy",at=@At("TAIL")) private void hera$removed(EntitiesDestroyS2CPacket p,CallbackInfo ci){if(DTap.instance!=null)for(int id:p.getEntityIds())DTap.instance.removed(id);}
}
