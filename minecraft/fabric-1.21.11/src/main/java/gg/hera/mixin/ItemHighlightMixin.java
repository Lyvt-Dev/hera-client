package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HandledScreen.class)
public abstract class ItemHighlightMixin {
    @Inject(method="drawSlot",at=@At("TAIL"))
    private void hera$slot(DrawContext c,Slot slot,int mouseX,int mouseY,CallbackInfo ci){
        var f=Esp.config;if(f==null || !f.options.itemHighlighter || slot.getStack().isEmpty() || !Esp.selected(slot.getStack().getItem()))return;
        int x=slot.x,y=slot.y,color=f.options.highlightColor;
        c.fill(x-1,y-1,x+17,y,color);c.fill(x-1,y+16,x+17,y+17,color);c.fill(x-1,y,x,y+16,color);c.fill(x+16,y,x+17,y+16,color);
    }
}
