package gg.hera.features;
import gg.hera.mixin.GameRendererAccessor;
import gg.hera.ui.HeraTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import org.joml.Quaternionf;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.*;

/** Bounded projected overlays using the actual camera and current zoom FOV. */
public final class WorldOverlay {
    private final MinecraftClient c=MinecraftClient.getInstance();
    private final Vec3d origin;private final Quaternionf inverse;private final Matrix4f projection;private final int w,h;
    public WorldOverlay(){var camera=c.gameRenderer.getCamera();origin=camera.getCameraPos();inverse=new Quaternionf(camera.getRotation()).conjugate();projection=c.gameRenderer.getBasicProjectionMatrix(((GameRendererAccessor)c.gameRenderer).hera$fov(camera,c.gameRenderer.getCamera().getLastTickProgress(),true));w=c.getWindow().getScaledWidth();h=c.getWindow().getScaledHeight();}
    private Vector4f clip(Vec3d p){var v=new Vector3f((float)(p.x-origin.x),(float)(p.y-origin.y),(float)(p.z-origin.z));inverse.transform(v);return projection.transform(new Vector4f(v,1));}
    public float[] point(Vec3d p){var v=clip(p);if(v.w<=.05f)return null;float x=(v.x/v.w+1)*w/2,y=(1-v.y/v.w)*h/2;return Float.isFinite(x)&&Float.isFinite(y)?new float[]{x,y}:null;}
    private static void line2(DrawContext ctx,float x,float y,float ex,float ey,int color,int thickness){
        float dx=ex-x,dy=ey-y,length=(float)Math.hypot(dx,dy);if(length<.01f)return;
        ctx.getMatrices().pushMatrix();ctx.getMatrices().translate(x,y);ctx.getMatrices().rotate((float)Math.atan2(dy,dx));ctx.getMatrices().translate(0,-thickness/2f);ctx.getMatrices().scale(length,thickness);ctx.fill(0,0,1,1,color);ctx.getMatrices().popMatrix();
    }
    public void line(DrawContext ctx,Vec3d a,Vec3d b,int color,int thickness){
        var av=clip(a);var bv=clip(b);float near=.05f;if(av.w<near && bv.w<near)return;
        if(av.w<near)av.lerp(bv,(near-av.w)/(bv.w-av.w));if(bv.w<near)bv.lerp(av,(near-bv.w)/(av.w-bv.w));
        float[] clipped=OverlayPolicy.clip((av.x/av.w+1)*w/2,(1-av.y/av.w)*h/2,(bv.x/bv.w+1)*w/2,(1-bv.y/bv.w)*h/2,w,h);
        if(clipped!=null)line2(ctx,clipped[0],clipped[1],clipped[2],clipped[3],color,thickness);
    }
    public void box(DrawContext ctx,Box b,int color,int thickness){
        Vec3d[] p=new Vec3d[8];for(int i=0;i<8;i++)p[i]=new Vec3d((i&1)==0?b.minX:b.maxX,(i&2)==0?b.minY:b.maxY,(i&4)==0?b.minZ:b.maxZ);
        for(int i=0;i<8;i++)for(int bit:new int[]{1,2,4})if((i&bit)==0)line(ctx,p[i],p[i|bit],color,thickness);
    }
    public void tracer(DrawContext ctx,Vec3d p,int color,int thickness){var target=point(p);if(target==null)return;var segment=OverlayPolicy.clip(w/2f,Esp.options().tracerCenter?h/2f:h-2,target[0],target[1],w,h);if(segment!=null)line2(ctx,segment[0],segment[1],segment[2],segment[3],color,thickness);}
    public void label(DrawContext ctx,Vec3d pos,String label,int color){var p=point(pos);if(p==null || p[0]<0 || p[0]>w || p[1]<0 || p[1]>h)return;int tw=c.textRenderer.getWidth(HeraTheme.text(label)),x=Math.clamp(Math.round(p[0])-tw/2,4,Math.max(4,w-tw-8)),y=Math.clamp(Math.round(p[1]),4,Math.max(4,h-20));HeraTheme.round(ctx,x-4,y-3,tw+8,15,4,0xDD171C27);HeraTheme.label(ctx,label,x,y,color);}
    public static void render(DrawContext ctx){
        var c=MinecraftClient.getInstance();var f=Esp.config;var o=Esp.options();
        if(f==null || c.world==null || c.player==null || c.options.hudHidden)return;
        if(!o.hitboxes && !o.tntTimer && !f.itemEsp && !f.playerEsp && !f.mobEsp)return;
        var overlay=new WorldOverlay();var entities=new ArrayList<Entity>();
        for(var e:c.world.getEntities())if(e!=c.player && !e.isRemoved() && e.squaredDistanceTo(c.player)<=(double)f.espRange*f.espRange && (Esp.marked(e)||o.hitboxes && hitboxTarget(e)||o.tntTimer && (e instanceof TntEntity || e instanceof net.minecraft.entity.vehicle.TntMinecartEntity)))entities.add(e);
        entities.sort(Comparator.<Entity>comparingDouble(e->e.squaredDistanceTo(c.player)).thenComparingInt(Entity::getId));
        float tick=c.gameRenderer.getCamera().getLastTickProgress();
        for(int i=0;i<Math.min(128,entities.size());i++){
            var e=entities.get(i);boolean esp=Esp.marked(e);var position=e.getLerpedPos(tick);var box=e.getBoundingBox().offset(position.subtract(e.getEntityPos()));
            if(esp && o.espBoxes)overlay.box(ctx,box,o.espColor,o.lineWidth);
            if(esp && o.espTracers)overlay.tracer(ctx,position.add(0,e.getHeight()*.5,0),o.espColor,o.lineWidth);
            if(!(esp && o.espBoxes) && o.hitboxes && hitboxTarget(e))overlay.box(ctx,box,o.hitboxColor,o.hitboxWidth);
            int fuse=e instanceof TntEntity tnt?tnt.getFuse():e instanceof net.minecraft.entity.vehicle.TntMinecartEntity cart?cart.getFuseTicks():-1;
            if(o.tntTimer && fuse>=0)overlay.label(ctx,position.add(0,e.getHeight()+.3,0),String.format(Locale.ROOT,"TNT · %.1f s",Math.max(0,fuse-tick)/20f),o.tntColor);
        }
    }
    private static boolean hitboxTarget(Entity e){var o=Esp.options();return o.hitboxPlayers && e instanceof PlayerEntity || o.hitboxMobs && e instanceof MobEntity || o.hitboxItems && e instanceof ItemEntity;}
}
