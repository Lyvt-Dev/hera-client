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
    private static final class Data { int schemaVersion; HudModule[] modules; int accent=0xFFF49B63; boolean animations; java.util.Map<String,String> presets; }
    public final java.util.Set<String> advancedSettings=new java.util.HashSet<>();
    public int accent=0xFFF49B63;public boolean animations;public final Map<String,String> presets=new LinkedHashMap<>();
    public final List<HudModule> modules = new ArrayList<>();
    public static final List<String> IDS = List.of("FPS", "Koordinaten", "Ping", "Speicher", "Uhr", "Richtung", "Geschwindigkeit", "Biome", "Server", "Keystrokes", "Armor HUD", "CPS", "Effekte", "Sprint", "Session", "Inventar", "Spotify");
    public HudConfig(Path file) { this.file = file; reset(); }
    public void reset() { modules.clear(); for (int i = 0; i < IDS.size(); i++) modules.add(new HudModule(IDS.get(i), i>=11?.55f:i>=9?.78f:.025f, i>=11?.1f+(i-11)*.12f:i>=9?.35f+(i-9)*.3f:.10f+i*.09f)); }
    public void load() {
        if (!Files.exists(file)) return;
        try {
            if (Files.size(file) > 524288) throw new IOException("Config too large");
            var json = JsonParser.parseString(Files.readString(file));
            boolean legacy = json.isJsonArray();
            Data data = legacy ? null : GSON.fromJson(json, Data.class);
            if (!legacy && (data == null || data.schemaVersion != 2)) throw new IOException("Unknown config version");
            if(data!=null){accent=data.accent|0xFF000000;animations=data.animations;if(data.presets!=null)data.presets.entrySet().stream().filter(e->e.getKey()!=null&&e.getKey().length()<=32&&e.getValue()!=null&&e.getValue().length()<=32768).limit(8).forEach(e->presets.put(e.getKey(),e.getValue()));}
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
    public String snapshot(){Data d=new Data();d.schemaVersion=2;d.modules=modules.toArray(HudModule[]::new);d.accent=accent;d.animations=animations;return GSON.toJson(d);}
    public void restore(String snapshot){Data d=GSON.fromJson(snapshot,Data.class);if(d==null||d.modules==null||d.schemaVersion!=2)throw new IllegalArgumentException("Invalid layout");for(var current:List.copyOf(modules))for(var m:d.modules)if(m!=null&&current.id.equals(m.id)){m.normalize();modules.set(modules.indexOf(current),m);break;}accent=d.accent|0xFF000000;animations=d.animations;}
    public void savePreset(String name){String n=name.strip();if(List.of("Hera Default","Competitive","Minimal").contains(n)||n.isEmpty()||n.length()>32||(!presets.containsKey(n)&&presets.size()>=8))throw new IllegalArgumentException("Max. 8 Presets, Name 1–32 Zeichen.");presets.put(n,snapshot());}
    public void applyPreset(String name){
        if(presets.containsKey(name)){restore(presets.get(name));return;}
        if(!List.of("Hera Default","Competitive","Minimal").contains(name))throw new IllegalArgumentException("Unknown preset");
        reset();animations=false;for(int i=0;i<modules.size();i++){var m=modules.get(i);m.x=.02f;m.y=.08f+i*.045f;m.padding=name.equals("Competitive")?3:5;m.radius=3;m.vanillaFont=false;
            m.enabled=List.of("FPS","Ping","Koordinaten").contains(m.id);if(name.equals("Minimal")){m.background=0;m.enabled=m.id.equals("FPS")||m.id.equals("Koordinaten");}
            if(m.id.equals("Keystrokes")){m.anchorX=1;m.x=.025f;m.y=.35f;m.enabled=name.equals("Hera Default");}
            if(m.id.equals("Armor HUD")){m.anchorX=1;m.x=.025f;m.y=.75f;m.enabled=!name.equals("Minimal");}
        }
    }
    public void save() throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "hera-hud-", ".tmp");
        try {
            Data data = new Data(); data.schemaVersion = 2; data.modules = modules.toArray(HudModule[]::new);data.accent=accent;data.animations=animations;data.presets=presets;
            Files.writeString(temporary, GSON.toJson(data));
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
