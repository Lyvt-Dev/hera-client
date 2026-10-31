package gg.hera.mixin;
import gg.hera.features.Presentation;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.util.DyeColor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ShieldModelRenderer.class)
public abstract class ShieldStatusMixin {
 @Unique private boolean hera$tint;
 @Unique private static final DyeColor[] hera$dyes=DyeColor.values();
 @Unique private static final int[] hera$palette=java.util.Arrays.stream(hera$dyes).mapToInt(DyeColor::getEntityColor).toArray();
 @Unique private static int hera$lastColor=-1;
 @Unique private static DyeColor hera$lastDye;
 @Inject(method="render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V",at=@At("HEAD"))
 private void hera$context(ComponentMap data,ItemDisplayContext context,MatrixStack m,OrderedRenderCommandQueue q,int light,int overlay,boolean glint,int outline,CallbackInfo ci){hera$tint=context.isFirstPerson()&&Presentation.hudEnabled("Shield Status");}
 @ModifyArg(method="render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"),index=8)
 private int hera$color(int vanilla){return hera$tint?Presentation.shieldColor():vanilla;}
 @ModifyArg(method="render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/block/entity/BannerBlockEntityRenderer;renderCanvas(Lnet/minecraft/client/texture/SpriteHolder;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IILnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/SpriteIdentifier;ZLnet/minecraft/util/DyeColor;Lnet/minecraft/component/type/BannerPatternsComponent;ZLnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"),index=9)
 private DyeColor hera$banner(DyeColor vanilla){if(!hera$tint)return vanilla;int color=Presentation.shieldColor()&0xFFFFFF;if(hera$lastDye==null||hera$lastColor!=color){int index=gg.hera.features.ColorMatch.nearest(color,hera$palette);hera$lastDye=index<0?vanilla:hera$dyes[index];hera$lastColor=color;}return hera$lastDye;}
}
