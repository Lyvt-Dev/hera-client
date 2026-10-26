package gg.hera.fastitems.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("specialModelType") SpecialModelRenderer<?> fastitems$specialRenderer();
    @Accessor("useLight") boolean fastitems$usesBlockLight();
    @Accessor("transform") Transformation fastitems$transform();
}
