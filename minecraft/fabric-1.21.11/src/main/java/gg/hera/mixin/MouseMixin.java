package gg.hera.mixin;
import gg.hera.features.CameraController;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method="onMouseButton",at=@At("HEAD"))
    private void hera$click(long window,net.minecraft.client.input.MouseInput input,int action,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){var c=net.minecraft.client.MinecraftClient.getInstance();if(action==org.lwjgl.glfw.GLFW.GLFW_PRESS&&c.currentScreen==null&&c.player!=null)gg.hera.hud.ClickMeter.INPUT.record(input.button(),System.nanoTime());}
    @Redirect(method="updateMouse",at=@At(value="INVOKE",target="Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void hera$look(ClientPlayerEntity player,double dx,double dy){if(CameraController.looking())CameraController.mouse(dx,dy);else player.changeLookDirection(dx,dy);}
}
