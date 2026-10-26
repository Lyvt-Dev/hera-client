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
    @Shadow protected abstract float clipToSpace(float distance);
    @Shadow protected abstract void moveBy(float x,float y,float z);
    @Inject(method="update",at=@At("TAIL"))
    private void hera$camera(World world,Entity entity,boolean thirdPerson,boolean inverse,float delta,CallbackInfo ci){
        if(CameraController.looking()){
            thirdPerson=true;setRotation(CameraController.yaw,CameraController.pitch);
            if(CameraController.detached()){var p=CameraController.position;setPos(p.x,p.y,p.z);}
            else {setPos(entity.getX(),entity.getEyeY(),entity.getZ());moveBy(-clipToSpace(gg.hera.features.Esp.options().lookDistance),0,0);}
        }
    }
}
