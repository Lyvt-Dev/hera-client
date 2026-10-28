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
    public boolean autoTotem, fastItems, itemEsp, playerEsp, mobEsp;
    public ModuleOptions options = new ModuleOptions();
    public int espRange = 64;
    public java.util.Set<String> markedItems = new java.util.LinkedHashSet<>();
    public static final double RANGE = 8.0;
    private static final class Data { int schemaVersion; boolean crystalTotem, autoTotem, fastItems, itemEsp, playerEsp, mobEsp; int espRange=64; java.util.Set<String> markedItems; ModuleOptions options; }
    public FeatureConfig(Path file) { this.file = file; }
    public void load() {
        if (!Files.exists(file)) return;
        try {
            if (Files.size(file) > 65536) throw new IOException("Config too large");
            Data data = GSON.fromJson(Files.readString(file), Data.class);
            if (data != null) crystalTotem = data.schemaVersion == 2 && data.crystalTotem;
            if(data != null && data.schemaVersion == 2) {
                autoTotem=data.autoTotem; fastItems=data.fastItems; itemEsp=data.itemEsp; playerEsp=data.playerEsp; mobEsp=data.mobEsp;
                espRange=Math.clamp(data.espRange,16,128);
                markedItems=ItemSelection.clean(data.markedItems);
                options=data.options==null?new ModuleOptions():data.options;options.normalize();
            }
        } catch (IOException | RuntimeException e) { LoggerFactory.getLogger("Hera").warn("Feature config could not be loaded; disabled defaults retained"); }
    }
    public void save() throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "hera-features-", ".tmp");
        try {
            Data data = new Data(); data.schemaVersion = 2; data.crystalTotem = crystalTotem;
            data.autoTotem=autoTotem;data.fastItems=fastItems;data.itemEsp=itemEsp;data.playerEsp=playerEsp;data.mobEsp=mobEsp;
            data.espRange=Math.clamp(espRange,16,128);data.markedItems=ItemSelection.clean(markedItems);
            options.normalize();data.options=options;
            String encoded=GSON.toJson(data);if(encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw new IOException("Zu viele Einträge in der Konfiguration. Auswahl verkleinern.");
            Files.writeString(temporary, encoded);
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
