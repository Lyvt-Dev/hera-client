package gg.hera.features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
public final class PresentationOverlay {
 private PresentationOverlay(){}
 public static void render(DrawContext c){var mc=MinecraftClient.getInstance();if(mc.world==null||mc.player==null||mc.options.hudHidden)return;var o=Esp.options();
  if(Presentation.enabled("Block Outlines")&&mc.crosshairTarget instanceof BlockHitResult hit){var shape=mc.world.getBlockState(hit.getBlockPos()).getOutlineShape(mc.world,hit.getBlockPos());if(!shape.isEmpty())WorldOverlay.frame().box(c,shape.getBoundingBox().offset(hit.getBlockPos()),o.blockColor,o.blockWidth);}
  if(Presentation.enabled("Way Points")){String world=Presentation.worldKey(mc);var overlay=WorldOverlay.frame();for(var p:o.waypoints)if(world.equals(p.world)){var pos=new Vec3d(p.x,p.y,p.z);overlay.label(c,pos,p.name+" · "+Math.round(mc.player.getEntityPos().distanceTo(pos))+" m",p.color);}}
 }
}
