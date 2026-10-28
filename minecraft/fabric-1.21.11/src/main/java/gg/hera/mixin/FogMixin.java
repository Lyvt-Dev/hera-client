package gg.hera.mixin;
import gg.hera.features.Esp;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(FogRenderer.class)
public abstract class FogMixin {
    @ModifyVariable(method="getFogBuffer",at=@At("HEAD"),argsOnly=true)
    private FogRenderer.FogType hera$fog(FogRenderer.FogType type){var o=Esp.options();if((!o.noFog&&!gg.hera.features.Presentation.enabled("Clear Lava")) || net.minecraft.client.MinecraftClient.getInstance().world==null)return type;var camera=net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera();var submerged=camera.getSubmersionType();if(gg.hera.features.Presentation.enabled("Clear Lava")&&submerged==net.minecraft.block.enums.CameraSubmersionType.LAVA)return FogRenderer.FogType.NONE;if(!o.noFog)return type;if(submerged==net.minecraft.block.enums.CameraSubmersionType.WATER && !o.fogWater || submerged==net.minecraft.block.enums.CameraSubmersionType.LAVA && !o.fogLava)return type;return FogRenderer.FogType.NONE;}
}
