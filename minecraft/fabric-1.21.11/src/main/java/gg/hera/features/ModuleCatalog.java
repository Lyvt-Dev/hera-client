package gg.hera.features;
import java.util.List;
public final class ModuleCatalog {
    public static final List<String> IDS=List.of("Rüstung ausblenden","Kristall-Totem","Auto-Totem","Fast Items","Item-ESP","Spieler-ESP","Mob-ESP","Hitboxes","TNT Timer","Item Highlighter","No Fog","Time Changer","Weather Changer","Zoom","Free Look","Full Bright","Freecam");
    private ModuleCatalog(){}
    public static boolean enabled(FeatureConfig f,String id){var o=f.options;return switch(id){
        case "Kristall-Totem"->f.crystalTotem;case "Auto-Totem"->f.autoTotem;case "Fast Items"->f.fastItems;case "Item-ESP"->f.itemEsp;case "Spieler-ESP"->f.playerEsp;case "Mob-ESP"->f.mobEsp;
        case "Rüstung ausblenden"->o.hideOwnArmor;case "Hitboxes"->o.hitboxes;case "TNT Timer"->o.tntTimer;case "Item Highlighter"->o.itemHighlighter;case "No Fog"->o.noFog;case "Time Changer"->o.timeChanger;case "Weather Changer"->o.weatherChanger;case "Zoom"->o.zoom;case "Free Look"->o.freeLook;case "Full Bright"->o.fullBright;case "Freecam"->o.freecam;default->false;};}
    public static void toggle(FeatureConfig f,String id){var o=f.options;switch(id){
        case "Kristall-Totem"->f.crystalTotem=!f.crystalTotem;case "Auto-Totem"->f.autoTotem=!f.autoTotem;case "Fast Items"->f.fastItems=!f.fastItems;case "Item-ESP"->f.itemEsp=!f.itemEsp;case "Spieler-ESP"->f.playerEsp=!f.playerEsp;case "Mob-ESP"->f.mobEsp=!f.mobEsp;
        case "Rüstung ausblenden"->o.hideOwnArmor=!o.hideOwnArmor;case "Hitboxes"->o.hitboxes=!o.hitboxes;case "TNT Timer"->o.tntTimer=!o.tntTimer;case "Item Highlighter"->o.itemHighlighter=!o.itemHighlighter;case "No Fog"->o.noFog=!o.noFog;case "Time Changer"->o.timeChanger=!o.timeChanger;case "Weather Changer"->o.weatherChanger=!o.weatherChanger;case "Zoom"->o.zoom=!o.zoom;case "Free Look"->o.freeLook=!o.freeLook;case "Full Bright"->o.fullBright=!o.fullBright;case "Freecam"->o.freecam=!o.freecam;default->{}}}
    public static String description(String id){return switch(id){case "Rüstung ausblenden"->"Dein Skin ohne sichtbare Rüstung";case "Hitboxes"->"Spieler, Mobs & Item-Boxen";case "TNT Timer"->"Zeit bis zur Explosion";case "Item Highlighter"->"Ausgewählte Inventar-Items";case "No Fog"->"Nebel lokal ausblenden";case "Time Changer"->"Deine Tageszeit";case "Weather Changer"->"Dein Wetter";case "Zoom"->"C halten zum Vergrößern";case "Free Look"->"Alt halten zum Umschauen";case "Full Bright"->"Volle Umgebungshelligkeit";case "Freecam"->"Kamera lösen · Taste V";default->"";};}
}
