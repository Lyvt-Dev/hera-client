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
    private Vec3d origin;private final Quaternionf inverse=new Quaternionf();private final Matrix4f projection=new Matrix4f();private int w,h;
    private final Vector3f vector=new Vector3f();private final Vector4f aClip=new Vector4f(),bClip=new Vector4f();
    private final Vector4f[] corners=new Vector4f[8];private final float[] segment=new float[4],screen=new float[2];
    private record Label(net.minecraft.text.Text text,int width){}
    private final Map<String,Label> labels=new LinkedHashMap<>(256,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<String,Label> entry){return size()>256;}};
    private static final WorldOverlay SHARED=new WorldOverlay();
    private static List<Entity> targets=List.of();private static Object targetWorld;private static int refresh;
    private static final Set<Entity> targetSet=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final NearestTargets<Entity> nearest=new NearestTargets<>(128,e->e.squaredDistanceTo(MinecraftClient.getInstance().player),Entity::getId);
    public WorldOverlay(){for(int i=0;i<8;i++)corners[i]=new Vector4f();}
    public static WorldOverlay frame(){SHARED.begin();return SHARED;}
    private void begin(){var camera=c.gameRenderer.getCamera();origin=camera.getCameraPos();inverse.set(camera.getRotation()).conjugate();projection.set(c.gameRenderer.getBasicProjectionMatrix(((GameRendererAccessor)c.gameRenderer).hera$fov(camera,camera.getLastTickProgress(),true)));w=c.getWindow().getScaledWidth();h=c.getWindow().getScaledHeight();}
    private Vector4f clip(double x,double y,double z,Vector4f out){vector.set((float)(x-origin.x),(float)(y-origin.y),(float)(z-origin.z));inverse.transform(vector);return projection.transform(out.set(vector,1));}
    public float[] point(Vec3d p){var v=clip(p.x,p.y,p.z,aClip);if(v.w<=.05f)return null;float x=(v.x/v.w+1)*w/2,y=(1-v.y/v.w)*h/2;if(!Float.isFinite(x)||!Float.isFinite(y))return null;screen[0]=x;screen[1]=y;return screen;}
    private static void line2(DrawContext ctx,float x,float y,float ex,float ey,int color,int thickness){
        float dx=ex-x,dy=ey-y,length=(float)Math.hypot(dx,dy);if(length<.01f)return;
        ctx.getMatrices().pushMatrix();ctx.getMatrices().translate(x,y);ctx.getMatrices().rotate((float)Math.atan2(dy,dx));ctx.getMatrices().translate(0,-thickness/2f);ctx.getMatrices().scale(length,thickness);ctx.fill(0,0,1,1,color);ctx.getMatrices().popMatrix();
    }
    public void line(DrawContext ctx,Vec3d a,Vec3d b,int color,int thickness){
        lineClip(ctx,clip(a.x,a.y,a.z,aClip),clip(b.x,b.y,b.z,bClip),color,thickness);
    }
    private void lineClip(DrawContext ctx,Vector4f a,Vector4f b,int color,int thickness){
        float ax=a.x,ay=a.y,aw=a.w,bx=b.x,by=b.y,bw=b.w,near=.05f;if(aw<near&&bw<near)return;
        if(aw<near){float t=(near-aw)/(bw-aw);ax+=(bx-ax)*t;ay+=(by-ay)*t;aw=near;}
        if(bw<near){float t=(near-bw)/(aw-bw);bx+=(ax-bx)*t;by+=(ay-by)*t;bw=near;}
        if(OverlayPolicy.clipInto((ax/aw+1)*w/2,(1-ay/aw)*h/2,(bx/bw+1)*w/2,(1-by/bw)*h/2,w,h,segment))line2(ctx,segment[0],segment[1],segment[2],segment[3],color,thickness);
    }
    public void box(DrawContext ctx,Box b,int color,int thickness){
        int outside=31;for(int i=0;i<8;i++){var p=clip((i&1)==0?b.minX:b.maxX,(i&2)==0?b.minY:b.maxY,(i&4)==0?b.minZ:b.maxZ,corners[i]);int mask=(p.w<.05f?1:0)|(p.x< -p.w?2:0)|(p.x>p.w?4:0)|(p.y< -p.w?8:0)|(p.y>p.w?16:0);outside&=mask;}if(outside!=0)return;
        for(int i=0;i<8;i++)for(int bit=1;bit<=4;bit<<=1)if((i&bit)==0)lineClip(ctx,corners[i],corners[i|bit],color,thickness);
    }
    public void tracer(DrawContext ctx,Vec3d p,int color,int thickness){var target=point(p);if(target==null)return;if(OverlayPolicy.clipInto(w/2f,Esp.options().tracerCenter?h/2f:h-2,target[0],target[1],w,h,segment))line2(ctx,segment[0],segment[1],segment[2],segment[3],color,thickness);}
    public void label(DrawContext ctx,Vec3d pos,String label,int color){var p=point(pos);if(p==null || p[0]<0 || p[0]>w || p[1]<0 || p[1]>h)return;var value=labels.computeIfAbsent(label,s->{var text=HeraTheme.text(s);return new Label(text,HeraTheme.measure(text,false));});int tw=value.width,x=Math.clamp(Math.round(p[0])-tw/2,4,Math.max(4,w-tw-8)),y=Math.clamp(Math.round(p[1]),4,Math.max(4,h-20));HeraTheme.round(ctx,x-4,y-3,tw+8,15,4,0xDD171C27);HeraTheme.hudText(ctx,value.text,x,y,color,false,false);}
    public static void clearTextCache(){SHARED.labels.clear();}
    public static void render(DrawContext ctx){
        var c=MinecraftClient.getInstance();var f=Esp.config;var o=Esp.options();
        if(f==null || c.world==null || c.player==null || c.options.hudHidden)return;
        if(!needed(f,o)||targetWorld!=c.world)return;
        var overlay=frame();
        float tick=c.gameRenderer.getCamera().getLastTickProgress();
        for(var e:targets){
            if(e.isRemoved()||e.squaredDistanceTo(c.player)>(double)f.espRange*f.espRange)continue;boolean esp=Esp.marked(e);var position=e.getLerpedPos(tick);var box=e.getBoundingBox().offset(position.subtract(e.getEntityPos()));
            if(esp && o.espBoxes)overlay.box(ctx,box,o.espColor,o.lineWidth);
            if(esp && o.espTracers)overlay.tracer(ctx,position.add(0,e.getHeight()*.5,0),o.espColor,o.lineWidth);
            if(!(esp && o.espBoxes) && o.hitboxes && hitboxTarget(e))overlay.box(ctx,box,o.hitboxColor,o.hitboxWidth);
            int fuse=e instanceof TntEntity tnt?tnt.getFuse():e instanceof net.minecraft.entity.vehicle.TntMinecartEntity cart?cart.getFuseTicks():-1;
            if(o.tntTimer && fuse>=0)overlay.label(ctx,position.add(0,e.getHeight()+.3,0),TntLabels.at(fuse,tick),o.tntColor);
        }
    }
    private static boolean needed(FeatureConfig f,ModuleOptions o){return o.hitboxes||o.tntTimer||(o.espBoxes||o.espTracers)&&(f.itemEsp||f.playerEsp||f.mobEsp);}
    public static boolean cached(Entity entity){return targetWorld==entity.getEntityWorld()&&targetSet.contains(entity);}
    public static void tick(MinecraftClient c){
        var f=Esp.config;if(f==null||c.world==null||c.player==null||!(needed(f,f.options)||f.options.espGlow&&(f.itemEsp||f.playerEsp||f.mobEsp))){targets=List.of();targetSet.clear();nearest.clear();targetWorld=null;refresh=0;return;}
        if(targetWorld!=c.world){targets=List.of();targetSet.clear();targetWorld=c.world;refresh=0;}if(refresh-->0)return;refresh=1;nearest.clear();var o=f.options;
        for(var e:c.world.getEntities())if(e!=c.player&&!e.isRemoved()&&e.squaredDistanceTo(c.player)<=(double)f.espRange*f.espRange&&(Esp.marked(e)||o.hitboxes&&hitboxTarget(e)||o.tntTimer&&(e instanceof TntEntity||e instanceof net.minecraft.entity.vehicle.TntMinecartEntity)))nearest.offer(e);
        targets=nearest.snapshot();targetSet.clear();targetSet.addAll(targets);
    }
    private static boolean hitboxTarget(Entity e){var o=Esp.options();return (o.invisibleHitboxes||!e.isInvisible())&&(o.hitboxPlayers && e instanceof PlayerEntity || o.hitboxMobs && e instanceof MobEntity || o.hitboxItems && e instanceof ItemEntity);}
}
