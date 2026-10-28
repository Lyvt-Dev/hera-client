package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.particle.*;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ParticleManager.class)
public abstract class PresentationParticleMixin {
 @Inject(method="addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",at=@At("HEAD"),cancellable=true)
 private void hera$filter(ParticleEffect type,double x,double y,double z,double vx,double vy,double vz,CallbackInfoReturnable<Particle> ci){if(Presentation.enabled("Particle Settings")&&Esp.options().hiddenParticles.contains(Registries.PARTICLE_TYPE.getId(type.getType()).toString()))ci.setReturnValue(null);}
}
