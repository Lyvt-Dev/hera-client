package gg.hera.features;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;
class CrosshairTest {
    @TempDir Path directory;
    private byte[] header(int w,int h){var b=ByteBuffer.allocate(33);b.putLong(0x89504E470D0A1A0AL).putInt(13).putInt(0x49484452).putInt(w).putInt(h);return b.array();}
    @Test void boundsAreCheckedBeforeDecode(){assertTrue(CrosshairImage.valid(header(32,32)));assertTrue(CrosshairImage.valid(header(256,1)));assertFalse(CrosshairImage.valid(header(257,16)));assertFalse(CrosshairImage.valid(header(0,16)));assertFalse(CrosshairImage.valid(header(Integer.MAX_VALUE,16)));assertFalse(CrosshairImage.valid(new byte[CrosshairImage.MAX_BYTES+1]));assertFalse(CrosshairImage.valid(null));assertFalse(CrosshairImage.valid(new byte[12]));var bad=header(16,16);bad[0]=0;assertFalse(CrosshairImage.valid(bad));}
    @Test void appearancePersistsAndNewModulesStayOff()throws Exception{var path=directory.resolve("features.json");var f=new FeatureConfig(path);f.options.crosshairImage=true;f.options.crosshairColor=0xFF23ABCD;f.options.crosshairGap=7;f.options.crosshairOpacity=60;f.options.nametagSneaking=false;f.save();var next=new FeatureConfig(path);next.load();assertFalse(next.options.customCrosshair);assertFalse(next.options.ownNametag);assertTrue(next.options.crosshairImage);assertEquals(7,next.options.crosshairGap);assertEquals(60,next.options.crosshairOpacity);assertEquals(0xFF23ABCD,next.options.crosshairColor);assertFalse(next.options.nametagSneaking);}
    @Test void invalidDimensionsAndOpacityAreClamped(){var o=new ModuleOptions();o.crosshairSize=Integer.MAX_VALUE;o.crosshairGap=-3;o.crosshairThickness=0;o.crosshairOpacity=1000;o.crosshairLength=-7;o.normalize();assertEquals(96,o.crosshairSize);assertEquals(0,o.crosshairGap);assertEquals(1,o.crosshairThickness);assertEquals(100,o.crosshairOpacity);assertEquals(1,o.crosshairLength);}
}
