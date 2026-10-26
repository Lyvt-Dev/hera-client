package gg.hera.mixin;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("getFov") float hera$fov(Camera camera,float tickProgress,boolean changingFov);
}
