package gg.hera.features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

/** Local camera only: no network-player position or inventory mutation. */
public final class CameraController {
 public static KeyBinding zoomKey,lookKey,camKey;
 private static boolean active,wasLooking;private static final CameraSession SESSION=new CameraSession();private static float frameZoomScale=1;
 public static float yaw,pitch;
 private static final CameraPath PATH=new CameraPath();private static final ZoomTransition ZOOM=new ZoomTransition();
 private CameraController(){}
 private static boolean valid(MinecraftClient c){return c.world!=null&&c.player!=null&&c.player.isAlive();}
 public static boolean detached(){var c=MinecraftClient.getInstance();return active&&Esp.options().freecam&&valid(c)&&SESSION.matches(c.world,c.player);}
 public static boolean looking(){
  var c=MinecraftClient.getInstance();boolean look=valid(c)&&c.currentScreen==null&&c.isWindowFocused()&&(detached()||Esp.options().freeLook&&lookKey!=null&&lookKey.isPressed());
  if(look&&!wasLooking&&!detached()){yaw=c.player.getYaw();pitch=c.player.getPitch();}wasLooking=look;return look;
 }
 /** Detached view remains in place behind menus; input remains focus-gated. */
 public static boolean controlsView(){var c=MinecraftClient.getInstance();return valid(c)&&(detached()||looking());}
 public static Vec3d renderPosition(float partial){return PATH.sample(partial);}
 public static boolean zooming(){var c=MinecraftClient.getInstance();return valid(c)&&Esp.options().zoom&&zoomKey!=null&&zoomKey.isPressed()&&c.currentScreen==null&&c.isWindowFocused();}
 public static float zoomScale(){return frameZoomScale;}
 public static void beginFrame(){var c=MinecraftClient.getInstance();if(!valid(c)||!SESSION.matches(c.world,c.player)){ZOOM.reset();frameZoomScale=1;return;}float amount=ZOOM.sample(zooming(),Esp.options().zoomSmooth,System.nanoTime());frameZoomScale=1+(Esp.options().zoomFactor-1)*amount;}
 public static void tick(MinecraftClient c){
  if(SESSION.update(c.world,c.player,c.player!=null&&c.player.isAlive())){active=false;Esp.options().freecam=false;wasLooking=false;ZOOM.reset();frameZoomScale=1;}
  PATH.beginTick();
  if(camKey!=null)while(camKey.wasPressed())if(valid(c)&&c.currentScreen==null&&c.isWindowFocused())Esp.options().freecam=!Esp.options().freecam;
  boolean desired=Esp.options().freecam&&valid(c);
  if(desired&&!active){var camera=c.gameRenderer.getCamera();PATH.reset(camera.getCameraPos());yaw=camera.getYaw();pitch=camera.getPitch();}
  active=desired;looking();
  if(!active||c.currentScreen!=null||!c.isWindowFocused())return;
  double forward=(c.options.forwardKey.isPressed()?1:0)-(c.options.backKey.isPressed()?1:0),side=(c.options.rightKey.isPressed()?1:0)-(c.options.leftKey.isPressed()?1:0),up=(c.options.jumpKey.isPressed()?1:0)-(c.options.sneakKey.isPressed()?1:0);
  double radians=Math.toRadians(yaw),speed=Esp.options().freecamSpeed*(c.options.sprintKey.isPressed()?3:1);
  var delta=new Vec3d(-Math.sin(radians)*forward+Math.cos(radians)*side,up,Math.cos(radians)*forward+Math.sin(radians)*side);
  if(delta.lengthSquared()>1)delta=delta.normalize();if(delta.lengthSquared()>0)PATH.move(delta.multiply(speed));
 }
 public static void mouse(double dx,double dy){if(!Double.isFinite(dx)||!Double.isFinite(dy))return;float nextYaw=yaw+(float)(dx*.15),nextPitch=pitch+(float)(dy*.15);if(!Float.isFinite(nextYaw)||!Float.isFinite(nextPitch))return;yaw=MathHelper.wrapDegrees(nextYaw);pitch=MathHelper.clamp(nextPitch,-90,90);}
}
