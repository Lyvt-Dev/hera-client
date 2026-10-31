package gg.hera.ui;
import gg.hera.hud.*;
import gg.hera.features.FeatureConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.io.IOException;

public final class HudEditorScreen extends Screen {
    private final HudConfig config;private final HudRenderer renderer;private final FeatureConfig features;
    private final Deque<String> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private HudModule selected;private double offsetX,offsetY,startScale;private boolean dragging,resizing,grid=true,snap=true,resetArmed;
    private String error="",gesture="";private int guideX=-1,guideY=-1,paletteScroll;
    public HudEditorScreen(HudConfig config,HudRenderer renderer,FeatureConfig features){super(HeraTheme.text("Hera · HUD Studio"));this.config=config;this.renderer=renderer;this.features=features;this.selected=config.modules.getFirst();}
    @Override protected void init(){if(selected!=null){String id=selected.id;selected=config.modules.stream().filter(m->m.id.equals(id)).findFirst().orElse(null);}renderer.invalidate();rebuild();}
    private void remember(){undo.addLast(config.snapshot());while(undo.size()>40)undo.removeFirst();redo.clear();}
    private void mutate(Runnable action){remember();action.run();renderer.invalidate();rebuild();}
    private void history(boolean forward){var from=forward?redo:undo;var to=forward?undo:redo;if(from.isEmpty())return;String id=selected==null?null:selected.id;to.addLast(config.snapshot());config.restore(from.removeLast());selected=id==null?null:config.modules.stream().filter(m->m.id.equals(id)).findFirst().orElse(null);renderer.invalidate();rebuild();}
    private void rebuild(){clearChildren();int w=Math.min(460,width-16),x=(width-w)/2,button=(w-16)/7;
        String[] labels={grid?"Raster ✓":"Raster",snap?"Snap ✓":"Snap","Undo","Redo","Presets","Reset","Fertig"};
        Runnable[] actions={()->{grid=!grid;rebuild();},()->{snap=!snap;rebuild();},()->history(false),()->history(true),()->{remember();client.setScreen(new HudPresetScreen(this,config,renderer));},this::reset,this::close};
        addDrawableChild(new HeraButton(x+w-60,8,52,17,"Akzent",false,()->client.setScreen(new ColorPickerScreen(this,config.accent,v->config.accent=v))));
        for(int i=0;i<7;i++){var b=new HeraButton(x+8+i*button,29,button-3,22,labels[i],i==6,actions[i]);b.active=i!=2||!undo.isEmpty();if(i==3)b.active=!redo.isEmpty();addDrawableChild(b);}
        int paletteRows=Math.max(1,(height-154)/24);paletteScroll=Math.clamp(paletteScroll,0,Math.max(0,config.modules.size()-paletteRows));
        for(int i=paletteScroll;i<Math.min(config.modules.size(),paletteScroll+paletteRows);i++){var m=config.modules.get(i);int row=i-paletteScroll;addDrawableChild(new HeraButton(width-152,82+row*24,140,22,m.id,false,()->{selected=m;rebuild();}){
                @Override protected void renderWidget(DrawContext c,int mx,int my,float d){
                    if(m==selected||isHovered()||isFocused())HeraTheme.round(c,getX(),getY(),width,height,4,m==selected?HeraTheme.SELECTED:HeraTheme.RAISED);
                    HeraTheme.round(c,getX()+8,getY()+9,4,4,2,m.enabled?config.accent:HeraTheme.LINE);
                    HeraTheme.label(c,HeraTheme.fit(m.id,width-30),getX()+20,getY()+7,m==selected?HeraTheme.TEXT:HeraTheme.MUTED);
                }
            });}
        if(selected!=null){int iw=Math.min(440,width-16),ix=(width-iw)/2,iy=height-66;
            addDrawableChild(new HeraButton(ix+8,iy+29,48,22,selected.enabled?"An":"Aus",selected.enabled,()->mutate(()->selected.enabled=!selected.enabled)));
            addDrawableChild(new HeraButton(ix+60,iy+29,24,22,"−",false,()->resize(-.1f)));addDrawableChild(new HeraButton(ix+88,iy+29,24,22,"+",false,()->resize(.1f)));
            addDrawableChild(new HeraButton(ix+116,iy+29,58,22,"Anker",false,this::anchor));
            addDrawableChild(new HeraButton(ix+178,iy+29,57,22,"Position",false,()->mutate(()->{selected.x=.02f;selected.y=.12f;selected.anchorX=0;selected.anchorY=0;selected.scale=1;})));
            if(iw>=340)addDrawableChild(new HeraButton(ix+239,iy+29,iw-247,22,"Einstellungen",false,()->{remember();client.setScreen(new ModuleSettingsScreen(this,features,selected.id,config,renderer));}));
            else addDrawableChild(new HeraButton(ix+239,iy+29,iw-247,22,"…",false,()->client.setScreen(new ModuleSettingsScreen(this,features,selected.id,config,renderer))));
        }
    }
    private void anchor(){mutate(()->{int x=px(selected),y=py(selected);selected.anchorX=(selected.anchorX+1)%3;selected.anchorY=selected.anchorX;place(selected,x,y);});}
    private void reset(){if(!resetArmed){resetArmed=true;error="Reset erneut wählen: Positionen werden zurückgesetzt.";rebuild();return;}mutate(()->{for(int i=0;i<config.modules.size();i++){var m=config.modules.get(i);m.x=.025f+(i/8)*.35f;m.y=.15f+(i%8)*.07f;m.anchorX=0;m.anchorY=0;m.scale=1;}selected=null;});resetArmed=false;error="";}
    private void resize(float amount){if(selected!=null)mutate(()->{selected.scale=Math.clamp(selected.scale+amount,.5f,3);selected.normalize();});}
    private int pw(HudModule m){return renderer.bounds(m,width,height,client).width();}private int ph(HudModule m){return renderer.bounds(m,width,height,client).height();}
    private int px(HudModule m){return renderer.x(m,width,client);}private int py(HudModule m){return renderer.y(m,height);}
    private void place(HudModule m,double x,double y){m.x=HudGeometry.offset(x,m.anchorX,width,pw(m));m.y=HudGeometry.offset(y,m.anchorY,height,ph(m));m.normalize();}
    private boolean overlap(){if(selected==null)return false;int x=px(selected),y=py(selected),w=pw(selected),h=ph(selected);return y+h>height-46&&x<width/2+100&&x+w>width/2-100||y+h>height-130&&x<Math.min(300,width/2);}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0x440A0C10);if(grid){for(int x=0;x<width;x+=8)c.fill(x,0,x+1,height,0x06FFFFFF);for(int y=0;y<height;y+=8)c.fill(0,y,width,y+1,0x06FFFFFF);}
        renderer.render(c,true,selected==null?"":selected.id);if(selected!=null){int x=px(selected),y=py(selected),w=pw(selected),h=ph(selected),color=config.accent;
            c.fill(x-1,y-1,x+w+1,y,color);c.fill(x-1,y+h,x+w+1,y+h+1,color);c.fill(x-1,y,x,y+h,color);c.fill(x+w,y,x+w+1,y+h,color);HeraTheme.round(c,x+w-3,y+h-3,6,6,1,color);
            if(guideX>=0)c.fill(guideX,57,guideX+1,height-70,(config.accent&0xFFFFFF)|0x66000000);if(guideY>=0)c.fill(0,guideY,width,guideY+1,(config.accent&0xFFFFFF)|0x66000000);
        }
        HeraTheme.round(c,width-156,60,148,Math.max(1,(height-154)/24)*24+26,5,0xF0101216);HeraTheme.label(c,"HUD-ELEMENTE",width-144,69,HeraTheme.MUTED);
        int tw=Math.min(460,width-16),tx=(width-tw)/2;HeraTheme.round(c,tx,6,tw,50,5,0xF0101216);HeraTheme.logo(c,tx+9,11,13);HeraTheme.label(c,"HUD Studio",tx+28,13,HeraTheme.TEXT);
        int iw=Math.min(440,width-16),ix=(width-iw)/2,iy=height-66;HeraTheme.round(c,ix,iy,iw,59,5,0xF0101216);
        if(selected==null){HeraTheme.label(c,"Element wählen · ziehen · Ecke zum Skalieren",ix+10,iy+12,HeraTheme.TEXT);HeraTheme.label(c,"Tab: Auswahl · Pfeile: Position · Strg+Z / Y",ix+10,iy+32,HeraTheme.MUTED);}
        else{String label=selected.id+" · "+Math.round(selected.scale*100)+"%"+(renderer.bounds(selected,width,height,client).scale()<selected.scale?" (angepasst)":"")+" · "+List.of("Oben links","Unten rechts","Zentriert").get(selected.anchorX);HeraTheme.label(c,HeraTheme.fit(label,iw-20),ix+10,iy+10,HeraTheme.TEXT);}
        if(!error.isEmpty()||overlap())HeraTheme.label(c,HeraTheme.fit(error.isEmpty()?"Mögliche Überlappung mit Chat / Hotbar":error,width-24),12,60,0xFFF1B66B);
        super.render(c,mx,my,delta);
    }
    @Override public boolean mouseClicked(Click click,boolean doubled){if(super.mouseClicked(click,doubled))return true;if(click.y()<57||click.y()>height-70||click.x()>=width-156&&click.y()>=60)return true;
        for(int i=config.modules.size()-1;i>=0;i--){var m=config.modules.get(i);if(!m.enabled&&m!=selected)continue;int x=px(m),y=py(m),w=pw(m),h=ph(m);if(click.x()>=x-2&&click.x()<=x+w+4&&click.y()>=y-2&&click.y()<=y+h+4){selected=m;gesture=config.snapshot();offsetX=click.x()-x;offsetY=click.y()-y;startScale=m.scale;resizing=click.button()==0&&click.x()>=x+w-6&&click.y()>=y+h-6;dragging=click.button()==0&&!resizing;if(click.button()==1)mutate(()->selected.enabled=!selected.enabled);resetArmed=false;error="";rebuild();return true;}}
        selected=null;dragging=false;resizing=false;rebuild();return false;
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy){if(selected==null||!dragging&&!resizing)return super.mouseDragged(click,dx,dy);if(resizing){selected.scale=(float)Math.clamp(startScale*(click.x()-px(selected))/Math.max(1,offsetX),.5,3);return true;}
        int x=(int)Math.round(click.x()-offsetX),y=(int)Math.round(click.y()-offsetY);guideX=-1;guideY=-1;
        if(snap||click.hasShift()){if(click.hasShift()){x=Math.round(x/8f)*8;y=Math.round(y/8f)*8;}else{int sx=HudGeometry.snap(x,pw(selected),width,width/2,0),sy=HudGeometry.snap(y,ph(selected),height,height/2,0);if(sx!=x)guideX=sx;if(sy!=y)guideY=sy;x=sx;y=sy;for(var m:config.modules)if(m!=selected&&m.enabled){sx=HudGeometry.snap(x,pw(selected),width,px(m),pw(m));sy=HudGeometry.snap(y,ph(selected),height,py(m),ph(m));if(sx!=x)guideX=sx;if(sy!=y)guideY=sy;x=sx;y=sy;}}}
        place(selected,x,y);return true;
    }
    @Override public boolean mouseReleased(Click click){if((dragging||resizing)&&!gesture.equals(config.snapshot())){undo.addLast(gesture);while(undo.size()>40)undo.removeFirst();redo.clear();}dragging=false;resizing=false;guideX=-1;guideY=-1;rebuild();return super.mouseReleased(click);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(x>=width-156&&y>=60&&y<height-70){paletteScroll+=(int)-Math.signum(vertical);rebuild();return true;}if(selected==null)return false;resize((float)vertical*.1f);return true;}
    @Override public boolean keyPressed(KeyInput input){if(input.hasCtrl()&&input.key()==GLFW.GLFW_KEY_Z){history(input.hasShift());return true;}if(input.hasCtrl()&&input.key()==GLFW.GLFW_KEY_Y){history(true);return true;}
        if(input.key()==GLFW.GLFW_KEY_TAB){int i=selected==null?-1:config.modules.indexOf(selected);selected=config.modules.get(Math.floorMod(i+(input.hasShift()?-1:1),config.modules.size()));rebuild();return true;}
        if(selected!=null){int step=input.hasShift()?8:1;int x=px(selected),y=py(selected);switch(input.key()){case GLFW.GLFW_KEY_LEFT->x-=step;case GLFW.GLFW_KEY_RIGHT->x+=step;case GLFW.GLFW_KEY_UP->y-=step;case GLFW.GLFW_KEY_DOWN->y+=step;case GLFW.GLFW_KEY_SPACE->{mutate(()->selected.enabled=!selected.enabled);return true;}case GLFW.GLFW_KEY_ENTER->{client.setScreen(new ModuleSettingsScreen(this,features,selected.id,config,renderer));return true;}default->{return super.keyPressed(input);}}remember();place(selected,x,y);return true;}
        return super.keyPressed(input);
    }
    @Override public void close(){try{config.save();features.save();client.setScreen(new ModuleScreen(config,renderer,features));}catch(IOException e){error="Layout konnte nicht gespeichert werden.";}}
    @Override public boolean shouldPause(){return false;}
}
