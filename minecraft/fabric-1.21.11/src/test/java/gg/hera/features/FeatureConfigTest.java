package gg.hera.features;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class FeatureConfigTest {
    @Test void newModulesDefaultOffAndSelectionPersists() throws Exception {
        Path file=directory.resolve("new.json");var c=new FeatureConfig(file);
        assertFalse(c.autoTotem||c.fastItems||c.itemEsp||c.playerEsp||c.mobEsp);assertTrue(c.markedItems.isEmpty());
        c.itemEsp=true;c.autoTotem=true;c.markedItems.add("minecraft:diamond");c.save();
        var loaded=new FeatureConfig(file);loaded.load();assertTrue(loaded.itemEsp);assertTrue(loaded.autoTotem);assertEquals(c.markedItems,loaded.markedItems);assertFalse(loaded.fastItems);
    }
    @TempDir Path directory;
    @Test void defaultsDisabledAndPersistsExplicitOptIn() throws Exception {
        Path file = directory.resolve("hera/features.json");
        var config = new FeatureConfig(file); config.load(); assertFalse(config.crystalTotem);
        config.crystalTotem = true; config.save();
        var reloaded = new FeatureConfig(file); reloaded.load(); assertTrue(reloaded.crystalTotem);
    }
    @Test void legacyFeatureOptInNeedsFreshConsent() throws Exception {
        Path file = directory.resolve("features.json");Files.writeString(file,"{\"crystalTotem\":true}");
        var config = new FeatureConfig(file);config.load();assertFalse(config.crystalTotem);
    }
    @Test void invalidConfigKeepsSafeDefault() throws Exception {
        Path file = directory.resolve("features.json");
        Files.writeString(file, "invalid json");
        var config = new FeatureConfig(file); config.load(); assertFalse(config.crystalTotem);
    }
}
