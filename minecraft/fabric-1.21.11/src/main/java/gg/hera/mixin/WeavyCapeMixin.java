package gg.hera.mixin;
import gg.hera.features.Presentation;
import gg.hera.cosmetics.WeavyCapeModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(CapeFeatureRenderer.class)
public abstract class WeavyCapeMixin {
 @Unique private WeavyCapeModel hera$cloth;
 @ModifyArg(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"),index=0)
 private Model<?> hera$cloth(Model<?> vanilla){if(!Presentation.enabled("Weavy Capes"))return vanilla;if(hera$cloth==null)hera$cloth=new WeavyCapeModel();return hera$cloth;}
}
