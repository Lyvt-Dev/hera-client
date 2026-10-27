package gg.hera.hud;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class HudStudioTest {
    @TempDir Path directory;
    @Test void existingLayoutsKeepOpaqueTextAndNewAppearanceDefaults()throws Exception{var path=directory.resolve("hud.json");Files.writeString(path,"{\"schemaVersion\":2,\"modules\":[{\"id\":\"FPS\",\"enabled\":true,\"x\":0.3,\"y\":0.4,\"scale\":1}]}");var c=new HudConfig(path);c.load();var m=c.modules.getFirst();assertEquals(.3f,m.x);assertEquals(1,m.opacity);assertEquals(5,m.padding);assertTrue(m.enabled);assertFalse(c.modules.getLast().enabled);}
    @Test void anchorRoundTripsAtAllRequestedResolutions(){for(int screen:new int[]{320,640,1280,1920,2560})for(int anchor=0;anchor<3;anchor++)for(int position:new int[]{0,50,100}){int size=40;float offset=HudGeometry.offset(position,anchor,screen,size);assertEquals(position,HudGeometry.position(offset,anchor,screen,size));}assertEquals(0,HudGeometry.position(1,0,20,50));}
    @Test void clickWindowCountsInputEdgesAndExpires(){var m=new ClickMeter();m.record(0,1_100_000_000L);m.record(1,1_500_000_000L);m.record(0,1_900_000_000L);m.record(3,2_000_000_000L);assertEquals(2,m.count(0,2_000_000_000L,1));assertEquals(1,m.count(1,2_000_000_000L,1));assertEquals(0,m.count(0,4_000_000_000L,1));}
    @Test void presetsAndUndoSnapshotsPreserveFullConfiguration()throws Exception{var c=new HudConfig(directory.resolve("hud.json"));c.modules.getFirst().opacity=.4f;c.modules.getFirst().anchorX=1;c.modules.getFirst().showGraph=true;String snapshot=c.snapshot();c.savePreset("Mein Layout");c.applyPreset("Minimal");c.restore(snapshot);assertEquals(.4f,c.modules.getFirst().opacity);assertEquals(1,c.modules.getFirst().anchorX);assertTrue(c.modules.getFirst().showGraph);c.save();var next=new HudConfig(directory.resolve("hud.json"));next.load();next.applyPreset("Mein Layout");assertEquals(.4f,next.modules.getFirst().opacity);assertThrows(IllegalArgumentException.class,()->next.savePreset("Minimal"));}
    @Test void appearanceRejectsNonFiniteAndExtremeValues(){var m=new HudModule("FPS",Float.NaN,Float.POSITIVE_INFINITY);m.opacity=Float.NaN;m.padding=999;m.radius=-8;m.scale=Float.NaN;m.precision=99;m.normalize();assertEquals(1,m.opacity);assertEquals(12,m.padding);assertEquals(0,m.radius);assertEquals(1,m.scale);assertEquals(3,m.precision);}
}
