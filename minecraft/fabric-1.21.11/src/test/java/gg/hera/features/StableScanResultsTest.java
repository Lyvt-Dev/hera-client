package gg.hera.features;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StableScanResultsTest {
    @Test void rescanDoesNotBlankOrPartiallyReplacePublishedMarkers(){var s=new StableScanResults<String>();s.commit(List.of("barrel-a","barrel-b"));s.progress(List.of());assertEquals(List.of("barrel-a","barrel-b"),s.visible());s.progress(List.of("barrel-c"));assertEquals(2,s.visible().size());s.commit(List.of("barrel-c"));assertEquals(List.of("barrel-c"),s.visible());}
    @Test void firstScanCanPopulateGradually(){var s=new StableScanResults<Integer>();s.progress(List.of(1));assertEquals(List.of(1),s.visible());s.progress(List.of(1,2));assertEquals(2,s.visible().size());}
    @Test void completedEmptyScanAndWorldResetRemoveOldMarkers(){var s=new StableScanResults<Integer>();s.commit(List.of(1));s.commit(List.of());assertTrue(s.visible().isEmpty());s.clear();s.progress(List.of(2));assertEquals(List.of(2),s.visible());}
}
