package gg.hera.mixin;
import gg.hera.features.CameraController;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean thirdPerson;
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow protected abstract void setPos(double x,double y,double z);
    @Shadow private float clipToSpace(float distance){throw new AssertionError();}
    @Shadow protected abstract void moveBy(float x,float y,float z);
    // Preserve Vanilla's interpolated focal position, including crouch/vehicle offsets.
    @ModifyVariable(method="update",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private boolean hera$baseCamera(boolean thirdPerson){return CameraController.controlsView()?false:thirdPerson;}
    @Inject(method="update",at=@At("TAIL"))
    private void hera$camera(World world,Entity entity,boolean thirdPerson,boolean inverse,float delta,CallbackInfo ci){
        if(CameraController.controlsView()){
            this.thirdPerson=true;boolean front=inverse&&!CameraController.detached();setRotation(CameraController.yaw+(front?180:0),front?-CameraController.pitch:CameraController.pitch);
            if(CameraController.detached()){var p=CameraController.renderPosition(delta);setPos(p.x,p.y,p.z);}
            else {moveBy(-clipToSpace(gg.hera.features.Esp.options().lookDistance),0,0);}
        }
    }
}
