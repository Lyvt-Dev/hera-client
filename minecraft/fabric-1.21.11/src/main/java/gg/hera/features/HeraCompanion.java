package gg.hera.features;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;

/** Local animated 3D companion: no server entity, packets, inventory or movement changes. */
public final class HeraCompanion {
    private static volatile long audioEpoch;private static boolean active;
    private static Object world; private static Vec3d previous,position;
    private static int idle,ticks,animationAge,lastHurt;private static String animation="idle";
    public record Pose(Vec3d previous,Vec3d position,float yaw,boolean shoulder,String animation,int age){}
    public static Pose pose(){var c=MinecraftClient.getInstance();if(position==null||c.player==null||c.world!=world||!Presentation.enabled("Hera Companion")||c.options.hudHidden||(shoulder&&c.options.getPerspective().isFirstPerson()))return null;return new Pose(previous,position,c.player.getYaw(),shoulder,animation,animationAge);}
    private static int hurtTicks; private static boolean shoulder;
    private static long voiceAfter; private static String bubble="";private static long bubbleUntil;
    private static final java.util.concurrent.ExecutorService AUDIO=java.util.concurrent.Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Hera companion audio");t.setDaemon(true);return t;});
    private static volatile Clip clip;
    private HeraCompanion(){}
    public static void greet(){if(Presentation.enabled("Hera Companion")&&MinecraftClient.getInstance().player!=null)say("hello","Hi! Ich bin Hera. Auf ins Abenteuer!");}
    private static void say(String id,String caption){
        long now=System.nanoTime();if(now<voiceAfter)return;voiceAfter=now+12_000_000_000L;bubble=caption;bubbleUntil=now+4_000_000_000L;
        var c=MinecraftClient.getInstance();var o=Esp.options();
        float volume=o.companionVoice?o.companionVolume*c.options.getSoundVolume(SoundCategory.MASTER):0;
        if(volume<=0)return;
        long epoch=audioEpoch;AUDIO.execute(()->{if(epoch!=audioEpoch)return;try(var input=HeraCompanion.class.getResourceAsStream("/assets/hera/sounds/companion/"+id+".wav")){
            if(input==null)return;try(var audio=AudioSystem.getAudioInputStream(new BufferedInputStream(input))){var sound=AudioSystem.getClip();sound.open(audio);if(epoch!=audioEpoch){sound.close();return;}clip=sound;
                if(sound.isControlSupported(FloatControl.Type.MASTER_GAIN)){var gain=(FloatControl)sound.getControl(FloatControl.Type.MASTER_GAIN);gain.setValue(Math.clamp((float)(20*Math.log10(volume)),gain.getMinimum(),gain.getMaximum()));}
                sound.addLineListener(event->{if(event.getType()==LineEvent.Type.STOP){sound.close();if(clip==sound)clip=null;}});sound.start();
            }
        }catch(Exception ignored){/* Captions remain available if a platform has no audio device. */}});
    }
    public static void tick(MinecraftClient c){
        if(c.world==null||c.player==null||!Presentation.enabled("Hera Companion")){
            if(active){active=false;audioEpoch++;}world=null;position=previous=null;idle=ticks=0;bubble="";voiceAfter=0;if(clip!=null)clip.stop();return;
        }
        active=true;var p=c.player;double yaw=Math.toRadians(p.getYaw());
        boolean fresh=world!=c.world;if(fresh){world=c.world;position=previous=p.getEntityPos();idle=0;ticks=0;lastHurt=0;hurtTicks=0;}
        if(p.getVelocity().horizontalLengthSquared()<.001)idle++;else idle=0;
        boolean nextShoulder=Esp.options().companionMode==2||(Esp.options().companionMode==0&&idle>100);
        boolean changed=shoulder!=nextShoulder;shoulder=nextShoulder;ticks++;
        Vec3d target=shoulder?p.getEntityPos().add(Math.cos(yaw)*.36,p.isSneaking()?.97:1.22,Math.sin(yaw)*.36):p.getEntityPos().add(Math.sin(yaw)*1.65, .6,-Math.cos(yaw)*1.65);
        previous=position;position=position.squaredDistanceTo(target)>144?target:position.lerp(target,shoulder?.45:.22);
        if(p.hurtTime>lastHurt)hurtTicks=10;lastHurt=p.hurtTime;
        boolean hurt=hurtTicks>0;if(hurt)hurtTicks--;
        String next=hurt?"hurt":shoulder?"fly_hover":p.isTouchingWater()?"swim":p.isGliding()?"fly_glide":p.getAbilities().flying?(p.getVelocity().lengthSquared()>.01?"fly":"fly_hover"):!p.isOnGround()?"fly_hover":p.isSprinting()?"run":p.getVelocity().horizontalLengthSquared()>.001?"walk":"idle";
        if(Esp.options().companionAnimation>0)next=java.util.List.of("idle","walk","run","swim","fly_hover","fly","fly_glide","hurt").get(Esp.options().companionAnimation-1);
        if(fresh||!next.equals(animation)){animation=next;animationAge=0;}else animationAge++;
        if(fresh)say("hello","Hi! Ich bin Hera. Auf ins Abenteuer!");
        else if(changed&&shoulder)say("shoulder","Hier oben ist der beste Platz!");
        else if(changed)say("follow","Ich bin direkt hinter dir!");
    }
    public static void render(DrawContext ctx){
        var c=MinecraftClient.getInstance();if(position==null||c.player==null||c.world!=world||!Presentation.enabled("Hera Companion")||c.options.hudHidden||bubble.isEmpty()||System.nanoTime()>=bubbleUntil)return;
        // Keep shoulder captions out of the first-person view.
        if(shoulder&&c.options.getPerspective().isFirstPerson())return;
        float partial=c.gameRenderer.getCamera().getLastTickProgress();var foot=previous.lerp(position,partial);
        double bob=shoulder?0:Math.sin((ticks+partial)*.13)*.055;foot=foot.add(0,bob,0);
        var camera=c.gameRenderer.getCamera().getCameraPos();
        if(c.world.raycast(new RaycastContext(camera,foot.add(0,.35,0),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,c.player)).getType()!=HitResult.Type.MISS)return;
        var frame=WorldOverlay.frame();
        if(!bubble.isEmpty()&&System.nanoTime()<bubbleUntil)frame.label(ctx,foot.add(0,shoulder?.65:1.35,0),bubble,0xFFFFC58F);
    }
}
