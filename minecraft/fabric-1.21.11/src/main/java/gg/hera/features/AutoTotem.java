package gg.hera.features;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** One vanilla offhand swap per request; no cursor pickup or chained clicks. */
public final class AutoTotem {
    private final FeatureConfig config;
    private int cooldown;
    public AutoTotem(FeatureConfig config){this.config=config;}
    public void tick(MinecraftClient c){
        if(cooldown>0){cooldown--;return;}
        if(CameraController.detached() || !config.autoTotem || c.player==null || c.interactionManager==null || c.currentScreen!=null || !c.player.isAlive() || c.player.isSpectator())return;
        if(c.player.currentScreenHandler!=c.player.playerScreenHandler || !c.player.currentScreenHandler.getCursorStack().isEmpty() || c.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))return;
        var inv=c.player.getInventory();
        for(int i=0;i<36;i++)if(inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)){
            c.interactionManager.clickSlot(c.player.playerScreenHandler.syncId,AutoTotemPolicy.handlerSlot(i),40,SlotActionType.SWAP,c.player);
            cooldown=config.options.totemDelay;return;
        }
    }
}
