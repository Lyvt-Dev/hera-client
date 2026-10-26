package gg.hera.ui;

import gg.hera.hud.*;
import gg.hera.features.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import java.io.IOException;
import java.util.*;

public final class ModuleScreen extends Screen {
    private final HudConfig hud;private final HudRenderer renderer;private final FeatureConfig features;
    private String search="",category="Alle Module",selected="Item-ESP",error="";
    private int listScroll,detailScroll,canvasWidth,canvasHeight;private float uiScale;
    private TextFieldWidget field;private WorkspaceLayout l;
    private List<ModuleSettingsScreen.Setting> controls=List.of();
    public ModuleScreen(HudConfig hud,HudRenderer renderer,FeatureConfig features){super(HeraTheme.text("Hera · Dein Setup"));this.hud=hud;this.renderer=renderer;this.features=features;}
    private HudModule hud(String id){return hud.modules.stream().filter(m->m.id.equals(id)).findFirst().orElse(null);}
    private boolean enabled(String id){var m=hud(id);return m==null?ModuleCatalog.enabled(features,id):m.enabled;}
    private void toggle(String id){var m=hud(id);if(m==null)ModuleCatalog.toggle(features,id);else m.enabled=!m.enabled;}
    private String group(String id){if(hud(id)!=null)return "HUD";return switch(id){case "Kristall-Totem","Auto-Totem","Zoom","Free Look","Freecam"->"Gameplay";default->"Darstellung";};}
    private String description(String id){String d=ModuleCatalog.description(id);if(!d.isEmpty())return d;return switch(id){case "Item-ESP"->"Markiere ausgewählte Items und Blöcke.";case "Spieler-ESP"->"Behalte geladene Spieler im Blick.";case "Mob-ESP"->"Markiere Mobs mit deinem eigenen Stil.";case "Fast Items"->"Passe die Darstellung von Item-Drops an.";case "Auto-Totem"->"Fülle deine Nebenhand mit einem Totem nach.";case "Kristall-Totem"->"Wähle ein Hotbar-Totem bei Kristall-Gefahr.";case "Keystrokes"->"Deine Tasten und Mausklicks im Spiel.";case "Armor HUD"->"Rüstung und Haltbarkeit auf einen Blick.";default->"Position, Größe und Farben für dein HUD.";};}
    private List<String> ids(){var all=new ArrayList<>(ModuleCatalog.IDS);hud.modules.forEach(m->all.add(m.id));String q=search.toLowerCase(Locale.ROOT);return all.stream().filter(id->category.equals("Alle Module")||category.equals("Aktiv")&&enabled(id)||category.equals(group(id))).filter(id->id.toLowerCase(Locale.ROOT).contains(q)||description(id).toLowerCase(Locale.ROOT).contains(q)).toList();}
    private void revealSelection(){int i=ids().indexOf(selected);if(i>=0){if(i<listScroll)listScroll=i;else if(i>=listScroll+l.visibleRows())listScroll=i-l.visibleRows()+1;}}
    public void select(String id){selected=id;detailScroll=0;revealSelection();rebuild();}
    @Override protected void init(){
        float physical=client.getWindow().getWidth()>=1600?2:1.5f;
        uiScale=Math.min(physical/(float)client.getWindow().getScaleFactor(),Math.min(width/740f,height/460f));
        canvasWidth=Math.round(width/uiScale);canvasHeight=Math.round(height/uiScale);l=WorkspaceLayout.of(canvasWidth,canvasHeight);revealSelection();
        field=new TextFieldWidget(textRenderer,l.listX()+19,l.y()+83,l.list()-38,18,HeraTheme.text("Module suchen"));
        field.setDrawsBackground(false);field.setMaxLength(80);field.addFormatter((v,o)->HeraTheme.text(v).asOrderedText());field.setText(search);field.setPlaceholder(HeraTheme.text("Suchen …"));
        field.setChangedListener(v->{search=v;listScroll=0;rebuild();});rebuild();
    }
    private void rebuild(){
        if(l==null)return;boolean searchFocused=field.isFocused();clearChildren();addDrawableChild(field);if(searchFocused)setFocused(field);
        int nav=l.y()+96;for(String name:List.of("Alle Module","Aktiv","HUD","Gameplay","Darstellung")){
            addDrawableChild(new HeraButton(l.x()+12,nav,l.rail()-24,30,name,false,()->{category=name;listScroll=0;detailScroll=0;if(!ids().isEmpty()&&!ids().contains(selected))selected=ids().getFirst();rebuild();}){
                @Override protected void renderWidget(DrawContext c,int mx,int my,float d){boolean current=category.equals(name);if(current||isHovered())HeraTheme.round(c,getX(),getY(),width,height,6,current?0xFF302B29:0xFF202630);HeraTheme.label(c,name,getX()+11,getY()+11,current?HeraTheme.ACCENT:HeraTheme.MUTED);if(current)HeraTheme.round(c,getX(),getY()+8,2,14,1,HeraTheme.ACCENT);}
            });nav+=38;
        }
        addDrawableChild(new HeraButton(l.x()+12,l.bottom()-37,l.rail()-24,27,"HUD anordnen",false,()->client.setScreen(new HudEditorScreen(hud,renderer,features))));
        var ids=ids();listScroll=Math.clamp(listScroll,0,Math.max(0,ids.size()-l.visibleRows()));
        for(int row=0;row<l.visibleRows()&&listScroll+row<ids.size();row++){
            String id=ids.get(listScroll+row);int ry=l.listY()+row*54;
            addDrawableChild(new HeraButton(l.listX()+9,ry,l.list()-18,49,id,false,()->select(id)){
                @Override public void onClick(Click click,boolean doubled){if(click.x()>getX()+width-39){toggle(id);rebuild();}else select(id);}
                @Override protected void renderWidget(DrawContext c,int mx,int my,float d){boolean on=enabled(id),chosen=selected.equals(id);setMessage(HeraTheme.text(id+" · "+(on?"An":"Aus")));
                    if(chosen||isHovered())HeraTheme.round(c,getX(),getY(),width,height,7,chosen?0xFF2C2A2C:0xFF202631);
                    if(chosen)HeraTheme.round(c,getX(),getY()+10,2,height-20,1,HeraTheme.ACCENT);
                    HeraTheme.icon(c,id,getX()+12,getY()+16,16,chosen?HeraTheme.ACCENT:HeraTheme.MUTED);
                    HeraTheme.label(c,HeraTheme.fit(id,width-83),getX()+39,getY()+12,HeraTheme.TEXT);HeraTheme.label(c,group(id),getX()+39,getY()+29,HeraTheme.MUTED);HeraTheme.toggle(c,getX()+width-34,getY()+18,on);
                }
            });
        }
        controls=new ModuleSettingsScreen(this,features,selected,hud,renderer).embedded().stream().filter(s->!s.label().get().startsWith("Modul:")).sorted(Comparator.comparingInt(s->s.label().get().contains("auswählen")?-1:0)).toList();
        int available=Math.max(1,(l.bottom()-(l.y()+133))/44);detailScroll=Math.clamp(detailScroll,0,Math.max(0,controls.size()-available));
        for(int row=0;row<available&&detailScroll+row<controls.size();row++){
            var setting=controls.get(detailScroll+row);int ry=l.y()+133+row*44;
            addDrawableChild(new HeraButton(l.detailX()+22,ry,l.detailWidth()-44,39,"",false,()->{setting.change().run();features.options.normalize();if(client.currentScreen==this)rebuild();}){
                @Override protected void renderWidget(DrawContext c,int mx,int my,float d){String full=setting.label().get();setMessage(HeraTheme.text(full));int split=full.indexOf(": ");String title=split<0?full.replace(" →",""):full.substring(0,split),value=split<0?"→":full.substring(split+2);
                    if(isHovered()||isFocused())HeraTheme.round(c,getX(),getY(),width,height,5,0xFF202630);
                    c.fill(getX()+2,getY()+height-1,getX()+width-2,getY()+height,0xFF252C37);
                    HeraTheme.label(c,HeraTheme.fit(title,width-90),getX()+8,getY()+15,HeraTheme.TEXT);
                    if(value.equals("An")||value.equals("Aus"))HeraTheme.toggle(c,getX()+width-35,getY()+13,value.equals("An"));else {int tw=textRenderer.getWidth(HeraTheme.text(value));HeraTheme.label(c,value,getX()+width-tw-9,getY()+15,HeraTheme.ACCENT);}
                }
            });
        }
        addDrawableChild(new HeraButton(l.detailX()+l.detailWidth()-91,l.y()+30,65,23,enabled(selected)?"Aktiv":"Inaktiv",enabled(selected),()->{toggle(selected);rebuild();}));
        addDrawableChild(new HeraButton(l.x()+l.width()-154,l.bottom()+12,136,25,"Speichern & schließen",true,this::close));
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.getMatrices().pushMatrix();c.getMatrices().scale(uiScale,uiScale);mx=Math.round(mx/uiScale);my=Math.round(my/uiScale);
        c.fill(0,0,canvasWidth,canvasHeight,0x55080B10);HeraTheme.panel(c,l.x(),l.y(),l.width(),l.height());
        HeraTheme.round(c,l.x()+1,l.y()+1,l.rail()-1,l.height()-2,10,0xFF11151C);
        c.fill(l.listX(),l.y()+1,l.detailX(),l.bottom(),0xFF171C25);c.fill(l.detailX(),l.y()+1,l.x()+l.width()-10,l.bottom(),0xFF131820);
        c.fill(l.listX(),l.y()+1,l.listX()+1,l.bottom(),HeraTheme.LINE);c.fill(l.detailX(),l.y()+1,l.detailX()+1,l.bottom(),HeraTheme.LINE);
        HeraTheme.round(c,l.x()+16,l.y()+22,23,23,7,HeraTheme.ACCENT);HeraTheme.title(c,"H",l.x()+22,l.y()+27,1.2f,0xFF211A16);HeraTheme.title(c,"hera",l.x()+46,l.y()+23,1.6f,HeraTheme.TEXT);HeraTheme.label(c,"DEIN SETUP",l.x()+17,l.y()+61,HeraTheme.MUTED);
        HeraTheme.title(c,category,l.listX()+18,l.y()+26,1.35f,HeraTheme.TEXT);HeraTheme.label(c,ids().size()+" Module",l.listX()+19,l.y()+49,HeraTheme.MUTED);
        HeraTheme.round(c,l.listX()+12,l.y()+75,l.list()-24,31,6,0xFF222A35);
        HeraTheme.label(c,group(selected).toUpperCase(Locale.ROOT),l.detailX()+24,l.y()+25,HeraTheme.MUTED);HeraTheme.title(c,HeraTheme.fit(selected,(int)((l.detailWidth()-45)/1.55f)),l.detailX()+24,l.y()+49,1.55f,HeraTheme.TEXT);
        HeraTheme.label(c,HeraTheme.fit(description(selected),l.detailWidth()-48),l.detailX()+24,l.y()+80,HeraTheme.MUTED);
        HeraTheme.label(c,"EINSTELLUNGEN",l.detailX()+26,l.y()+112,HeraTheme.MUTED);
        if(ids().isEmpty())HeraTheme.label(c,"Keine Treffer",l.listX()+22,l.listY()+20,HeraTheme.MUTED);
        super.render(c,mx,my,delta);
        int total=ModuleCatalog.IDS.size()+hud.modules.size();long active=ModuleCatalog.IDS.stream().filter(this::enabled).count()+hud.modules.stream().filter(m->m.enabled).count();
        c.fill(l.x()+12,l.bottom(),l.x()+l.width()-12,l.bottom()+1,HeraTheme.LINE);HeraTheme.label(c,error.isEmpty()?active+" von "+total+" aktiv · Minecraft 1.21.11":error,l.listX()+18,l.bottom()+21,error.isEmpty()?HeraTheme.MUTED:0xFFFF8099);
        if(ids().size()>l.visibleRows())HeraTheme.label(c,"↕",l.detailX()-12,l.bottom()-14,HeraTheme.MUTED);
        if(controls.size()>Math.max(1,(l.bottom()-(l.y()+133))/44))HeraTheme.label(c,"↕ Scrollen für mehr",l.detailX()+24,l.bottom()-12,HeraTheme.MUTED);
        c.getMatrices().popMatrix();
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){x/=uiScale;y/=uiScale;if(x>=l.detailX())detailScroll-=(int)Math.signum(vertical);else if(x>=l.listX())listScroll-=(int)Math.signum(vertical);rebuild();return true;}
    private Click unscale(Click click){return new Click(click.x()/uiScale,click.y()/uiScale,click.buttonInfo());}
    @Override public boolean mouseClicked(Click click,boolean doubled){return super.mouseClicked(unscale(click),doubled);}
    @Override public boolean mouseReleased(Click click){return super.mouseReleased(unscale(click));}
    @Override public boolean mouseDragged(Click click,double dx,double dy){return super.mouseDragged(unscale(click),dx/uiScale,dy/uiScale);}
    @Override public void mouseMoved(double x,double y){super.mouseMoved(x/uiScale,y/uiScale);}
    @Override public void close(){try{hud.save();features.save();super.close();}catch(IOException e){error="Speichern fehlgeschlagen. Bitte erneut versuchen.";}}
    @Override public boolean shouldPause(){return false;}
}
