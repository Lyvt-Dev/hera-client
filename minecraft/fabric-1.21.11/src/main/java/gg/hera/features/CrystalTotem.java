package gg.hera.features;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public final class CrystalTotem {
    private final CrystalTotemPolicy policy = new CrystalTotemPolicy();
    private final FeatureConfig config;
    public CrystalTotem(FeatureConfig config) { this.config = config; }
    public void tick(MinecraftClient client) {
        if (!config.crystalTotem) { policy.select(false, true, false, -1, new boolean[0]); return; }
        boolean blocked = CameraController.detached() || client.player == null || client.world == null || client.getNetworkHandler() == null
            || client.currentScreen != null || !client.player.isAlive() || client.player.isSpectator();
        if (blocked) { policy.select(config.crystalTotem, true, false, -1, new boolean[0]); return; }
        boolean threat = client.world.getPlayers().stream().anyMatch(other -> other != client.player
            && other.isAlive() && !other.isSpectator()
            && other.squaredDistanceTo(client.player) <= config.options.crystalRange * config.options.crystalRange
            && (other.getMainHandStack().isOf(Items.END_CRYSTAL) || other.getOffHandStack().isOf(Items.END_CRYSTAL)));
        var inventory = client.player.getInventory();
        boolean[] totems = new boolean[9];
        for (int slot = 0; slot < 9; slot++) totems[slot] = inventory.getStack(slot).isOf(Items.TOTEM_OF_UNDYING);
        int slot = policy.select(config.crystalTotem, false, threat, inventory.getSelectedSlot(), totems);
        if (slot >= 0) {
            inventory.setSelectedSlot(slot);
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }
}
