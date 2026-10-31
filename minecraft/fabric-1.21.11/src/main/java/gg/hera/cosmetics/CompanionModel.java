package gg.hera.cosmetics;
import com.google.gson.Gson;
import gg.hera.features.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Cached owner mesh and baked animation samples; one bone-pose calculation per render. */
public final class CompanionModel {
 public static final class Data {public Bone[] bones;public Map<String,Clip> clips;}
 public static final class Bone {
  public String id,name;public float[] pivot,rotation;public Bone[] bones;public Cube[] cubes;
  public transient float[] position=new float[3],angles=new float[3],fromPosition=new float[3],fromAngles=new float[3];
  public transient Quaternionf orientation=new Quaternionf();
 }
 public static final class Cube {public float[] pivot,rotation;public Quad[] quads;public transient Quaternionf orientation=new Quaternionf();public transient boolean rotated;}
 public static final class Quad {public float[] normal;public float[][] vertices;}
 public static final class Clip {public double duration;public boolean loop;public Map<String,Map<String,float[][]>> tracks;}
 private static final Identifier SKIN=Identifier.of("hera","companion/skin.png"),GLOW=Identifier.of("hera","companion/glow.png");
 private static Data data;private static final CompanionPoseTransition TRANSITION=new CompanionPoseTransition();private static final Quaternionf YAW=new Quaternionf();
 private CompanionModel(){}
 public static void loadAsync(){
  var client=MinecraftClient.getInstance();
  java.util.concurrent.CompletableFuture.supplyAsync(()->{
   try(var input=CompanionModel.class.getResourceAsStream("/assets/hera/companion/model.json")){if(input==null)throw new IOException("Missing Hera model");var value=new Gson().fromJson(new InputStreamReader(input,StandardCharsets.UTF_8),Data.class);for(var bone:value.bones)initialize(bone);return value;}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}
  }).whenComplete((value,error)->client.execute(()->{if(error!=null){org.slf4j.LoggerFactory.getLogger("Hera").warn("Companion model unavailable; other Hera modules remain available");return;}data=value;TRANSITION.reset();}));
 }
 private static void initialize(Bone bone){for(var cube:bone.cubes){cube.rotated=cube.rotation[0]!=0||cube.rotation[1]!=0||cube.rotation[2]!=0;orient(cube.orientation,cube.rotation);}for(var child:bone.bones)initialize(child);}
 private static void orient(Quaternionf quaternion,float[] degrees){quaternion.rotationZYX((float)Math.toRadians(degrees[2]),(float)Math.toRadians(degrees[1]),(float)Math.toRadians(degrees[0]));}
 private static void capture(Bone bone){System.arraycopy(bone.position,0,bone.fromPosition,0,3);System.arraycopy(bone.angles,0,bone.fromAngles,0,3);for(var child:bone.bones)capture(child);}
 private static void prepare(Bone bone,Clip clip,double time,float blend){
  var channels=clip.tracks.get(bone.id);var position=channels==null?null:channels.get("position");var rotation=channels==null?null:channels.get("rotation");
  for(int i=0;i<3;i++){float p=position==null?0:CompanionAnimationMath.sample(position,time,i),r=bone.rotation[i]+(rotation==null?0:CompanionAnimationMath.sample(rotation,time,i));bone.position[i]=CompanionAnimationMath.blend(bone.fromPosition[i],p,blend);bone.angles[i]=CompanionAnimationMath.angle(bone.fromAngles[i],r,blend);}
  orient(bone.orientation,bone.angles);for(var child:bone.bones)prepare(child,clip,time,blend);
 }
 public static void render(WorldRenderContext context){
  var pose=HeraCompanion.pose();if(pose==null||data==null)return;var mc=MinecraftClient.getInstance();float partial=Math.clamp(mc.gameRenderer.getCamera().getLastTickProgress(),0,1);
  float blend=TRANSITION.update(pose.session(),pose.revision(),pose.age(),partial,Esp.options().companionBlend);if(TRANSITION.capturePrevious())for(var bone:data.bones)capture(bone);
  var animation=data.clips.get(pose.animation());double time=(pose.age()+(double)partial)/20.;time=animation.loop?time%animation.duration:Math.min(time,animation.duration);
  for(var bone:data.bones)prepare(bone,animation,time,blend);
  var position=pose.previous().lerp(pose.position(),partial);var camera=mc.gameRenderer.getCamera().getCameraPos();var matrices=context.matrices();matrices.push();try{
   matrices.translate(position.x-camera.x,position.y-camera.y,position.z-camera.z);matrices.multiply(YAW.rotationY((float)Math.toRadians(180-pose.yaw())));float size=pose.shoulder()?.2f:.55f;matrices.scale(size,size,size);
   int light=WorldRenderer.getLightmapCoordinates(mc.world,net.minecraft.util.math.BlockPos.ofFloored(position));var solid=context.consumers().getBuffer(RenderLayers.entityCutoutNoCull(SKIN));for(var bone:data.bones)draw(bone,matrices,solid,light);
   if(Esp.options().companionGlow){var glow=context.consumers().getBuffer(RenderLayers.eyes(GLOW));for(var bone:data.bones)draw(bone,matrices,glow,LightmapTextureManager.MAX_LIGHT_COORDINATE);}
  }finally{matrices.pop();}
 }
 private static void draw(Bone bone,MatrixStack matrices,VertexConsumer consumer,int light){
  matrices.push();try{matrices.translate(bone.pivot[0]+bone.position[0]/16,bone.pivot[1]+bone.position[1]/16,bone.pivot[2]+bone.position[2]/16);matrices.multiply(bone.orientation);
   for(var cube:bone.cubes){matrices.push();try{matrices.translate(cube.pivot[0],cube.pivot[1],cube.pivot[2]);if(cube.rotated)matrices.multiply(cube.orientation);var entry=matrices.peek();for(var quad:cube.quads)for(var v:quad.vertices)consumer.vertex(entry,v[0],v[1],v[2]).color(0xFFFFFFFF).texture(v[3],v[4]).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry,quad.normal[0],quad.normal[1],quad.normal[2]);}finally{matrices.pop();}}
   for(var child:bone.bones)draw(child,matrices,consumer,light);
  }finally{matrices.pop();}
 }
}
