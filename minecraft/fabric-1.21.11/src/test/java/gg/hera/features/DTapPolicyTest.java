package gg.hera.features;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DTapPolicyTest {
    @Test void suitabilityRequiresEnoughDamageForHealthAndAbsorption(){assertEquals(24,DTapPolicy.requiredDamage(20,4,6));assertEquals(6,DTapPolicy.requiredDamage(1,4,6));assertFalse(DTapPolicy.suitable(10,2,20,DTapPolicy.requiredDamage(20,0,6),10,8));}
    @Test void invalidTargetHealthCannotQualify(){assertEquals(Double.POSITIVE_INFINITY,DTapPolicy.requiredDamage(Double.NaN,0,6));assertEquals(Double.POSITIVE_INFINITY,DTapPolicy.requiredDamage(0,0,6));}
    @Test void explosionFalloffHasBoundedRadius(){assertEquals(85,DTapPolicy.rawDamage(0,1));assertEquals(0,DTapPolicy.rawDamage(12,1));assertTrue(DTapPolicy.rawDamage(2,1)>DTapPolicy.rawDamage(6,1));}
    @Test void nonFiniteInputsCannotTriggerActions(){assertEquals(0,DTapPolicy.rawDamage(Double.NaN,1));assertFalse(DTapPolicy.suitable(Double.NaN,1,20,6,10,8));assertFalse(DTapPolicy.suitable(12,Double.NaN,20,6,10,8));}
    @Test void targetDamageSelfDamageAndReserveAllMustPass(){assertTrue(DTapPolicy.suitable(10,5,20,6,10,8));assertFalse(DTapPolicy.suitable(5,5,20,6,10,8));assertFalse(DTapPolicy.suitable(20,11,30,6,10,8));assertFalse(DTapPolicy.suitable(20,9,16,6,10,8));}
    @Test void coverReducesTargetEstimate(){assertTrue(DTapPolicy.rawDamage(3,.25)<DTapPolicy.rawDamage(3,1));}
}
