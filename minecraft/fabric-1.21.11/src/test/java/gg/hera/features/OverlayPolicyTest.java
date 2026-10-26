package gg.hera.features;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OverlayPolicyTest {
    @Test void clipsCrossingLinesToViewport(){assertArrayEquals(new float[]{0,50,100,50},OverlayPolicy.clip(-500,50,600,50,100,100),.01f);}
    @Test void rejectsLinesOutsideOneEdge(){assertNull(OverlayPolicy.clip(-20,-20,-10,-10,100,100));}
    @Test void rejectsNonFiniteCoordinates(){assertNull(OverlayPolicy.clip(Float.NaN,1,2,3,100,100));assertNull(OverlayPolicy.clip(0,1,Float.POSITIVE_INFINITY,3,100,100));}
    @Test void keepsInteriorAndVerticalLines(){assertArrayEquals(new float[]{10,10,90,90},OverlayPolicy.clip(10,10,90,90,100,100));assertArrayEquals(new float[]{50,0,50,100},OverlayPolicy.clip(50,-5,50,120,100,100),.01f);}
}
