package gg.hera.mixin;
import gg.hera.features.Presentation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(DrawContext.class)
public abstract class ShulkerPreviewMixin {
 @Inject(method="drawItemTooltip",at=@At("HEAD"),cancellable=true)
 private void hera$preview(TextRenderer renderer,ItemStack stack,int x,int y,CallbackInfo ci){if(!Presentation.enabled("Shulker Preview")||!(stack.getItem() instanceof net.minecraft.item.BlockItem b)||!(b.getBlock() instanceof net.minecraft.block.ShulkerBoxBlock))return;var contents=stack.get(DataComponentTypes.CONTAINER);if(contents==null)return;var ctx=(DrawContext)(Object)this;int xx=Math.clamp(x+10,4,Math.max(4,ctx.getScaledWindowWidth()-174)),yy=Math.clamp(y+10,4,Math.max(4,ctx.getScaledWindowHeight()-80));gg.hera.ui.HeraTheme.round(ctx,xx,yy,170,76,5,0xF0101216);gg.hera.ui.HeraTheme.label(ctx,gg.hera.ui.HeraTheme.fit(stack.getName().getString(),158),xx+6,yy+6,gg.hera.ui.HeraTheme.TEXT);var items=net.minecraft.util.collection.DefaultedList.ofSize(27,ItemStack.EMPTY);contents.copyTo(items);for(int i=0;i<27;i++){int px=xx+4+i%9*18,py=yy+20+i/9*18;ctx.fill(px,py,px+16,py+16,0x182C313A);ctx.drawItem(items.get(i),px,py);ctx.drawStackOverlay(renderer,items.get(i),px,py);}ci.cancel();}
}
