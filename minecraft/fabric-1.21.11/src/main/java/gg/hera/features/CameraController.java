package gg.hera.features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

/** Local camera only: never teleports or replaces the network player. */
public final class CameraController {
    public static KeyBinding zoomKey,lookKey,camKey;
    private static boolean active;private static Object world;
    public static float yaw,pitch;public static Vec3d position=Vec3d.ZERO;
    private CameraController(){}
    public static boolean detached(){return active;}
    public static boolean looking(){var c=MinecraftClient.getInstance();return c.player!=null && c.currentScreen==null && (active || Esp.options().freeLook && lookKey!=null && lookKey.isPressed());}
    public static boolean zooming(){var c=MinecraftClient.getInstance();return Esp.options().zoom && zoomKey!=null && zoomKey.isPressed() && c.currentScreen==null && c.world!=null;}
    private static boolean wasLooking;
    public static void tick(MinecraftClient c){
        if(c.world!=world || c.player==null || !c.player.isAlive()){active=false;Esp.options().freecam=false;wasLooking=false;world=c.world;}
        if(camKey!=null)while(camKey.wasPressed()){if(c.player!=null && c.currentScreen==null)Esp.options().freecam=!Esp.options().freecam;}
        boolean desired=Esp.options().freecam && c.player!=null;
        if(desired && !active){position=c.gameRenderer.getCamera().getCameraPos();yaw=c.player.getYaw();pitch=c.player.getPitch();}
        active=desired;
        boolean look=looking();if(look && !wasLooking && !active){yaw=c.player.getYaw();pitch=c.player.getPitch();}wasLooking=look;
        if(!active || c.currentScreen!=null || !c.isWindowFocused())return;
        double forward=(c.options.forwardKey.isPressed()?1:0)-(c.options.backKey.isPressed()?1:0),side=(c.options.rightKey.isPressed()?1:0)-(c.options.leftKey.isPressed()?1:0),up=(c.options.jumpKey.isPressed()?1:0)-(c.options.sneakKey.isPressed()?1:0);
        double rad=Math.toRadians(yaw),speed=Esp.options().freecamSpeed*(c.options.sprintKey.isPressed()?3:1);
        var delta=new Vec3d(-Math.sin(rad)*forward+Math.cos(rad)*side,up,Math.cos(rad)*forward+Math.sin(rad)*side);
        if(delta.lengthSquared()>1)delta=delta.normalize();position=position.add(delta.multiply(speed));
    }
    public static void mouse(double dx,double dy){yaw+=(float)(dx*.15);pitch=MathHelper.clamp(pitch+(float)(dy*.15),-90,90);}
}
