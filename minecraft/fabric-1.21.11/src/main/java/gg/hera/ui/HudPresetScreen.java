package gg.hera.ui;
import gg.hera.hud.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
public final class HudPresetScreen extends Screen {
    private final Screen parent;private final HudConfig config;private final HudRenderer renderer;private TextFieldWidget name;private String error="";private String pending="";private int scroll;
    public HudPresetScreen(Screen parent,HudConfig config,HudRenderer renderer){super(HeraTheme.text("HUD Presets"));this.parent=parent;this.config=config;this.renderer=renderer;}
    @Override protected void init(){clearChildren();int x=(width-280)/2,y=Math.max(12,(height-300)/2);int row=0;var all=new java.util.ArrayList<>(java.util.List.of("Hera Default","Competitive","Minimal"));all.addAll(config.presets.keySet());int count=Math.max(1,(height-145)/25);scroll=Math.clamp(scroll,0,Math.max(0,all.size()-count));
        addDrawableChild(new HeraButton(x+166,y+8,114,22,config.animations?"Animation an":"Animation aus",false,()->{config.animations=!config.animations;init();}));
        for(int i=scroll;i<Math.min(all.size(),scroll+count);i++){String preset=all.get(i);int r=row++;addDrawableChild(new HeraButton(x,y+40+r*25,280,22,preset,false,()->apply(preset)));}
        int controls=Math.min(height-72,y+44+row*25);name=new TextFieldWidget(textRenderer,x,controls,174,22,HeraTheme.text("Preset-Name"));name.setMaxLength(32);name.setPlaceholder(HeraTheme.text("Eigener Preset-Name"));addDrawableChild(name);
        addDrawableChild(new HeraButton(x+180,controls,100,22,"Speichern",true,()->{try{config.savePreset(name.getText());config.save();close();}catch(Exception e){error="Name 1–32 Zeichen · maximal 8 Presets";}}));
        addDrawableChild(new HeraButton(x,height-30,280,22,"Zurück",false,this::close));
    }
    private void apply(String name){if(!pending.equals(name)){error="Ersetzt das Layout. Erneut wählen zum Bestätigen.";pending=name;return;}try{config.applyPreset(name);renderer.invalidate();config.save();close();}catch(Exception e){error="Preset konnte nicht geladen werden.";}}
    @Override public void render(DrawContext c,int mx,int my,float d){c.fill(0,0,width,height,0xF0111318);HeraTheme.title(c,"HUD Presets",(width-280)/2,Math.max(12,(height-300)/2)+12,1.2f,HeraTheme.TEXT);if(!error.isEmpty())HeraTheme.label(c,HeraTheme.fit(error,width-24),12,height-46,HeraTheme.ACCENT);super.render(c,mx,my,d);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){scroll+=(int)-Math.signum(vertical);clearChildren();init();return true;}
    @Override public void close(){try{config.save();renderer.invalidate();client.setScreen(parent);}catch(Exception e){error="Speichern fehlgeschlagen.";}}
    @Override public boolean shouldPause(){return false;}
}
