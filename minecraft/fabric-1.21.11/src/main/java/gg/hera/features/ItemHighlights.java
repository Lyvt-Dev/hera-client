package gg.hera.features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
public final class ItemHighlights {
    private ItemHighlights(){}
    public static void render(DrawContext ctx){var c=MinecraftClient.getInstance();var f=Esp.config;if(f==null || !f.options.itemHighlighter || c.player==null || c.options.hudHidden || c.currentScreen!=null || c.player.isSpectator())return;
        int base=ctx.getScaledWindowWidth()/2-90,y=ctx.getScaledWindowHeight()-21,color=f.options.highlightColor;
        for(int i=0;i<9;i++){var stack=c.player.getInventory().getStack(i);if(stack.isEmpty() || !ItemSelection.matches(f.markedItems,Registries.ITEM.getId(stack.getItem()).toString()))continue;int x=base+20*i;
            ctx.fill(x,y,x+18,y+1,color);ctx.fill(x,y+18,x+18,y+19,color);ctx.fill(x,y,x+1,y+19,color);ctx.fill(x+17,y,x+18,y+19,color);
        }
    }
}
