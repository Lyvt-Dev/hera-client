package gg.hera.features;

import gg.hera.mixin.GameRendererAccessor;
import gg.hera.ui.HeraTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.inventory.Inventory;
import net.minecraft.registry.Registries;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.*;

/** Searches only already-loaded block entities, never their hidden inventory. */
public final class ContainerMarkers {
    private final FeatureConfig config;
    private List<BlockPos> positions=List.of();private int ticks;
    public ContainerMarkers(FeatureConfig config){this.config=config;}
    public void tick(MinecraftClient c){
        if(!config.itemEsp || c.player==null || c.world==null){positions=List.of();ticks=0;return;}
        if(ticks++%10!=0)return;
        var found=new ArrayList<BlockPos>();int cx=c.player.getBlockX()>>4,cz=c.player.getBlockZ()>>4,r=(config.espRange+15)/16;
        for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            var chunk=c.world.getChunkManager().getChunk(cx+dx,cz+dz,ChunkStatus.FULL,false);
            if(chunk==null)continue;
            for(var entry:chunk.getBlockEntities().entrySet()){
                if(!(entry.getValue() instanceof Inventory))continue;
                var pos=entry.getKey();
                if(pos.getSquaredDistance(c.player.getEntityPos())>(double)config.espRange*config.espRange)continue;
                String id=Registries.ITEM.getId(entry.getValue().getCachedState().getBlock().asItem()).toString();
                if(ItemSelection.matches(config.markedItems,id))found.add(pos.toImmutable());
            }
        }
        found.sort(Comparator.comparingDouble(p->p.getSquaredDistance(c.player.getEntityPos())));
        positions=List.copyOf(found.subList(0,Math.min(128,found.size())));
    }
    public void render(DrawContext ctx){
        var c=MinecraftClient.getInstance();
        if(!config.itemEsp || c.world==null || c.player==null || c.options.hudHidden || c.currentScreen!=null)return;
        var camera=c.gameRenderer.getCamera();var origin=camera.getCameraPos();
        var inverse=new Quaternionf(camera.getRotation()).conjugate();
        float tick=c.getRenderTickCounter().getTickProgress(false);
        var projection=c.gameRenderer.getBasicProjectionMatrix(((GameRendererAccessor)c.gameRenderer).hera$fov(camera,tick,true));
        int width=c.getWindow().getScaledWidth(),height=c.getWindow().getScaledHeight();
        for(var p:positions){
            if(!c.world.isChunkLoaded(p))continue;
            var state=c.world.getBlockState(p);String id=Registries.ITEM.getId(state.getBlock().asItem()).toString();
            if(!ItemSelection.matches(config.markedItems,id))continue;
            var v=new Vector3f((float)(p.getX()+.5-origin.x),(float)(p.getY()+.6-origin.y),(float)(p.getZ()+.5-origin.z));inverse.transform(v);
            if(v.z>=-.05f)continue;
            var clip=projection.transform(new Vector4f(v,1));float nx=clip.x/clip.w,ny=clip.y/clip.w;
            if(!Float.isFinite(nx)||!Float.isFinite(ny)||Math.abs(nx)>1||Math.abs(ny)>1)continue;
            String label=state.getBlock().getName().getString()+" · "+Math.round(Math.sqrt(p.getSquaredDistance(c.player.getEntityPos())))+" m";
            int tw=c.textRenderer.getWidth(HeraTheme.text(label)),x=Math.clamp(Math.round((nx+1)*width/2)-tw/2,4,Math.max(4,width-tw-8)),y=Math.clamp(Math.round((1-ny)*height/2),4,height-20);
            HeraTheme.round(ctx,x-4,y-3,tw+8,15,4,0xDD171C27);HeraTheme.label(ctx,label,x,y,HeraTheme.ACCENT);
        }
    }
}
