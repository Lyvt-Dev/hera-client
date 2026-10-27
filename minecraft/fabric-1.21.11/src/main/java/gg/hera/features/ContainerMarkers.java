package gg.hera.features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.registry.Registries;
import java.util.*;

/** Incremental loaded-chunk scan. Never probes unloaded chunks or hidden inventories. */
public final class ContainerMarkers {
    private record Section(net.minecraft.world.chunk.WorldChunk chunk,int index){}
    private final FeatureConfig config;
    private final StableScanResults<BlockPos> results=new StableScanResults<>();
    private final Set<net.minecraft.block.Block> blocks=new HashSet<>();
    private final java.util.function.Predicate<net.minecraft.block.BlockState> matches=state->blocks.contains(state.getBlock());
    private PriorityQueue<BlockPos> nearest;private Object world;private Set<String> selection=Set.of();
    private int cooldown,lastRange,chunkCursor,sectionCursor,cellCursor,cx,cz,radius;private boolean scanning;
    private Section active;private BlockPos scanOrigin=BlockPos.ORIGIN;
    private record Label(int bucket,String value){}
    private int ticks;
    private final Map<BlockPos,Label> labels=new LinkedHashMap<>(128,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<BlockPos,Label> entry){return size()>128;}};
    public ContainerMarkers(FeatureConfig config){this.config=config;}
    public void tick(MinecraftClient c){
        ticks++;
        var o=config.options;
        if(!config.itemEsp||c.player==null||c.world==null||!(o.espBoxes||o.espTracers||o.espLabels)){if(world!=null)clear();return;}
        if(world!=c.world||!selection.equals(config.markedItems)||lastRange!=config.espRange){clear();world=c.world;selection=Set.copyOf(config.markedItems);lastRange=config.espRange;
            for(String id:selection){var item=Registries.ITEM.get(net.minecraft.util.Identifier.of(id));if(item instanceof net.minecraft.item.BlockItem block)blocks.add(block.getBlock());}
        }
        if(blocks.isEmpty()||c.options.hudHidden)return;
        if(!scanning){
            if(cooldown-->0)return;
            scanOrigin=c.player.getBlockPos();final var origin=scanOrigin;
            nearest=new PriorityQueue<>(Comparator.<BlockPos>comparingDouble(p->p.getSquaredDistance(origin)).reversed());
            cx=c.player.getBlockX()>>4;cz=c.player.getBlockZ()>>4;radius=(config.espRange+15)/16;
            chunkCursor=sectionCursor=cellCursor=0;active=null;scanning=true;
        }
        var budget=new WorkBudget(4096,1_000_000);int inspected=0;boolean dirty=false;
        int side=radius*2+1,total=side*side;
        while(budget.hasTime()){
            if(active==null){
                if(chunkCursor>=total)break;if(inspected++>=64)break;
                int x=cx+chunkCursor%side-radius,z=cz+chunkCursor/side-radius;
                var chunk=c.world.getChunkManager().getChunk(x,z,ChunkStatus.FULL,false);
                if(chunk==null||sectionCursor>=chunk.getSectionArray().length){chunkCursor++;sectionCursor=0;continue;}
                int section=sectionCursor++,bottom=c.world.getBottomY()+section*16;
                if(bottom>scanOrigin.getY()+lastRange||bottom+15<scanOrigin.getY()-lastRange)continue;
                var data=chunk.getSectionArray()[section];if(data.isEmpty()||!data.hasAny(matches))continue;
                active=new Section(chunk,section);cellCursor=0;
            }
            if(!c.world.isChunkLoaded(active.chunk.getPos().x,active.chunk.getPos().z)){active=null;continue;}
            var section=active.chunk.getSectionArray()[active.index];
            int bx=active.chunk.getPos().getStartX(),bz=active.chunk.getPos().getStartZ(),by=c.world.getBottomY()+active.index*16;
            while(cellCursor<4096&&budget.step()){
                int index=cellCursor++,x=index&15,z=(index>>>4)&15,y=index>>>8;
                if(!blocks.contains(section.getBlockState(x,y,z).getBlock()))continue;
                double dx=bx+x-scanOrigin.getX(),dy=by+y-scanOrigin.getY(),dz=bz+z-scanOrigin.getZ(),distance=dx*dx+dy*dy+dz*dz;
                if(distance>(double)lastRange*lastRange||nearest.size()>=128&&distance>=nearest.peek().getSquaredDistance(scanOrigin))continue;
                if(nearest.size()>=128)nearest.poll();nearest.add(new BlockPos(bx+x,by+y,bz+z));dirty=true;
            }
            if(cellCursor==4096)active=null;else break;
        }
        if(chunkCursor>=total&&active==null){results.commit(nearest);scanning=false;cooldown=20;}
        else if(dirty)results.progress(nearest);
    }
    private void clear(){results.clear();blocks.clear();labels.clear();selection=Set.of();nearest=null;world=null;active=null;scanning=false;cooldown=0;}
    public void render(DrawContext ctx){
        var c=MinecraftClient.getInstance();var o=config.options;
        if(!config.itemEsp||c.world==null||c.world!=world||c.player==null||c.options.hudHidden||results.visible().isEmpty()||!(o.espBoxes||o.espTracers||o.espLabels))return;
        var overlay=WorldOverlay.frame();var origin=c.player.getBlockPos();
        for(var p:results.visible()){
            if(!c.world.isChunkLoaded(p)||p.getSquaredDistance(origin)>(double)config.espRange*config.espRange)continue;
            var state=c.world.getBlockState(p);if(!blocks.contains(state.getBlock()))continue;
            if(o.espBoxes)overlay.box(ctx,new net.minecraft.util.math.Box(p),o.espColor,o.lineWidth);
            if(o.espTracers)overlay.tracer(ctx,p.toCenterPos(),o.espColor,o.lineWidth);
            if(o.espLabels){int bucket=(ticks+(p.hashCode()&3))/5;var label=labels.get(p);if(label==null||label.bucket!=bucket){label=new Label(bucket,state.getBlock().getName().getString()+" · "+Math.round(Math.sqrt(p.getSquaredDistance(c.player.getEntityPos())))+" m");labels.put(p,label);}overlay.label(ctx,p.toCenterPos(),label.value,o.espColor);}
        }
    }
}
