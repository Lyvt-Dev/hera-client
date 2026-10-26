package gg.hera.features;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AutoTotemPolicyTest {
    @Test void hotbarMapsToPlayerHandlerSlots(){assertEquals(36,AutoTotemPolicy.handlerSlot(0));assertEquals(44,AutoTotemPolicy.handlerSlot(8));}
    @Test void mainInventoryKeepsSlot(){for(int i=9;i<36;i++)assertEquals(i,AutoTotemPolicy.handlerSlot(i));}
    @Test void armorAndOffhandAreNeverSourceSlots(){assertThrows(IllegalArgumentException.class,()->AutoTotemPolicy.handlerSlot(-1));assertThrows(IllegalArgumentException.class,()->AutoTotemPolicy.handlerSlot(36));}
}
