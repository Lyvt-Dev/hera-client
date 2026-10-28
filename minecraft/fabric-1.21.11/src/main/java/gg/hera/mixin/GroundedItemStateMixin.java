package gg.hera.mixin;
import gg.hera.features.GroundedItemState;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.*;
@Mixin(ItemEntityRenderState.class)
public abstract class GroundedItemStateMixin implements GroundedItemState {
 @Unique private boolean hera$grounded;
 public boolean hera$grounded(){return hera$grounded;}public void hera$grounded(boolean value){hera$grounded=value;}
}
