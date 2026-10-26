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

/** Bounded palette scan of loaded chunks only; never reads hidden inventory. */
public final class ContainerMarkers {
    private record Section(net.minecraft.world.chunk.WorldChunk chunk,int index){}
    private final FeatureConfig config;
    private final StableScanResults<BlockPos> results=new StableScanResults<>();private final java.util.ArrayDeque<Section> pending=new java.util.ArrayDeque<>();
    private final java.util.HashSet<net.minecraft.block.Block> blocks=new java.util.HashSet<>();
    private java.util.PriorityQueue<BlockPos> nearest;private Object world;private java.util.Set<String> selection=java.util.Set.of();private int cooldown,lastRange;private BlockPos scanOrigin=BlockPos.ORIGIN;
    public ContainerMarkers(FeatureConfig config){this.config=config;}
    public void tick(MinecraftClient c){
        if(!config.itemEsp || c.player==null || c.world==null){clear();return;}
        if(world!=c.world || !selection.equals(config.markedItems) || lastRange!=config.espRange){clear();world=c.world;selection=java.util.Set.copyOf(config.markedItems);lastRange=config.espRange;
            for(String id:selection){var item=Registries.ITEM.get(net.minecraft.util.Identifier.of(id));if(item instanceof net.minecraft.item.BlockItem block)blocks.add(block.getBlock());}
        }
        if(blocks.isEmpty())return;
        if(pending.isEmpty()){
            if(cooldown-->0)return;
            final var origin=c.player.getBlockPos();scanOrigin=origin;nearest=new java.util.PriorityQueue<>(java.util.Comparator.<BlockPos>comparingDouble(p->p.getSquaredDistance(origin)).reversed());
            int cx=c.player.getBlockX()>>4,cz=c.player.getBlockZ()>>4,r=(config.espRange+15)/16;
            for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
                var chunk=c.world.getChunkManager().getChunk(cx+dx,cz+dz,ChunkStatus.FULL,false);if(chunk==null)continue;
                for(int i=0;i<chunk.getSectionArray().length;i++)pending.addLast(new Section(chunk,i));
            }
        }
        int inspected=0,dense=0;
        while(!pending.isEmpty() && inspected++<64 && dense<4){var work=pending.removeFirst();var chunk=work.chunk();
            if(!c.world.isChunkLoaded(chunk.getPos().x,chunk.getPos().z))continue;
            var section=chunk.getSectionArray()[work.index()];if(section.isEmpty() || !section.hasAny(state->blocks.contains(state.getBlock())))continue;dense++;
            int bx=chunk.getPos().getStartX(),bz=chunk.getPos().getStartZ(),by=c.world.getBottomY()+work.index()*16;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++){
                if(!blocks.contains(section.getBlockState(x,y,z).getBlock()))continue;var pos=new BlockPos(bx+x,by+y,bz+z);
                double distance=pos.getSquaredDistance(scanOrigin);if(distance>(double)config.espRange*config.espRange)continue;
                if(nearest.size()<128)nearest.add(pos);else if(distance<nearest.peek().getSquaredDistance(scanOrigin)){nearest.poll();nearest.add(pos);}
            }
        }
        if(nearest!=null){if(pending.isEmpty())results.commit(nearest);else results.progress(nearest);}
        if(pending.isEmpty())cooldown=20;
    }
    private void clear(){results.clear();pending.clear();blocks.clear();selection=java.util.Set.of();nearest=null;world=null;cooldown=0;}
    public void render(DrawContext ctx){
        var c=MinecraftClient.getInstance();
        if(!config.itemEsp || c.world==null || c.player==null || c.options.hudHidden)return;
        var overlay=new WorldOverlay();var o=config.options;
        for(var p:results.visible()){
            if(!c.world.isChunkLoaded(p))continue;
            var state=c.world.getBlockState(p);String id=Registries.ITEM.getId(state.getBlock().asItem()).toString();
            if(!ItemSelection.matches(config.markedItems,id) || p.getSquaredDistance(c.player.getBlockPos())>(double)config.espRange*config.espRange)continue;
            if(o.espBoxes)overlay.box(ctx,new net.minecraft.util.math.Box(p),o.espColor,o.lineWidth);
            if(o.espTracers)overlay.tracer(ctx,p.toCenterPos(),o.espColor,o.lineWidth);
            String label=state.getBlock().getName().getString()+" · "+Math.round(Math.sqrt(p.getSquaredDistance(c.player.getEntityPos())))+" m";
            if(o.espLabels)overlay.label(ctx,p.toCenterPos(),label,o.espColor);
        }
    }
}
