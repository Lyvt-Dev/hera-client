package gg.hera.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("Hera");
    private final Path file;
    public final List<HudModule> modules = new ArrayList<>();
    public static final List<String> IDS = List.of("FPS", "Koordinaten", "Ping", "Speicher", "Uhr", "Richtung", "Geschwindigkeit", "Biome", "Server");
    public HudConfig(Path file) { this.file = file; reset(); }
    public void reset() { modules.clear(); for (int i = 0; i < IDS.size(); i++) modules.add(new HudModule(IDS.get(i), .02f, .03f + i * .045f)); }
    public void load() {
        if (!Files.exists(file)) return;
        try {
            if (Files.size(file) > 65536) throw new IOException("Config too large");
            HudModule[] loaded = GSON.fromJson(Files.readString(file), HudModule[].class);
            if (loaded == null) throw new IOException("Empty config");
            for (HudModule original : modules) {
                for (HudModule candidate : loaded) {
                    if (candidate != null && original.id.equals(candidate.id)) {
                        candidate.normalize();
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
            Files.writeString(temporary, GSON.toJson(modules));
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
