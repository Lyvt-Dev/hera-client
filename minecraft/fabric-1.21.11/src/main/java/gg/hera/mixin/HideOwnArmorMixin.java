package gg.hera.mixin;
import gg.hera.features.Esp;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
@Mixin(ArmorFeatureRenderer.class)
public abstract class HideOwnArmorMixin {
    @Inject(method="renderArmor",at=@At("HEAD"),cancellable=true)
    private void hera$armor(MatrixStack matrices,OrderedRenderCommandQueue queue,ItemStack stack,EquipmentSlot slot,int light,BipedEntityRenderState state,CallbackInfo ci){
        var player=MinecraftClient.getInstance().player;
        var o=Esp.options();boolean hidden=switch(slot){case HEAD->o.hideHelmet;case CHEST->o.hideChest;case LEGS->o.hideLegs;case FEET->o.hideBoots;default->false;};
        if(o.hideOwnArmor && hidden && player!=null && state instanceof PlayerEntityRenderState p && p.id==player.getId())ci.cancel();
    }
}
