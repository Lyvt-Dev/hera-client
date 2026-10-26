package gg.hera.ui;

import gg.hera.hud.*;
import gg.hera.features.FeatureConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import java.io.IOException;
import java.util.Locale;

public final class HudEditorScreen extends Screen {
    private static final int[] COLORS={0xFFF0A46E,0xFFF3F4F8,0xFF9AE8E2,0xFFA7DBA0,0xFFF4A5C4};
    private final HudConfig config;private final HudRenderer renderer;private final FeatureConfig features;
    private HudModule selected;private double offsetX,offsetY;private boolean dragging,grid=true,resetArmed;
    private String error="";
    public HudEditorScreen(HudConfig config,HudRenderer renderer,FeatureConfig features){super(HeraTheme.text("Hera · HUD Studio"));this.config=config;this.renderer=renderer;this.features=features;}
    @Override protected void init(){rebuild();}
    private void rebuild(){
        clearChildren();int tw=Math.min(390,width-20),tx=(width-tw)/2;
        addDrawableChild(new HeraButton(tx+tw-65,19,55,22,"Fertig",true,this::close));
        addDrawableChild(new HeraButton(tx+tw-119,19,48,22,grid?"Raster an":"Raster aus",false,()->{grid=!grid;rebuild();}));
        addDrawableChild(new HeraButton(tx+tw-179,19,54,22,resetArmed?"Sicher?":"Reset",false,this::reset));
        if(selected!=null){
            int iw=Math.min(370,width-20),ix=(width-iw)/2,iy=height-76;
            addDrawableChild(new HeraButton(ix+iw-67,iy+8,57,20,selected.enabled?"Aktiv":"Inaktiv",selected.enabled,()->{selected.enabled=!selected.enabled;rebuild();}));
            addDrawableChild(new HeraButton(ix+10,iy+35,24,21,"−",false,()->resize(-.1f)));
            addDrawableChild(new HeraButton(ix+79,iy+35,24,21,"+",false,()->resize(.1f)));
            addDrawableChild(new HeraButton(ix+111,iy+35,58,21,"Farbe",false,this::color));
            addDrawableChild(new HeraButton(ix+177,iy+35,Math.min(92,iw-187),21,selected.background==0?"Fläche aus":"Fläche an",false,()->{selected.background=selected.background==0?0xDB1B1E26:0;rebuild();}));
        }
    }
    private void reset(){if(!resetArmed){resetArmed=true;rebuild();return;}for(int i=0;i<config.modules.size();i++){HudModule m=config.modules.get(i);int rows=Math.max(1,(height-145)/30);m.x=(12+(i/rows)*Math.min(160,width/2))/(float)width;m.y=(62+(i%rows)*30)/(float)height;m.scale=1;}selected=null;resetArmed=false;rebuild();}
    private void resize(float amount){if(selected!=null){selected.scale=Math.clamp(selected.scale+amount,.5f,3);rebuild();}}
    private void color(){if(selected==null)return;int next=0;for(int i=0;i<COLORS.length;i++)if(selected.color==COLORS[i])next=(i+1)%COLORS.length;selected.color=COLORS[next];}
    private int px(HudModule m){return Math.min(Math.round(m.x*width),Math.max(0,width-Math.round(renderer.width(m,client)*m.scale)));}
    private int py(HudModule m){return Math.min(Math.round(m.y*height),Math.max(0,height-Math.round(24*m.scale)));}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0x550A0D13);
        if(grid){for(int x=0;x<width;x+=8)c.fill(x,0,x+1,height,0x08FFFFFF);for(int y=0;y<height;y+=8)c.fill(0,y,width,y+1,0x08FFFFFF);}
        renderer.render(c,true);
        if(selected!=null){int x=px(selected),y=py(selected),w=Math.round(renderer.width(selected,client)*selected.scale),h=Math.round(24*selected.scale);
            c.fill(x-2,y-2,x+w+2,y-1,HeraTheme.ACCENT);c.fill(x-2,y+h+1,x+w+2,y+h+2,HeraTheme.ACCENT);c.fill(x-2,y-2,x-1,y+h+2,HeraTheme.ACCENT);c.fill(x+w+1,y-2,x+w+2,y+h+2,HeraTheme.ACCENT);
            if(dragging){c.fill(width/2,48,width/2+1,height-85,0x44F0A46E);c.fill(0,height/2,width,height/2+1,0x44F0A46E);}
        }
        int tw=Math.min(390,width-20),tx=(width-tw)/2;
        HeraTheme.panel(c,tx,10,tw,40);HeraTheme.title(c,"HUD Studio",tx+12,21,1.2f,HeraTheme.TEXT);
        int iw=Math.min(370,width-20),ix=(width-iw)/2,iy=height-76;
        HeraTheme.panel(c,ix,iy,iw,65);
        if(selected==null){HeraTheme.title(c,"Alles an seinem Platz.",ix+13,iy+12,1.15f,HeraTheme.TEXT);HeraTheme.label(c,"Ein HUD anklicken und ziehen.",ix+13,iy+32,HeraTheme.MUTED);HeraTheme.label(c,"Shift: Raster · Mausrad: Größe · Esc: fertig",ix+13,iy+47,HeraTheme.MUTED);}
        else{HeraTheme.title(c,selected.id,ix+12,iy+12,1.1f,HeraTheme.TEXT);HeraTheme.label(c,String.format(Locale.ROOT,"%.1f×",selected.scale),ix+43,iy+42,HeraTheme.ACCENT);HeraTheme.round(c,ix+153,iy+42,7,7,3,selected.color);}
        super.render(c,mx,my,delta);
        if(!error.isEmpty())HeraTheme.label(c,error,12,56,0xFFFF9DAB);
    }
    @Override public boolean mouseClicked(Click click,boolean doubled){
        if(super.mouseClicked(click,doubled))return true;
        double x=click.x(),y=click.y();int tw=Math.min(390,width-20),iw=Math.min(370,width-20);
        if((y>=10&&y<=50&&x>=(width-tw)/2&&x<=(width+tw)/2)||(y>=height-76&&x>=(width-iw)/2&&x<=(width+iw)/2))return true;
        for(int i=config.modules.size()-1;i>=0;i--){HudModule m=config.modules.get(i);double px=px(m),py=py(m);
            if(x>=px&&x<=px+renderer.width(m,client)*m.scale&&y>=py&&y<=py+24*m.scale){selected=m;offsetX=x-px;offsetY=y-py;dragging=click.button()==0;if(click.button()==1)m.enabled=!m.enabled;resetArmed=false;rebuild();return true;}}
        selected=null;dragging=false;resetArmed=false;rebuild();return false;
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy){
        if(selected==null||!dragging)return super.mouseDragged(click,dx,dy);
        double x=click.x()-offsetX,y=click.y()-offsetY;if(click.hasShift()){x=Math.round(x/8)*8;y=Math.round(y/8)*8;}
        selected.x=(float)Math.clamp(x/width,0,1);selected.y=(float)Math.clamp(y/height,0,1);return true;
    }
    @Override public boolean mouseReleased(Click click){dragging=false;return super.mouseReleased(click);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(selected==null)return false;resize((float)vertical*.1f);return true;}
    @Override public boolean keyPressed(KeyInput input){
        if(input.key()==GLFW.GLFW_KEY_R){reset();return true;}
        if(selected!=null){if(input.key()==GLFW.GLFW_KEY_C){color();return true;}if(input.key()==GLFW.GLFW_KEY_B){selected.background=selected.background==0?0xDB1B1E26:0;rebuild();return true;}
            int step=input.hasShift()?8:1;switch(input.key()){case GLFW.GLFW_KEY_LEFT->selected.x-=step/(float)width;case GLFW.GLFW_KEY_RIGHT->selected.x+=step/(float)width;case GLFW.GLFW_KEY_UP->selected.y-=step/(float)height;case GLFW.GLFW_KEY_DOWN->selected.y+=step/(float)height;default->{return super.keyPressed(input);}}selected.normalize();return true;}
        return super.keyPressed(input);
    }
    @Override public void close(){try{config.save();features.save();client.setScreen(new ModuleScreen(config,renderer,features));}catch(IOException e){error="Layout konnte nicht gespeichert werden.";}}
    @Override public boolean shouldPause(){return false;}
}
