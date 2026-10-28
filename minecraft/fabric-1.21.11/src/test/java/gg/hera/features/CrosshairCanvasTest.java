package gg.hera.features;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrosshairCanvasTest {
 @Test void dragStrokeHasNoGapsAndClamps(){var c=new CrosshairCanvas();c.line(-3,-8,40,40,0xFFFFFFFF);for(int i=0;i<32;i++)assertEquals(-1,c.get(i,i));}
 @Test void undoSnapshotIsIndependent(){var c=new CrosshairCanvas();c.line(4,7,4,7,-1);var saved=c.copy();c.clear();assertEquals(0,c.get(4,7));c.restore(saved);assertEquals(-1,c.get(4,7));c.line(4,7,4,7,0);assertEquals(0,c.get(4,7));}
}
