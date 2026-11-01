package gg.hera.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/** Shared numeric control: mouse drag and vanilla slider keyboard navigation. */
public final class HeraSlider extends SliderWidget {
    private final Supplier<String> label;
    private final DoubleConsumer changed;
    private final double min,max,step;
    public HeraSlider(int x,int y,int w,int h,Supplier<String> label,double current,double min,double max,double step,DoubleConsumer changed){
        super(x,y,w,h,HeraTheme.text(label.get()),(current-min)/(max-min));
        this.label=label;this.min=min;this.max=max;this.step=step;this.changed=changed;
    }
    public static double quantize(double value,double min,double max,double step){return Math.clamp(min+Math.round((value-min)/step)*step,min,max);}
    @Override protected void updateMessage(){if(label!=null)setMessage(HeraTheme.text(label.get()));}
    @Override protected void applyValue(){changed.accept(quantize(min+value*(max-min),min,max,step));updateMessage();}
    @Override public void renderWidget(DrawContext c,int mx,int my,float delta){
        HeraTheme.card(c,getX(),getY(),width,height,8,isHovered()||isFocused()?HeraTheme.CARD_HOVER:HeraTheme.CARD,isFocused());
        String full=label.get();int split=full.lastIndexOf(": ");
        if(split>0){String value=full.substring(split+2);int vw=HeraTheme.measure(HeraTheme.text(value),false);
            HeraTheme.round(c,getX()+width-vw-18,getY()+4,vw+10,13,4,HeraTheme.ACCENT_SOFT);HeraTheme.label(c,value,getX()+width-vw-13,getY()+6,HeraTheme.ACCENT);
            HeraTheme.label(c,HeraTheme.fit(full.substring(0,split),width-vw-36),getX()+10,getY()+6,HeraTheme.TEXT);}
        else HeraTheme.label(c,HeraTheme.fit(full,width-20),getX()+10,getY()+6,HeraTheme.TEXT);
        int track=width-28,knob=(int)Math.round(track*value),yy=getY()+height-11;
        HeraTheme.round(c,getX()+14,yy,track,4,2,0xFF2A303A);
        HeraTheme.round(c,getX()+14,yy,Math.max(4,knob),4,2,HeraTheme.ACCENT);
        HeraTheme.round(c,getX()+8+knob,yy-4,12,12,6,0x66F49B63);
        HeraTheme.round(c,getX()+9+knob,yy-3,10,10,5,0xFFF3F5FA);
    }
}
