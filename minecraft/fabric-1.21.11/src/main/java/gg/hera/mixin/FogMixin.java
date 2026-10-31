package gg.hera.mixin;
import gg.hera.features.Esp;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(FogRenderer.class)
public abstract class FogMixin {
    @ModifyVariable(method="getFogBuffer",at=@At("HEAD"),argsOnly=true)
    private FogRenderer.FogType hera$fog(FogRenderer.FogType type){
        var c=net.minecraft.client.MinecraftClient.getInstance();if(c.world==null)return type;var o=Esp.options();boolean clear=gg.hera.features.Presentation.enabled("Clear Lava");if(!o.noFog&&!clear)return type;
        var medium=switch(c.gameRenderer.getCamera().getSubmersionType()){case WATER->gg.hera.features.WorldVisualPolicy.Medium.WATER;case LAVA->gg.hera.features.WorldVisualPolicy.Medium.LAVA;case NONE->gg.hera.features.WorldVisualPolicy.Medium.AIR;default->gg.hera.features.WorldVisualPolicy.Medium.POWDER_SNOW;};
        return gg.hera.features.WorldVisualPolicy.removeFog(o,clear,medium)?FogRenderer.FogType.NONE:type;
    }
}
