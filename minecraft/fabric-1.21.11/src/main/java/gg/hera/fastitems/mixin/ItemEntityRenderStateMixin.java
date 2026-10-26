package gg.hera.fastitems.mixin;

import gg.hera.fastitems.FastItemsState;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements FastItemsState {
    @Unique private boolean fastitems$billboard;
    @Override public boolean fastitems$isBillboard() { return fastitems$billboard; }
    @Override public void fastitems$setBillboard(boolean value) { fastitems$billboard = value; }
}
