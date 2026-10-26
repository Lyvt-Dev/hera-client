package gg.hera.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import org.slf4j.LoggerFactory;

public final class FeatureConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    public boolean crystalTotem = false;
    public static final double RANGE = 8.0;
    private static final class Data { boolean crystalTotem; }
    public FeatureConfig(Path file) { this.file = file; }
    public void load() {
        if (!Files.exists(file)) return;
        try {
            if (Files.size(file) > 4096) throw new IOException("Config too large");
            Data data = GSON.fromJson(Files.readString(file), Data.class);
            if (data != null) crystalTotem = data.crystalTotem;
        } catch (IOException | RuntimeException e) { LoggerFactory.getLogger("Hera").warn("Feature config could not be loaded; disabled defaults retained"); }
    }
    public void save() throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "hera-features-", ".tmp");
        try {
            Data data = new Data(); data.crystalTotem = crystalTotem;
            Files.writeString(temporary, GSON.toJson(data));
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
