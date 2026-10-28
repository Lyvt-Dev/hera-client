package gg.hera.mixin;
import gg.hera.features.CameraController;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class InputMixin extends Input {
    @Inject(method="tick",at=@At("TAIL"))
    private void hera$input(CallbackInfo ci){if(CameraController.detached()){playerInput=PlayerInput.DEFAULT;movementVector=Vec2f.ZERO;return;}if(gg.hera.features.Presentation.sprintRequested()){var p=playerInput;playerInput=new PlayerInput(p.forward(),p.backward(),p.left(),p.right(),p.jump(),p.sneak(),true);}}
}
