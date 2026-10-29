package gg.hera.features;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class PresentationConfigTest {
 @TempDir Path dir;
 @Test void allNewFeaturesAreOptInAndPersist(){var f=new FeatureConfig(dir.resolve("features.json"));for(String id:Presentation.IDS)assertFalse(f.options.presentation.getOrDefault(id,false));f.options.presentation.put("Clear Lava",true);f.options.countedItems.add("minecraft:arrow");f.options.hiddenParticles.add("minecraft:smoke");try{f.save();}catch(Exception e){throw new AssertionError(e);}var loaded=new FeatureConfig(dir.resolve("features.json"));loaded.load();assertTrue(loaded.options.presentation.get("Clear Lava"));assertTrue(loaded.options.countedItems.contains("minecraft:arrow"));assertTrue(loaded.options.hiddenParticles.contains("minecraft:smoke"));}
 @Test void invalidAndUnboundedConfigurationIsClamped(){var o=new ModuleOptions();o.itemScale=Float.NaN;o.itemX=100;o.capeStrength=99;o.presentation.put("fake",true);var bad=new ModuleOptions.Waypoint();bad.x=Double.NaN;o.waypoints.add(bad);o.normalize();assertEquals(1,o.itemScale);assertEquals(1,o.itemX);assertEquals(2,o.capeStrength);assertTrue(o.waypoints.isEmpty());assertFalse(o.presentation.containsKey("fake"));}
 @Test void tpsIsAnEstimateWithNoInventedInitialValue(){assertTrue(Double.isNaN(Presentation.estimateTps(0,0)));assertEquals(10,Presentation.estimateTps(20,2_000_000_000L));assertEquals(20,Presentation.estimateTps(20,100_000_000L));}
 @Test void companionIsOptInAndSettingsSurviveRestart()throws Exception{var f=new FeatureConfig(dir.resolve("companion.json"));assertFalse(ModuleCatalog.enabled(f,"Hera Companion"));assertFalse(f.options.companionVoice);f.options.presentation.put("Hera Companion",true);f.options.companionMode=2;f.options.companionVoice=true;f.options.companionVolume=.25f;f.save();var loaded=new FeatureConfig(dir.resolve("companion.json"));loaded.load();assertTrue(ModuleCatalog.enabled(loaded,"Hera Companion"));assertEquals(2,loaded.options.companionMode);assertTrue(loaded.options.companionVoice);assertEquals(.25f,loaded.options.companionVolume);}
 @Test void companionSettingsAreBounded(){var o=new ModuleOptions();o.companionMode=99;o.companionVolume=Float.NaN;o.normalize();assertEquals(2,o.companionMode);assertEquals(.5f,o.companionVolume);}
}
