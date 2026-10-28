package gg.hera.ui;
import gg.hera.features.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import java.util.*;
public final class CrosshairPaintScreen extends Screen {
 private final Screen parent;private final FeatureConfig config;private final CrosshairCanvas canvas=new CrosshairCanvas();private final Deque<int[]> undo=new ArrayDeque<>();private int x,y,cell,lastX,lastY;private boolean painting,eraser,saving,dirty=true;
 private static final net.minecraft.util.Identifier PREVIEW=net.minecraft.util.Identifier.of("hera","dynamic/crosshair_editor");private int color=0xFFFFFFFF;private String status="Links malen · rechts radieren";
 public CrosshairPaintScreen(Screen parent,FeatureConfig f){super(HeraTheme.text("Fadenkreuz malen"));this.parent=parent;config=f;}
 private void remember(){undo.addLast(canvas.copy());while(undo.size()>32)undo.removeFirst();}
 @Override protected void init(){dirty=true;cell=Math.max(2,Math.min(7,(height-100)/32));int size=cell*32;x=(width-size-116)/2;y=(height-size)/2;clearChildren();int bx=x+size+12;addDrawableChild(new HeraButton(bx,y,104,24,eraser?"Radierer":"Pinsel",false,()->{eraser=!eraser;init();}));addDrawableChild(new HeraButton(bx,y+30,104,24,"Farbe wählen",false,()->client.setScreen(new ColorPickerScreen(this,color,v->color=v))));addDrawableChild(new HeraButton(bx,y+60,104,24,"Rückgängig",false,()->{if(!undo.isEmpty()){canvas.restore(undo.removeLast());dirty=true;}}));addDrawableChild(new HeraButton(bx,y+90,104,24,"Leeren",false,()->{remember();canvas.clear();dirty=true;}));addDrawableChild(new HeraButton(bx,y+120,104,24,"Übernehmen",true,this::save));addDrawableChild(new HeraButton(x,y+size+10,104,22,"Zurück",false,this::close));}
 private void save(){if(saving)return;saving=true;status="Wird gespeichert …";CustomCrosshair.saveCanvas(canvas.copy()).thenAccept(error->{saving=false;status=error.isEmpty()?"Gespeichert":error;if(error.isEmpty()){config.options.crosshairImage=true;try{config.save();client.setScreen(parent);}catch(Exception e){status="Einstellungen nicht gespeichert";}}});}
 private boolean inside(double mx,double my){return mx>=x&&mx<x+cell*32&&my>=y&&my<y+cell*32;}
 @Override public boolean mouseClicked(Click click,boolean doubled){if(super.mouseClicked(click,doubled))return true;if(!saving&&inside(click.x(),click.y())){remember();painting=true;lastX=(int)(click.x()-x)/cell;lastY=(int)(click.y()-y)/cell;canvas.line(lastX,lastY,lastX,lastY,eraser||click.button()==1?0:color);dirty=true;return true;}return false;}
 @Override public boolean mouseDragged(Click click,double dx,double dy){if(painting&&!saving){int ex=(int)Math.floor((click.x()-x)/cell),ey=(int)Math.floor((click.y()-y)/cell);canvas.line(lastX,lastY,ex,ey,eraser||click.button()==1?0:color);dirty=true;lastX=Math.clamp(ex,0,31);lastY=Math.clamp(ey,0,31);return true;}return super.mouseDragged(click,dx,dy);}
 @Override public boolean mouseReleased(Click click){painting=false;return super.mouseReleased(click);}
 @Override public void render(DrawContext c,int mx,int my,float d){c.fill(0,0,width,height,0xF0101216);HeraTheme.label(c,status,x,Math.max(8,y-20),HeraTheme.TEXT);if(dirty){var image=new net.minecraft.client.texture.NativeImage(32,32,false);for(int yy=0;yy<32;yy++)for(int xx=0;xx<32;xx++)image.setColorArgb(xx,yy,canvas.get(xx,yy));client.getTextureManager().registerTexture(PREVIEW,new net.minecraft.client.texture.NativeImageBackedTexture(()->"Hera crosshair canvas",image));dirty=false;}for(int yy=0;yy<8;yy++)for(int xx=0;xx<8;xx++){int px=x+xx*cell*4,py=y+yy*cell*4;c.fill(px,py,px+cell*4,py+cell*4,(xx+yy)%2==0?0xFF292D34:0xFF20242B);}c.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,PREVIEW,x,y,0,0,cell*32,cell*32,32,32,32,32,0xFFFFFFFF);super.render(c,mx,my,d);}
 @Override public void removed(){client.getTextureManager().destroyTexture(PREVIEW);}
 @Override public void close(){if(!saving)client.setScreen(parent);}
}
