package gg.hera.ui;

import gg.hera.hud.*;
import gg.hera.features.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.tooltip.Tooltip;
import java.io.IOException;
import java.util.*;

public final class ModuleScreen extends Screen {
    private final HudConfig hud;private final HudRenderer renderer;private final FeatureConfig features;
    private String search="",category="Alle Module",selected="Item-ESP",error="";
    private int listScroll,detailScroll,canvasWidth,canvasHeight;private float uiScale;
    private TextFieldWidget field;private WorkspaceLayout l;
    private List<ModuleSettingsScreen.Setting> controls=List.of();
    private List<String> visibleIds=List.of();private int activeCount,totalCount;
    public ModuleScreen(HudConfig hud,HudRenderer renderer,FeatureConfig features){super(HeraTheme.text("Hera · Dein Setup"));this.hud=hud;this.renderer=renderer;this.features=features;}
    private HudModule hud(String id){return hud.modules.stream().filter(m->m.id.equals(id)).findFirst().orElse(null);}
    private boolean enabled(String id){var m=hud(id);return m==null?ModuleCatalog.enabled(features,id):m.enabled;}
    private void toggle(String id){var m=hud(id);if(m==null)ModuleCatalog.toggle(features,id);else m.enabled=!m.enabled;}
    private String group(String id){if(hud(id)!=null)return "HUD";return switch(id){case "D-Tap","Kristall-Totem","Auto-Totem","Zoom","Free Look","Freecam","Toggle Sprint"->"Gameplay";default->"Darstellung";};}
    private String description(String id){String extra=switch(id){case "Hera Companion"->"Dein 3D-Modell · Folgen, Schulter & 8 Animationen";case "Weavy Capes"->"Segmentierte Cape-Bewegung · lokal dargestellt";case "Chat Heads"->"Skin-Köpfe bei Nachrichten bekannter Spieler";case "Clear Lava"->"Lavanebel und Feuer unter Lava ausblenden";case "Arrow Trail"->"Lichtspur für fliegende geladene Pfeile";case "Item Model"->"Größe und Position deiner Hand-Items";case "Potion Status"->"Aktive Trankeffekte mit verbleibender Zeit";case "Uptime"->"Dauer deiner aktuellen Verbindung";case "TPS"->"Schätzung aus empfangenen Weltzeit-Paketen";case "Combo Counter"->"Bestätigte Spielertreffer · Reset nach 3 Sekunden";case "Item Counter"->"Zähle selbst ausgewählte Inventar-Items";case "Item Changes"->"Erhaltene und verlorene Inventar-Items";case "Day Counter"->"Minecraft-Tage deiner aktuellen Welt";case "Resource Pack Display"->"Aktive Ressourcenpakete";case "Saturation HUD"->"Serverübermittelte Hunger- und Sättigungswerte";case "Paperdoll"->"Deine Spielfigur als bewegliches HUD";case "Helditem"->"Item-Name und Verzauberungen";case "Scoreboard"->"Server-Scoreboard frei positionieren";case "Action Bar"->"Server-Actionbar positionieren und skalieren";case "Titles"->"Server-Titel positionieren und skalieren";case "Block Outlines"->"Farbe und Stärke der anvisierten Blockumrandung";case "Hit Color"->"Farbe verletzter geladener Entities";case "Jump Reset"->"Visuelle Timing-Hilfe · springt nicht automatisch";case "Toggle Sprint"->"Sprintmodus umschalten · Taste in Minecraft belegen";case "Old Animations"->"Klassische Swing-Bewegung für Hand-Items";case "No Hurt Cam"->"Schadensbedingtes Kamerawackeln ausblenden";case "Low Fire"->"Feuer-Overlay absenken";case "Shield Status"->"Schildstatus mit zustandsabhängiger HUD-Farbe";case "No Advancement"->"Fortschritts-Toasts ausblenden";case "Particle Settings"->"Ausgewählte Partikel lokal deaktivieren";case "Item Physics"->"Liegende und rotierende Item-Drops";case "Clear GUI"->"Abdunklung hinter Ingame-Menüs entfernen";case "Way Points"->"Eigene Wegpunkte je Server und Dimension";case "Shulker Preview"->"Übermittelte Shulker-Inhalte im Tooltip ansehen";case "Hitboxes"->"Geladene Entities · echtes Server-Vanish ist nicht sichtbar";default->"";};if(!extra.isEmpty())return extra;String d=ModuleCatalog.description(id);if(!d.isEmpty())return d;return switch(id){case "Item-ESP"->"Markiere ausgewählte Items und Blöcke.";case "Spieler-ESP"->"Behalte geladene Spieler im Blick.";case "Mob-ESP"->"Markiere Mobs mit deinem eigenen Stil.";case "Fast Items"->"Passe die Darstellung von Item-Drops an.";case "Auto-Totem"->"Fülle deine Nebenhand mit einem Totem nach.";case "Kristall-Totem"->"Wähle ein Hotbar-Totem bei Kristall-Gefahr.";case "Keystrokes"->"Deine Tasten und Mausklicks im Spiel.";case "Armor HUD"->"Rüstung und Haltbarkeit auf einen Blick.";default->"Position, Größe und Farben für dein HUD.";};}
    private List<String> ids(){var all=new ArrayList<>(ModuleCatalog.IDS);hud.modules.forEach(m->all.add(m.id));String q=search.toLowerCase(Locale.ROOT);return all.stream().filter(id->category.equals("Alle Module")||category.equals("Aktiv")&&enabled(id)||category.equals(group(id))).filter(id->id.toLowerCase(Locale.ROOT).contains(q)||description(id).toLowerCase(Locale.ROOT).contains(q)).toList();}
    private void revealSelection(){int i=ids().indexOf(selected);if(i>=0){if(i<listScroll)listScroll=i;else if(i>=listScroll+l.visibleRows())listScroll=i-l.visibleRows()+1;}}
    public void select(String id){selected=id;detailScroll=0;revealSelection();rebuild();}
    @Override protected void init(){
        float physical=client.getWindow().getWidth()>=1600?1.65f:1.25f;
        uiScale=Math.min(physical/(float)client.getWindow().getScaleFactor(),Math.min(width/890f,height/550f));
        canvasWidth=Math.round(width/uiScale);canvasHeight=Math.round(height/uiScale);l=WorkspaceLayout.of(canvasWidth,canvasHeight);revealSelection();
        field=new TextFieldWidget(textRenderer,l.listX()+19,l.y()+83,l.list()-38,18,HeraTheme.text("Module suchen"));
        field.setDrawsBackground(false);field.setMaxLength(80);field.addFormatter((v,o)->HeraTheme.text(v).asOrderedText());field.setText(search);field.setPlaceholder(HeraTheme.text("Suchen …"));
        field.setChangedListener(v->{search=v;listScroll=0;rebuild();});rebuild();
    }
    private void rebuild(){
        if(l==null)return;
        int focus=children().indexOf(getFocused());boolean searchFocused=field.isFocused();
        visibleIds=ids();totalCount=ModuleCatalog.IDS.size()+hud.modules.size();activeCount=(int)(ModuleCatalog.IDS.stream().filter(this::enabled).count()+hud.modules.stream().filter(m->m.enabled).count());
        if(!visibleIds.contains(selected)&&!visibleIds.isEmpty()){selected=visibleIds.getFirst();detailScroll=0;}
        clearChildren();addDrawableChild(field);if(searchFocused)setFocused(field);
        int nav=l.y()+96;for(String name:List.of("Alle Module","Aktiv","HUD","Gameplay","Darstellung")){
            addDrawableChild(new HeraButton(l.x()+12,nav,l.rail()-24,30,name,false,()->{category=name;listScroll=0;detailScroll=0;if(!ids().isEmpty()&&!ids().contains(selected))selected=ids().getFirst();rebuild();}){
                @Override protected void renderWidget(DrawContext c,int mx,int my,float d){boolean current=category.equals(name);if(current||isHovered()||isFocused())HeraTheme.round(c,getX(),getY(),width,height,6,current?HeraTheme.SELECTED:HeraTheme.RAISED);HeraTheme.icon(c,name,getX()+10,getY()+9,12,current?HeraTheme.ACCENT:HeraTheme.MUTED);HeraTheme.label(c,name,getX()+29,getY()+11,current?HeraTheme.TEXT:HeraTheme.MUTED);if(current)HeraTheme.round(c,getX(),getY()+8,2,14,1,HeraTheme.ACCENT);}
            });nav+=38;
        }
        addDrawableChild(new HeraButton(l.x()+12,l.bottom()-37,l.rail()-24,27,"HUD anordnen",false,()->client.setScreen(new HudEditorScreen(hud,renderer,features))));
        var ids=visibleIds;listScroll=Math.clamp(listScroll,0,Math.max(0,ids.size()-l.visibleRows()));
        for(int row=0;row<l.visibleRows()&&listScroll+row<ids.size();row++){
            String id=ids.get(listScroll+row);int ry=l.listY()+row*60;
            addDrawableChild(new HeraButton(l.listX()+9,ry,l.list()-18,55,id,false,()->select(id)){
                @Override public boolean keyPressed(KeyInput input){if(isFocused()&&input.key()==GLFW.GLFW_KEY_SPACE){toggle(id);rebuild();return true;}return super.keyPressed(input);}
                @Override public void onClick(Click click,boolean doubled){if(click.x()>getX()+width-39){toggle(id);rebuild();}else select(id);}
                @Override protected void renderWidget(DrawContext c,int mx,int my,float d){boolean on=enabled(id),chosen=selected.equals(id);
                    if(chosen||isHovered()||isFocused())HeraTheme.round(c,getX(),getY(),width,height,7,chosen?HeraTheme.SELECTED:HeraTheme.RAISED);
                    if(chosen)HeraTheme.round(c,getX(),getY()+10,2,height-20,1,HeraTheme.ACCENT);
                    HeraTheme.icon(c,id,getX()+12,getY()+11,16,chosen?HeraTheme.ACCENT:HeraTheme.MUTED);
                    HeraTheme.label(c,HeraTheme.fit(id,width-83),getX()+39,getY()+12,HeraTheme.TEXT);HeraTheme.label(c,HeraTheme.fit(description(id),width-56),getX()+12,getY()+36,HeraTheme.MUTED);HeraTheme.toggle(c,getX()+width-34,getY()+12,on);
                }
            }).setTooltip(Tooltip.of(HeraTheme.text(description(id)+" · Leertaste: An / Aus")));
        }
        controls=visibleIds.isEmpty()?List.of():new ModuleSettingsScreen(this,features,selected,hud,renderer).embedded().stream().filter(s->!s.label().get().startsWith("Modul:")).sorted(Comparator.comparingInt(s->s.label().get().contains("auswählen")?-1:0)).toList();
        int available=Math.max(1,(l.bottom()-14-(l.y()+133))/48);detailScroll=Math.clamp(detailScroll,0,Math.max(0,controls.size()-available));
        for(int row=0;row<available&&detailScroll+row<controls.size();row++){
            var setting=controls.get(detailScroll+row);int ry=l.y()+133+row*48;
            if(setting.numeric()!=null){var n=setting.numeric();addDrawableChild(new HeraSlider(l.detailX()+22,ry,l.detailWidth()-44,43,setting.label(),n.value().getAsDouble(),n.min(),n.max(),n.step(),v->{n.change().accept(v);renderer.invalidate();}));continue;}
            addDrawableChild(new HeraButton(l.detailX()+22,ry,l.detailWidth()-44,43,setting.label().get(),false,()->{setting.change().run();features.options.normalize();renderer.invalidate();if(client.currentScreen==this)rebuild();}){
                @Override protected void renderWidget(DrawContext c,int mx,int my,float d){String full=setting.label().get();int split=full.indexOf(": ");String title=split<0?full.replace(" →",""):full.substring(0,split),value=split<0?"→":full.substring(split+2);
                    if(isHovered()||isFocused())HeraTheme.round(c,getX(),getY(),width,height,5,HeraTheme.RAISED);
                    HeraTheme.label(c,HeraTheme.fit(title,width-51),getX()+8,getY()+(value.equals("An")||value.equals("Aus")?15:5),HeraTheme.TEXT);
                    if(value.equals("An")||value.equals("Aus"))HeraTheme.toggle(c,getX()+width-35,getY()+13,value.equals("An"));else {HeraTheme.label(c,HeraTheme.fit(value,width-16),getX()+8,getY()+23,HeraTheme.ACCENT);}
                }
            });
        }
        if(!visibleIds.isEmpty())addDrawableChild(new HeraButton(l.detailX()+l.detailWidth()-91,l.y()+30,65,23,enabled(selected)?"Aktiv":"Inaktiv",enabled(selected),()->{toggle(selected);rebuild();}));
        if(visibleIds.isEmpty())controls=List.of();
        addDrawableChild(new HeraButton(l.x()+l.width()-154,l.bottom()+12,136,25,"Speichern & schließen",true,this::close));
        if(!searchFocused&&focus>=0&&focus<children().size())setFocused(children().get(focus));
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.getMatrices().pushMatrix();c.getMatrices().scale(uiScale,uiScale);mx=Math.round(mx/uiScale);my=Math.round(my/uiScale);
        c.fill(0,0,canvasWidth,canvasHeight,0x55080B10);HeraTheme.panel(c,l.x(),l.y(),l.width(),l.height());
        HeraTheme.round(c,l.x()+1,l.y()+1,l.rail()-1,l.height()-2,10,HeraTheme.BG);
        c.fill(l.listX(),l.y()+1,l.detailX(),l.bottom(),HeraTheme.PANEL);c.fill(l.detailX(),l.y()+1,l.x()+l.width()-10,l.bottom(),HeraTheme.BG);
        c.fill(l.listX(),l.y()+1,l.listX()+1,l.bottom(),HeraTheme.LINE);c.fill(l.detailX(),l.y()+1,l.detailX()+1,l.bottom(),HeraTheme.LINE);
        HeraTheme.logo(c,l.x()+15,l.y()+17,29);HeraTheme.title(c,"hera",l.x()+46,l.y()+23,1.6f,HeraTheme.TEXT);HeraTheme.label(c,"DEIN SETUP",l.x()+17,l.y()+61,HeraTheme.MUTED);
        HeraTheme.title(c,category,l.listX()+18,l.y()+26,1.35f,HeraTheme.TEXT);HeraTheme.label(c,visibleIds.size()+" Module",l.listX()+19,l.y()+49,HeraTheme.MUTED);
        HeraTheme.round(c,l.listX()+12,l.y()+75,l.list()-24,31,5,field.isFocused()?HeraTheme.LINE:HeraTheme.RAISED);
        if(!visibleIds.isEmpty()){HeraTheme.label(c,group(selected).toUpperCase(Locale.ROOT),l.detailX()+24,l.y()+25,HeraTheme.MUTED);HeraTheme.title(c,HeraTheme.fit(selected,(int)((l.detailWidth()-45)/1.55f)),l.detailX()+24,l.y()+49,1.55f,HeraTheme.TEXT);
        HeraTheme.label(c,HeraTheme.fit(description(selected),l.detailWidth()-48),l.detailX()+24,l.y()+80,HeraTheme.MUTED);
        HeraTheme.label(c,"EINSTELLUNGEN",l.detailX()+26,l.y()+112,HeraTheme.MUTED);}
        if(visibleIds.isEmpty()){HeraTheme.label(c,"Keine passenden Module",l.listX()+22,l.listY()+20,HeraTheme.TEXT);HeraTheme.label(c,"Suche oder Kategorie ändern.",l.listX()+22,l.listY()+40,HeraTheme.MUTED);}
        super.render(c,mx,my,delta);
        c.fill(l.x()+12,l.bottom(),l.x()+l.width()-12,l.bottom()+1,HeraTheme.LINE);HeraTheme.label(c,error.isEmpty()?activeCount+" von "+totalCount+" aktiv · Esc zum Speichern":error,l.listX()+18,l.bottom()+21,error.isEmpty()?HeraTheme.MUTED:0xFFFF8099);
        scrollbar(c,l.detailX()-5,l.listY(),l.bottom()-l.listY(),visibleIds.size(),l.visibleRows(),listScroll);
        int rows=Math.max(1,(l.bottom()-14-(l.y()+133))/48);
        scrollbar(c,l.detailX()+l.detailWidth()-7,l.y()+133,l.bottom()-14-(l.y()+133),controls.size(),rows,detailScroll);
        c.getMatrices().popMatrix();
    }
    private void scrollbar(DrawContext c,int x,int y,int h,int count,int rows,int offset){
        if(count<=rows)return;int thumb=Math.max(16,h*rows/count),top=y+(h-thumb)*offset/(count-rows);
        HeraTheme.round(c,x,top,3,thumb,1,0xFF67707D);
    }
    @Override public boolean keyPressed(KeyInput input){
        if(input.hasCtrl()&&input.key()==GLFW.GLFW_KEY_F){setFocused(field);field.setFocused(true);return true;}
        if(input.key()==GLFW.GLFW_KEY_ESCAPE&&field.isFocused()&&!search.isEmpty()){field.setText("");return true;}
        return super.keyPressed(input);
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
