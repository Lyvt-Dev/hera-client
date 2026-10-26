package gg.hera.fastitems.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Only active layers belong to the current resolved model; capacity may contain stale layers. */
@Mixin(ItemRenderState.class)
public interface ItemRenderStateAccessor {
    @Accessor("layerCount") int fastitems$activeLayerCount();
    @Accessor("layers") ItemRenderState.LayerRenderState[] fastitems$layers();
}
