package gg.hera.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("Hera");
    private final Path file;
    private static final class Data { int schemaVersion; HudModule[] modules; }
    public final List<HudModule> modules = new ArrayList<>();
    public static final List<String> IDS = List.of("FPS", "Koordinaten", "Ping", "Speicher", "Uhr", "Richtung", "Geschwindigkeit", "Biome", "Server", "Keystrokes", "Armor HUD");
    public HudConfig(Path file) { this.file = file; reset(); }
    public void reset() { modules.clear(); for (int i = 0; i < IDS.size(); i++) modules.add(new HudModule(IDS.get(i), i>=9?.78f:.025f, i>=9?.35f+(i-9)*.3f:.10f+i*.09f)); }
    public void load() {
        if (!Files.exists(file)) return;
        try {
            if (Files.size(file) > 65536) throw new IOException("Config too large");
            var json = JsonParser.parseString(Files.readString(file));
            boolean legacy = json.isJsonArray();
            Data data = legacy ? null : GSON.fromJson(json, Data.class);
            if (!legacy && (data == null || data.schemaVersion != 2)) throw new IOException("Unknown config version");
            HudModule[] loaded = legacy ? GSON.fromJson(json, HudModule[].class) : data.modules;
            if (loaded == null) throw new IOException("Empty config");
            for (HudModule original : modules) {
                for (HudModule candidate : loaded) {
                    if (candidate != null && original.id.equals(candidate.id)) {
                        candidate.normalize();
                        if (legacy) { candidate.enabled = false; candidate.color = 0xFFE9EDF5; candidate.background = 0xA61A202B; }
                        int index = modules.indexOf(original);
                        modules.set(index, candidate);
                        break;
                    }
                }
            }
        } catch (IOException | RuntimeException e) { LOGGER.warn("Could not load Hera HUD config; defaults retained", e); }
    }
    public void save() throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "hera-hud-", ".tmp");
        try {
            Data data = new Data(); data.schemaVersion = 2; data.modules = modules.toArray(HudModule[]::new);
            Files.writeString(temporary, GSON.toJson(data));
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
