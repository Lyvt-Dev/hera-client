package gg.hera.ui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import java.util.function.IntConsumer;
public final class ColorPickerScreen extends Screen {
    private final Screen parent;private final IntConsumer changed;private int color,x,y,w,h;
    public ColorPickerScreen(Screen parent,int color,IntConsumer changed){super(HeraTheme.text("Farbe wählen"));this.parent=parent;this.color=color;this.changed=changed;}
    @Override protected void init(){w=Math.min(330,width-20);h=Math.min(220,height-20);x=(width-w)/2;y=(height-h)/2;clearChildren();
        String[] names={"Rot","Grün","Blau"};for(int i=0;i<3;i++){final int channel=i,shift=(2-i)*8;addDrawableChild(new SliderWidget(x+14,y+48+i*34,w-28,25,HeraTheme.text(names[i]),((color>>shift)&255)/255d){
            @Override protected void updateMessage(){setMessage(HeraTheme.text(names[channel]+" · "+Math.round(value*255)));}
            @Override protected void applyValue(){color=(color&~(255<<shift))|((int)Math.round(value*255)<<shift);changed.accept(color);updateMessage();}
            @Override public void renderWidget(DrawContext c,int mx,int my,float d){HeraTheme.round(c,getX(),getY(),getWidth(),getHeight(),5,HeraTheme.RAISED);HeraTheme.round(c,getX()+4,getY()+getHeight()-6,getWidth()-8,2,1,0xFF485466);int knob=(int)((getWidth()-12)*value);HeraTheme.round(c,getX()+3+knob,getY()+getHeight()-9,6,8,3,color);HeraTheme.label(c,getMessage().getString(),getX()+7,getY()+4,HeraTheme.TEXT);}
        });}
        addDrawableChild(new HeraButton(x+w-90,y+h-30,76,20,"Fertig",true,this::close));
    }
    @Override public void render(DrawContext c,int mx,int my,float d){c.fill(0,0,width,height,0x88080B11);HeraTheme.panel(c,x,y,w,h);HeraTheme.title(c,"Farbe wählen",x+14,y+17,1.2f,HeraTheme.TEXT);HeraTheme.round(c,x+w-46,y+12,30,23,5,color);super.render(c,mx,my,d);}
    @Override public void close(){changed.accept(color);client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
