package gg.hera.features;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class FeatureConfigTest {
    @TempDir Path directory;
    @Test void defaultsDisabledAndPersistsExplicitOptIn() throws Exception {
        Path file = directory.resolve("hera/features.json");
        var config = new FeatureConfig(file); config.load(); assertFalse(config.crystalTotem);
        config.crystalTotem = true; config.save();
        var reloaded = new FeatureConfig(file); reloaded.load(); assertTrue(reloaded.crystalTotem);
    }
    @Test void invalidConfigKeepsSafeDefault() throws Exception {
        Path file = directory.resolve("features.json");
        Files.writeString(file, "invalid json");
        var config = new FeatureConfig(file); config.load(); assertFalse(config.crystalTotem);
    }
}
