package gg.hera.ui;
import gg.hera.features.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import java.util.*;
import java.util.function.*;
import java.io.IOException;
import gg.hera.hud.*;
public final class ModuleSettingsScreen extends Screen {
    public record Setting(Supplier<String> label,Runnable change){}
    private final Screen parent;private final FeatureConfig f;private final String id;private final List<Setting> settings=new ArrayList<>();
    private int x,y,w,h,page,rows;private String error="";
    private HudConfig hud;private HudRenderer renderer;private boolean embedded;
    public ModuleSettingsScreen(Screen parent,FeatureConfig f,String id){super(HeraTheme.text(id));this.parent=parent;this.f=f;this.id=id;}
    public ModuleSettingsScreen(Screen parent,FeatureConfig f,String id,HudConfig hud,HudRenderer renderer){this(parent,f,id);this.hud=hud;this.renderer=renderer;}
    private void option(Supplier<String> label,Runnable action){settings.add(new Setting(label,action));}
    private void flag(String label,BooleanSupplier value,Runnable action){option(()->label+": "+(value.getAsBoolean()?"An":"Aus"),action);}
    private void color(String label,IntSupplier value,IntConsumer change){option(()->label+" →",()->client.setScreen(new ColorPickerScreen(returnTarget(),value.getAsInt(),change)));}
    @Override protected void init(){
        w=Math.min(380,width-20);h=Math.min(340,height-20);x=(width-w)/2;y=(height-h)/2;rows=Math.max(1,(h-110)/29);collectSettings();rebuild();
    }
    public List<Setting> embedded(){embedded=true;collectSettings();return List.copyOf(settings);}
    private Screen returnTarget(){return embedded?parent:this;}
    private void collectSettings(){settings.clear();var o=f.options;
        var module=hud==null?null:hud.modules.stream().filter(m->m.id.equals(id)).findFirst().orElse(null);
        if(module==null)flag("Modul",()->ModuleCatalog.enabled(f,id),()->ModuleCatalog.toggle(f,id));
        else {
            flag("Modul",()->module.enabled,()->module.enabled=!module.enabled);
            option(()->String.format(Locale.ROOT,"Größe: %.1f×",module.scale),()->{module.scale=module.scale>=3?.5f:module.scale+.1f;module.normalize();});
            color("Textfarbe",()->module.color,v->module.color=v);
            option(()->"Hintergrund: "+(module.background==0?"Aus":"An"),()->module.background=module.background==0?0xC01A202B:0);
            option(()->"Position im HUD-Editor →",()->client.setScreen(new HudEditorScreen(hud,renderer,f)));
            if(id.equals("Keystrokes")){flag("Mausklicks",()->o.keyMouse,()->o.keyMouse=!o.keyMouse);flag("Leertaste",()->o.keySpace,()->o.keySpace=!o.keySpace);}
            if(id.equals("Armor HUD"))flag("Haltbarkeit in Prozent",()->o.armorDurability,()->o.armorDurability=!o.armorDurability);
        }
        switch(id){
            case "D-Tap"->{flag("Vorsichtig: Fadenkreuz & Erholung",()->o.dTapCautious,()->o.dTapCautious=!o.dTapCautious);option(()->"Mindest-Zielschaden: "+o.dTapTarget,()->o.dTapTarget=o.dTapTarget>=30?2:o.dTapTarget+1);option(()->"Max. Eigenschaden: "+o.dTapSelf,()->o.dTapSelf=o.dTapSelf>=30?2:o.dTapSelf+1);option(()->"Lebensreserve: "+o.dTapReserve,()->o.dTapReserve=o.dTapReserve>=20?2:o.dTapReserve+1);option(()->"Aktionspause: "+DTapPolicy.actionDelay(o.dTapDelay,o.dTapCautious)+" Ticks",()->o.dTapDelay=o.dTapDelay%10+1);flag("Statusmeldungen",()->o.dTapFeedback,()->o.dTapFeedback=!o.dTapFeedback);}
            case "Rüstung ausblenden"->{flag("Helm ausblenden",()->o.hideHelmet,()->o.hideHelmet=!o.hideHelmet);flag("Brustplatte ausblenden",()->o.hideChest,()->o.hideChest=!o.hideChest);flag("Hose ausblenden",()->o.hideLegs,()->o.hideLegs=!o.hideLegs);flag("Stiefel ausblenden",()->o.hideBoots,()->o.hideBoots=!o.hideBoots);}
            case "Item-ESP","Spieler-ESP","Mob-ESP"->{
                flag("Tracer",()->o.espTracers,()->o.espTracers=!o.espTracers);flag("Box-Umrandung",()->o.espBoxes,()->o.espBoxes=!o.espBoxes);flag("Entity-Leuchten",()->o.espGlow,()->o.espGlow=!o.espGlow);
                flag("Block-Beschriftungen",()->o.espLabels,()->o.espLabels=!o.espLabels);option(()->"Tracer-Start: "+(o.tracerCenter?"Bildmitte":"Unten"),()->o.tracerCenter=!o.tracerCenter);
                color("ESP-Farbe",()->o.espColor,v->o.espColor=v);option(()->"Linienstärke: "+o.lineWidth+" px",()->o.lineWidth=o.lineWidth%4+1);
                option(()->"Reichweite: "+f.espRange+" m",()->f.espRange=f.espRange>=128?16:f.espRange*2);
                option(()->"Items & Container auswählen →",()->client.setScreen(new ItemSelectionScreen(returnTarget(),f)));
            }
            case "Hitboxes"->{flag("Spieler",()->o.hitboxPlayers,()->o.hitboxPlayers=!o.hitboxPlayers);flag("Mobs",()->o.hitboxMobs,()->o.hitboxMobs=!o.hitboxMobs);flag("Item-Drops",()->o.hitboxItems,()->o.hitboxItems=!o.hitboxItems);color("Farbe",()->o.hitboxColor,v->o.hitboxColor=v);option(()->"Linienstärke: "+o.hitboxWidth+" px",()->o.hitboxWidth=o.hitboxWidth%4+1);option(()->"Reichweite: "+f.espRange+" m",()->f.espRange=f.espRange>=128?16:f.espRange*2);}
            case "TNT Timer"->{color("Timer-Farbe",()->o.tntColor,v->o.tntColor=v);option(()->"Reichweite: "+f.espRange+" m",()->f.espRange=f.espRange>=128?16:f.espRange*2);}
            case "Item Highlighter"->{color("Markierungsfarbe",()->o.highlightColor,v->o.highlightColor=v);option(()->"Items auswählen →",()->client.setScreen(new ItemSelectionScreen(returnTarget(),f,false)));}
            case "Time Changer"->option(()->"Tageszeit: "+o.worldTime+" Ticks",()->o.worldTime=(o.worldTime+1000)%24000);
            case "Weather Changer"->option(()->"Wetter: "+List.of("Klar","Regen","Gewitter").get(o.weather),()->o.weather=(o.weather+1)%3);
            case "Zoom"->option(()->"Vergrößerung: "+o.zoomFactor+"×",()->o.zoomFactor=o.zoomFactor>=12?2:o.zoomFactor+1);
            case "Freecam"->option(()->String.format(Locale.ROOT,"Tempo: %.1f Blöcke/Tick",o.freecamSpeed),()->o.freecamSpeed=o.freecamSpeed>=3?.1f:o.freecamSpeed+.1f);
            case "Free Look"->option(()->"Kameraabstand: "+o.lookDistance+" m",()->o.lookDistance=o.lookDistance>=8?1:o.lookDistance+1);
            case "Full Bright"->option(()->"Helligkeit: "+Math.round(o.brightness*100)+"%",()->o.brightness=o.brightness>=1?.2f:o.brightness+.2f);
            case "No Fog"->{flag("Unterwassernebel entfernen",()->o.fogWater,()->o.fogWater=!o.fogWater);flag("Lavanebel entfernen",()->o.fogLava,()->o.fogLava=!o.fogLava);}
            case "Kristall-Totem"->option(()->"Kristall-Reichweite: "+o.crystalRange+" m",()->o.crystalRange=o.crystalRange>=16?2:o.crystalRange+1);
            case "Auto-Totem"->option(()->"Pause: "+o.totemDelay+" Ticks",()->o.totemDelay=o.totemDelay>=40?2:o.totemDelay+2);
            case "Fast Items"->{flag("3D-Items drehen",()->o.fastItems3d,()->o.fastItems3d=!o.fastItems3d);flag("2D-Seiten behalten",()->o.fastItemsSides,()->o.fastItemsSides=!o.fastItemsSides);flag("Schatten",()->o.fastItemsShadows,()->o.fastItemsShadows=!o.fastItemsShadows);}
            default->{}
        }
    }
    private void rebuild(){clearChildren();page=Math.clamp(page,0,Math.max(0,(settings.size()-1)/rows));for(int i=0;i<rows && page*rows+i<settings.size();i++){var s=settings.get(page*rows+i);addDrawableChild(new HeraButton(x+14,y+46+i*29,w-28,25,s.label.get(),false,()->{s.change.run();f.options.normalize();if(client.currentScreen==this)rebuild();}));}
        addDrawableChild(new HeraButton(x+14,y+h-30,26,20,"‹",false,()->{page--;rebuild();}));addDrawableChild(new HeraButton(x+44,y+h-30,26,20,"›",false,()->{page++;rebuild();}));addDrawableChild(new HeraButton(x+w-90,y+h-30,76,20,"Fertig",true,this::close));}
    @Override public void render(DrawContext c,int mx,int my,float d){c.fill(0,0,width,height,0x88080B11);HeraTheme.panel(c,x,y,w,h);HeraTheme.title(c,id,x+14,y+15,1.2f,HeraTheme.TEXT);super.render(c,mx,my,d);HeraTheme.label(c,HeraTheme.fit(error.isEmpty()?hint():error,w-28),x+14,y+h-48,HeraTheme.MUTED);HeraTheme.label(c,(page+1)+" / "+(Math.max(0,(settings.size()-1)/rows)+1),x+82,y+h-23,HeraTheme.MUTED);}
    private String hint(){return switch(id){case "Freecam"->"WASD · Leertaste/Shift · V zum Zurückkehren";case "Free Look"->"Linkes Alt halten · Bewegung bleibt unverändert";case "Zoom"->"C halten · Tasten in Minecraft neu belegbar";case "No Fog","Full Bright","Time Changer","Weather Changer"->"Lokale Darstellung · Server bleibt unverändert";default->"Einstellungen werden mit Fertig gespeichert.";};}
    @Override public void close(){try{f.save();if(hud!=null)hud.save();client.setScreen(parent);}catch(IOException e){error="Speichern fehlgeschlagen.";}}
    @Override public boolean shouldPause(){return false;}
}
