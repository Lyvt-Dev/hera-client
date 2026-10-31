package gg.hera.cosmetics;

import com.google.gson.Gson;
import gg.hera.features.HeraCompanion;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Owner-provided Blockbench mesh, immutable baked animation data, no expression execution. */
public final class CompanionModel {
 public static final class Data {public Bone[] bones;public Map<String,Clip> clips;}
 public static final class Bone {public String id,name;public float[] pivot,rotation;public Bone[] bones;public Cube[] cubes;}
 public static final class Cube {public float[] pivot,rotation;public Quad[] quads;}
 public static final class Quad {public float[] normal;public float[][] vertices;}
 public static final class Clip {public double duration;public boolean loop;public Map<String,Map<String,float[][]>> tracks;}
 private static final Identifier SKIN=Identifier.of("hera","companion/skin.png"),GLOW=Identifier.of("hera","companion/glow.png");
 private static Data data;
 private CompanionModel(){}
 public static void load(){try(var in=CompanionModel.class.getResourceAsStream("/assets/hera/companion/model.json")){if(in==null)throw new IOException("Missing Hera model");data=new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Data.class);}catch(IOException e){throw new IllegalStateException("Hera model could not load",e);}}
 public static void render(WorldRenderContext context){
  var pose=HeraCompanion.pose();if(pose==null||data==null)return;
  var mc=MinecraftClient.getInstance();float partial=mc.gameRenderer.getCamera().getLastTickProgress();var position=pose.previous().lerp(pose.position(),partial);var camera=mc.gameRenderer.getCamera().getCameraPos();
  var matrices=context.matrices();matrices.push();try{
   matrices.translate(position.x-camera.x,position.y-camera.y,position.z-camera.z);
   matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180-pose.yaw()));float size=pose.shoulder()?.2f:.55f;matrices.scale(size,size,size);
   var animation=data.clips.get(pose.animation());double time=(pose.age()+partial)/20.;if(animation.loop)time%=animation.duration;else time=Math.min(time,animation.duration);
   int light=WorldRenderer.getLightmapCoordinates(mc.world,net.minecraft.util.math.BlockPos.ofFloored(position));
   var solid=context.consumers().getBuffer(RenderLayers.entityCutoutNoCull(SKIN));for(var bone:data.bones)draw(bone,animation,time,matrices,solid,light);
   var glow=context.consumers().getBuffer(RenderLayers.eyes(GLOW));for(var bone:data.bones)draw(bone,animation,time,matrices,glow,LightmapTextureManager.MAX_LIGHT_COORDINATE);
  }finally{matrices.pop();}
 }
 private static void rotation(MatrixStack m,float[] r){m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(r[2]));m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(r[1]));m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(r[0]));}
 private static float value(float[][] samples,double time,int axis){double frame=Math.max(0,Math.min(samples.length-1,time*60));int a=(int)frame,b=Math.min(a+1,samples.length-1);return (float)(samples[a][axis]+(samples[b][axis]-samples[a][axis])*(frame-a));}
 private static void draw(Bone b,Clip clip,double time,MatrixStack m,VertexConsumer out,int light){
  m.push();try{m.translate(b.pivot[0],b.pivot[1],b.pivot[2]);var channels=clip.tracks.get(b.id);
   if(channels!=null){var pos=channels.get("position");if(pos!=null)m.translate(value(pos,time,0)/16,value(pos,time,1)/16,value(pos,time,2)/16);var rot=channels.get("rotation");if(rot!=null){m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(b.rotation[2]+value(rot,time,2)));m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(b.rotation[1]+value(rot,time,1)));m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(b.rotation[0]+value(rot,time,0)));}else rotation(m,b.rotation);}else rotation(m,b.rotation);
   for(var cube:b.cubes){m.push();try{m.translate(cube.pivot[0],cube.pivot[1],cube.pivot[2]);rotation(m,cube.rotation);var entry=m.peek();for(var quad:cube.quads)for(var v:quad.vertices)out.vertex(entry,v[0],v[1],v[2]).color(0xFFFFFFFF).texture(v[3],v[4]).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry,quad.normal[0],quad.normal[1],quad.normal[2]);}finally{m.pop();}}
   for(var child:b.bones)draw(child,clip,time,m,out,light);
  }finally{m.pop();}
 }
}
