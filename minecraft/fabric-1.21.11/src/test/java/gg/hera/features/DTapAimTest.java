package gg.hera.features;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DTapAimTest {
    @Test void cardinalDirectionsAndPitchMatchMinecraft(){assertEquals(0,DTapAim.yaw(0,1),.001);assertEquals(-90,DTapAim.yaw(1,0),.001);assertEquals(-90,DTapAim.pitch(0,1,0),.001);assertEquals(90,DTapAim.pitch(0,-1,0),.001);}
    @Test void rotationTakesShortestBoundedPath(){assertEquals(2,DTapAim.difference(-179,179),.001);assertEquals(181,DTapAim.step(179,-179,45),.001);assertEquals(45,DTapAim.step(0,120,45),.001);}
}
