package gg.hera.features;
import net.minecraft.util.math.Vec3d;
/** Tick endpoints only; render sampling never changes simulation state. */
public final class CameraPath {
 private Vec3d previous=Vec3d.ZERO,current=Vec3d.ZERO;
 public void reset(Vec3d position){previous=current=position;}
 public void beginTick(){previous=current;}
 public void move(Vec3d delta){current=current.add(delta);}
 public Vec3d sample(float partial){return previous.lerp(current,Float.isFinite(partial)?Math.clamp(partial,0,1):0);}
}
