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
        if(isHovered()||isFocused())HeraTheme.round(c,getX(),getY(),width,height,5,HeraTheme.RAISED);
        HeraTheme.label(c,HeraTheme.fit(label.get(),width-16),getX()+8,getY()+6,HeraTheme.TEXT);
        int track=width-24,knob=(int)Math.round(track*value),yy=getY()+height-10;
        HeraTheme.round(c,getX()+12,yy,track,3,1,HeraTheme.LINE);
        HeraTheme.round(c,getX()+12,yy,knob,3,1,HeraTheme.ACCENT);
        HeraTheme.round(c,getX()+8+knob,yy-3,9,9,4,isFocused()?HeraTheme.TEXT:HeraTheme.ACCENT);
    }
}
