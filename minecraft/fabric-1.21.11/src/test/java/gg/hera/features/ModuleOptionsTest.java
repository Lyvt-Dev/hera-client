package gg.hera.features;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class ModuleOptionsTest {
    @TempDir Path directory;
    @Test void oldConfigurationsUseCautiousDTapWithoutEnablingIt() throws Exception {
        var path=directory.resolve("features.json");
        java.nio.file.Files.writeString(path,"{\"schemaVersion\":2,\"options\":{\"dTap\":false}}");
        var f=new FeatureConfig(path);f.load();
        assertTrue(f.options.dTapCautious);assertFalse(f.options.dTap);
        f.options.dTapCautious=false;f.save();
        var next=new FeatureConfig(path);next.load();assertFalse(next.options.dTapCautious);
    }
    @Test void everyGameplayModuleStartsOff(){var f=new FeatureConfig(directory.resolve("features.json"));for(String id:ModuleCatalog.IDS)assertFalse(ModuleCatalog.enabled(f,id),id);}
    @Test void everyModuleHasIndependentToggle(){var f=new FeatureConfig(directory.resolve("features.json"));for(String id:ModuleCatalog.IDS){ModuleCatalog.toggle(f,id);assertTrue(ModuleCatalog.enabled(f,id),id);ModuleCatalog.toggle(f,id);assertFalse(ModuleCatalog.enabled(f,id),id);}}
    @Test void invalidOptionsAreBounded(){var o=new ModuleOptions();o.zoomFactor=Float.NaN;o.freecamSpeed=Float.POSITIVE_INFINITY;o.crystalRange=-30;o.lineWidth=500;o.hitboxWidth=-3;o.worldTime=-1;o.weather=100;o.totemDelay=0;o.normalize();assertEquals(4,o.zoomFactor);assertEquals(.5f,o.freecamSpeed);assertEquals(2,o.crystalRange);assertEquals(4,o.lineWidth);assertEquals(1,o.hitboxWidth);assertEquals(23999,o.worldTime);assertEquals(2,o.weather);assertEquals(2,o.totemDelay);}
    @Test void settingsPersistWithoutOptingInOtherModules() throws Exception {var path=directory.resolve("features.json");var f=new FeatureConfig(path);f.options.zoom=true;f.options.zoomFactor=7;f.options.espTracers=true;f.options.espColor=0xFF123456;f.save();var next=new FeatureConfig(path);next.load();assertTrue(next.options.zoom);assertEquals(7,next.options.zoomFactor);assertTrue(next.options.espTracers);assertEquals(0xFF123456,next.options.espColor);assertFalse(next.playerEsp);assertFalse(next.options.freecam);}
    @Test void olderConfigurationGetsDisabledNewModules() throws Exception {var path=directory.resolve("features.json");java.nio.file.Files.writeString(path,"{\"schemaVersion\":2,\"crystalTotem\":true}");var f=new FeatureConfig(path);f.load();assertTrue(f.crystalTotem);assertFalse(f.options.freecam);assertFalse(f.options.fullBright);assertFalse(f.options.zoom);}
}
