package gg.hera.features;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;


public final class CrystalTotem {
    private final CrystalTotemPolicy policy = new CrystalTotemPolicy();
    private final FeatureConfig config;
    private final boolean[] totems=new boolean[9];
    public CrystalTotem(FeatureConfig config) { this.config = config; }
    public void tick(MinecraftClient client) {
        if (!config.crystalTotem) { policy.select(false, true, false, -1, new boolean[0]); return; }
        boolean blocked = CameraController.detached() || client.player == null || client.world == null || client.getNetworkHandler() == null
            || client.currentScreen != null || client.player.isUsingItem() || client.player.currentScreenHandler != client.player.playerScreenHandler || !client.player.isAlive() || client.player.isSpectator();
        if (blocked) { policy.select(config.crystalTotem, true, false, -1, new boolean[0]); return; }
        boolean threat=false;
        for(var other:client.world.getPlayers())if(other != client.player
            && other.isAlive() && !other.isSpectator()
            && other.squaredDistanceTo(client.player) <= config.options.crystalRange * config.options.crystalRange
            && (other.getMainHandStack().isOf(Items.END_CRYSTAL) || other.getOffHandStack().isOf(Items.END_CRYSTAL))){threat=true;break;}
        var inventory = client.player.getInventory();
        for (int slot = 0; slot < 9; slot++) totems[slot] = inventory.getStack(slot).isOf(Items.TOTEM_OF_UNDYING);
        int slot = policy.select(config.crystalTotem, false, threat, inventory.getSelectedSlot(), totems);
        if (slot >= 0) {
            inventory.setSelectedSlot(slot);
            // Vanilla interaction manager synchronizes the changed slot once.
        }
    }
}
