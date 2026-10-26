package gg.hera.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;

public final class HeraTheme {
    public static final int BG=0xB014161C, PANEL=0x991B1E26, RAISED=0x88333C4A, LINE=0x38DCE6F8;
    public static final int TEXT=0xFFF3F4F8, MUTED=0xFF9DA6B8, ACCENT=0xFFF0A46E;
    private static final Style FONT=Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("hera","nunito")));
    private HeraTheme() {}
    public static MutableText text(String value) { return Text.literal(value).setStyle(FONT); }
    public static void round(DrawContext c,int x,int y,int w,int h,int r,int color) {
        if(w<=0||h<=0)return;
        r=Math.min(r,Math.min(w,h)/2); c.fill(x+r,y,x+w-r,y+h,color); c.fill(x,y+r,x+w,y+h-r,color);
        for(int row=0;row<r;row++) {
            int inset=(int)Math.ceil(r-Math.sqrt(r*r-(r-row-0.5)*(r-row-0.5)));
            c.fill(x+inset,y+row,x+w-inset,y+row+1,color);
            c.fill(x+inset,y+h-row-1,x+w-inset,y+h-row,color);
        }
    }
    public static void panel(DrawContext c,int x,int y,int w,int h) {
        round(c,x-4,y+4,w+8,h+4,14,0x28000000);round(c,x-1,y-1,w+2,h+2,14,0x50E5EDFF);round(c,x,y,w,h,13,BG);round(c,x+2,y+2,w-4,Math.min(30,h/2),11,0x0CFFFFFF);c.fill(x+15,y+1,x+w-15,y+2,0x45FFFFFF);
    }
    public static void label(DrawContext c,String value,int x,int y,int color) { c.drawText(MinecraftClient.getInstance().textRenderer,text(value),x,y,color,false); }
    public static void title(DrawContext c,String value,int x,int y,float scale,int color) {
        c.getMatrices().pushMatrix();c.getMatrices().translate(x,y);c.getMatrices().scale(scale,scale);
        c.drawText(MinecraftClient.getInstance().textRenderer,text(value).styled(s->s.withBold(true)),0,0,color,false);c.getMatrices().popMatrix();
    }
    public static String fit(String value,int width) {
        var renderer=MinecraftClient.getInstance().textRenderer;
        if(renderer.getWidth(text(value))<=width)return value;
        while(!value.isEmpty()&&renderer.getWidth(text(value+"…"))>width)value=value.substring(0,value.length()-1);
        return value+"…";
    }
    public static void toggle(DrawContext c,int x,int y,boolean on) {
        round(c,x,y,25,13,6,on?ACCENT:LINE);round(c,x+(on?14:2),y+2,9,9,4,on?0xFF24180F:0xFFB1BACB);
    }
}
