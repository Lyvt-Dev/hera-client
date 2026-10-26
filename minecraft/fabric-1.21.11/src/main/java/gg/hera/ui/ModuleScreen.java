package gg.hera.ui;

import gg.hera.hud.*;
import gg.hera.features.FeatureConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.io.IOException;
import java.util.*;
import java.util.function.BooleanSupplier;

public final class ModuleScreen extends Screen {
    private final HudConfig config;
    private final HudRenderer renderer;
    private final FeatureConfig features;
    private String search="",category="Alle",error="";
    private int page;
    private TextFieldWidget field;
    private ModuleLayout layout;
    public ModuleScreen(HudConfig config,HudRenderer renderer,FeatureConfig features) {
        super(HeraTheme.text("Hera · Module"));this.config=config;this.renderer=renderer;this.features=features;
    }
    private List<String> filtered() {
        List<String> ids=new ArrayList<>();
        if(!category.equals("HUD"))ids.add("Kristall-Totem");
        if(!category.equals("Spiel"))config.modules.forEach(m->ids.add(m.id));
        return ids.stream().filter(id->id.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))).toList();
    }
    @Override protected void init() {
        layout=ModuleLayout.of(width,height);
        int sy=layout.gridY()-25;
        field=new TextFieldWidget(textRenderer,layout.contentX()+10,sy+5,layout.contentWidth()-20,16,HeraTheme.text("Module suchen"));
        field.addFormatter((value,offset)->HeraTheme.text(value).asOrderedText());
        field.setMaxLength(60);field.setDrawsBackground(false);field.setText(search);field.setPlaceholder(HeraTheme.text("Module suchen …"));
        field.setChangedListener(value->{search=value;page=0;rebuild();});rebuild();
    }
    private void rebuild() {
        clearChildren();addDrawableChild(field);
        int bx=layout.x()+8,by=layout.y()+68;
        for(String tab:List.of("Alle","HUD","Spiel")) {
            addDrawableChild(new HeraButton(bx,by,layout.side()-16,25,tab,category.equals(tab),()->{category=tab;page=0;rebuild();}));by+=32;
        }
        addDrawableChild(new HeraButton(bx,layout.y()+layout.height()-35,layout.side()-16,25,"HUD-Editor",false,()->client.setScreen(new HudEditorScreen(config,renderer,features))));
        var ids=filtered();page=Math.clamp(page,0,layout.maxPage(ids.size()));
        for(int i=page*layout.pageSize();i<Math.min(ids.size(),(page+1)*layout.pageSize());i++) {
            String id=ids.get(i);HudModule module=config.modules.stream().filter(m->m.id.equals(id)).findFirst().orElse(null);
            int local=i-page*layout.pageSize();
            int x=layout.contentX()+(local%layout.columns())*(layout.cardWidth()+8),y=layout.gridY()+(local/layout.columns())*(layout.cardHeight()+8);
            addDrawableChild(new ModuleCard(x,y,layout.cardWidth(),layout.cardHeight(),id,module==null?"8 Blöcke · nur Hotbar":"Dein "+id.toLowerCase(Locale.ROOT)+"-HUD",()->module==null?features.crystalTotem:module.enabled,()->{if(module==null)features.crystalTotem=!features.crystalTotem;else module.enabled=!module.enabled;}));
        }
        int fy=layout.y()+layout.height()-31;
        var previous=new HeraButton(layout.contentX(),fy,24,22,"‹",false,()->{page--;rebuild();});previous.active=page>0;addDrawableChild(previous);
        var next=new HeraButton(layout.contentX()+29,fy,24,22,"›",false,()->{page++;rebuild();});next.active=page<layout.maxPage(ids.size());addDrawableChild(next);
        addDrawableChild(new HeraButton(layout.contentX()+layout.contentWidth()-74,fy,74,22,"Fertig",true,this::close));
    }
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        applyBlur(c);c.fill(0,0,width,height,0x55080B11);HeraTheme.panel(c,layout.x(),layout.y(),layout.width(),layout.height());
        HeraTheme.round(c,layout.x(),layout.y(),layout.side(),layout.height(),11,0x78191C23);
        c.fill(layout.x()+layout.side(),layout.y()+12,layout.x()+layout.side()+1,layout.y()+layout.height()-12,HeraTheme.LINE);
        HeraTheme.title(c,"Hera.",layout.x()+15,layout.y()+18,1.8f,HeraTheme.ACCENT);
        HeraTheme.label(c,"DEIN CLIENT",layout.x()+15,layout.y()+43,HeraTheme.MUTED);
        HeraTheme.title(c,"Dein Setup.",layout.contentX(),layout.y()+17,1.6f,HeraTheme.TEXT);
        if(layout.height()>=300)HeraTheme.label(c,"Kleine Details. Ganz dein Spiel.",layout.contentX(),layout.y()+40,HeraTheme.MUTED);
        int sy=layout.gridY()-25;
        HeraTheme.round(c,layout.contentX(),sy,layout.contentWidth(),21,6,HeraTheme.RAISED);
        super.render(c,mx,my,delta);
        var ids=filtered();
        if(ids.isEmpty())HeraTheme.label(c,"Keine Module gefunden.",layout.contentX()+10,layout.gridY()+25,HeraTheme.MUTED);
        if(layout.contentWidth()>240)HeraTheme.label(c,(page+1)+" / "+(layout.maxPage(ids.size())+1),layout.contentX()+64,layout.y()+layout.height()-24,HeraTheme.MUTED);
        if(!error.isEmpty())HeraTheme.label(c,HeraTheme.fit(error,layout.contentWidth()),layout.contentX(),layout.y()+layout.height()-44,0xFFFF9DAB);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(x<layout.contentX()||y<layout.gridY())return super.mouseScrolled(x,y,horizontal,vertical);
        page=Math.clamp(page-(int)Math.signum(vertical),0,layout.maxPage(filtered().size()));rebuild();return true;
    }
    @Override public void close(){try{config.save();features.save();super.close();}catch(IOException e){error="Speichern fehlgeschlagen. Bitte erneut versuchen.";}}
    @Override public boolean shouldPause(){return false;}
    private static final class ModuleCard extends HeraButton {
        private final String name,description;private final BooleanSupplier enabled;
        ModuleCard(int x,int y,int w,int h,String name,String description,BooleanSupplier enabled,Runnable toggle){super(x,y,w,h,name,false,toggle);this.name=name;this.description=description;this.enabled=enabled;}
        @Override protected void renderWidget(DrawContext c,int mx,int my,float delta){
            boolean on=enabled.getAsBoolean(),hover=isHovered()||isFocused();setMessage(HeraTheme.text(name+" · "+(on?"An":"Aus")));
            HeraTheme.round(c,getX(),getY(),width,height,8,hover?0x66FFFFFF:on?0x99C38C65:0x30FFFFFF);
            HeraTheme.round(c,getX()+1,getY()+1,width-2,height-2,7,hover?0xBB394353:HeraTheme.PANEL);
            HeraTheme.round(c,getX()+10,getY()+9,19,17,5,on?0xFF3C2E23:HeraTheme.RAISED);
            HeraTheme.label(c,switch(name){case "Kristall-Totem"->"T";case "FPS"->"F";case "Koordinaten"->"+";case "Ping"->"P";case "Speicher"->"R";case "Uhr"->"O";case "Richtung"->"N";case "Geschwindigkeit"->">";case "Biome"->"B";default->"S";},getX()+16,getY()+13,on?HeraTheme.ACCENT:HeraTheme.MUTED);
            HeraTheme.toggle(c,getX()+width-35,getY()+11,on);
            HeraTheme.label(c,HeraTheme.fit(name,width-20),getX()+10,getY()+34,HeraTheme.TEXT);
            if(height>=65)HeraTheme.label(c,HeraTheme.fit(description,width-20),getX()+10,getY()+51,HeraTheme.MUTED);
        }
    }
}
