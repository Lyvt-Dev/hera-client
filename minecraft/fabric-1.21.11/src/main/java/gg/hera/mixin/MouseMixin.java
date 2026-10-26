package gg.hera.mixin;
import gg.hera.features.CameraController;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Redirect(method="updateMouse",at=@At(value="INVOKE",target="Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void hera$look(ClientPlayerEntity player,double dx,double dy){if(CameraController.looking())CameraController.mouse(dx,dy);else player.changeLookDirection(dx,dy);}
}
