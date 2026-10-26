package gg.hera.features;

/** One switch per crystal-threat episode, never inventory movement or slot spam. */
public final class CrystalTotemPolicy {
    private boolean armed = true;
    public int select(boolean enabled, boolean blocked, boolean threat, int currentSlot, boolean[] hotbarTotems) {
        if (!enabled || blocked || !threat) { armed = true; return -1; }
        if (!armed) return -1;
        if (currentSlot >= 0 && currentSlot < Math.min(9, hotbarTotems.length) && hotbarTotems[currentSlot]) { armed = false; return -1; }
        for (int slot = 0; slot < Math.min(9, hotbarTotems.length); slot++) {
            if (hotbarTotems[slot]) { armed = false; return slot; }
        }
        return -1;
    }
}
