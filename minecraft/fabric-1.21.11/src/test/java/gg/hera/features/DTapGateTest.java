package gg.hera.features;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DTapGateTest {
    @Test void popAloneNeverAllowsFollowup(){var g=new DTapGate();g.begin(12,30);g.status(12,(byte)35);assertFalse(g.ready());g.removed(30);assertTrue(g.ready());}
    @Test void packetsCanArriveInReverseOrder(){var g=new DTapGate();g.begin(12,30);g.removed(30);assertFalse(g.ready());g.status(12,(byte)35);assertTrue(g.ready());}
    @Test void unrelatedEntitiesAndStatusCodesDoNotConfirm(){var g=new DTapGate();g.begin(12,30);g.status(13,(byte)35);g.removed(31);g.status(12,(byte)2);assertFalse(g.ready());}
    @Test void newAttemptAndWorldResetForgetOldConfirmations(){var g=new DTapGate();g.begin(12,30);g.status(12,(byte)35);g.removed(30);g.begin(12,31);assertFalse(g.ready());g.reset();g.status(12,(byte)35);g.removed(31);assertFalse(g.ready());}
}
