package gg.hera.features;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalTotemPolicyTest {
    private boolean[] hotbar(int... slots) { boolean[] inventory = new boolean[36]; for (int s : slots) inventory[s] = true; return inventory; }
    @Test void onlySelectsHotbarTotems() {
        var p = new CrystalTotemPolicy();
        assertEquals(-1, p.select(true, false, true, 0, hotbar(15, 35)));
        assertEquals(8, p.select(true, false, true, 0, hotbar(8, 15)));
    }
    @Test void noThreatOrDisabledNeverSelects() {
        var p = new CrystalTotemPolicy();
        assertEquals(-1, p.select(false, false, true, 0, hotbar(3)));
        assertEquals(-1, p.select(true, false, false, 0, hotbar(3)));
        assertEquals(-1, p.select(true, true, true, 0, hotbar(3)));
    }
    @Test void switchesOnceAndRearmsAfterThreatEnds() {
        var p = new CrystalTotemPolicy();
        assertEquals(3, p.select(true, false, true, 0, hotbar(3)));
        assertEquals(-1, p.select(true, false, true, 0, hotbar(3)));
        assertEquals(-1, p.select(true, false, false, 0, hotbar(3)));
        assertEquals(3, p.select(true, false, true, 0, hotbar(3)));
    }
    @Test void doesNotSwitchAwayFromAnAlreadySelectedTotem() {
        assertEquals(-1, new CrystalTotemPolicy().select(true, false, true, 7, hotbar(2, 7)));
    }
    @Test void waitsIfNoTotemExistsThenUsesFirstAvailable() {
        var p = new CrystalTotemPolicy();
        assertEquals(-1, p.select(true, false, true, 0, hotbar()));
        assertEquals(2, p.select(true, false, true, 0, hotbar(2, 6)));
    }
    @Test void resetOnDisconnectOrDisabled() {
        var p = new CrystalTotemPolicy();
        assertEquals(1, p.select(true, false, true, 0, hotbar(1)));
        assertEquals(-1, p.select(true, true, false, -1, new boolean[0]));
        assertEquals(1, p.select(true, false, true, 0, hotbar(1)));
    }
}
