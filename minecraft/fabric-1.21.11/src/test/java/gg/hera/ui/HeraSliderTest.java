package gg.hera.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HeraSliderTest {
    @Test void quantizationPreservesBoundsAndStep(){
        assertEquals(.5,HeraSlider.quantize(-5,.5,3,.1),.00001);
        assertEquals(3,HeraSlider.quantize(4,.5,3,.1),.00001);
        assertEquals(1.2,HeraSlider.quantize(1.24,.5,3,.1),.00001);
        assertEquals(7,HeraSlider.quantize(6.8,2,12,1),.00001);
    }
}
