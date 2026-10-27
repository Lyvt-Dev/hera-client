package gg.hera.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;

public final class HeraTheme {
    public static final int BG=0xFF10151D, PANEL=0xFF171E28, RAISED=0xFF242D39, LINE=0xFF2C3543;
    public static final int TEXT=0xFFF3F6FC, MUTED=0xFF98A5B8, ACCENT=0xFFFFB27C;
    private static final Style FONT=Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("hera","medium")));
    private static final Style BOLD=Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("hera","bold")));
    private HeraTheme() {}
    public static MutableText text(String value) { return Text.literal(value).setStyle(FONT); }
    public static void round(DrawContext c,int x,int y,int w,int h,int r,int color) {
        if(w<=0||h<=0)return;
        r=Math.max(0,Math.min(14,Math.min(r,Math.min(w,h)/2)));
        if(r==0){c.fill(x,y,x+w,y+h,color);return;}
        c.drawGuiTexture(RenderPipelines.GUI_TEXTURED,Identifier.of("hera","round/"+r),x,y,w,h,color);
    }
    public static void icon(DrawContext c,String name,int x,int y,int size,int color){
        String id=switch(name){case "FPS","Zoom"->"fps";case "Koordinaten","Hitboxes","Item Highlighter"->"coords";case "Ping"->"ping";case "Speicher","Full Bright"->"memory";case "Uhr","TNT Timer","Time Changer"->"clock";case "Richtung","Free Look"->"direction";case "Geschwindigkeit","Freecam","Keystrokes"->"speed";case "Biome","Weather Changer","No Fog"->"biome";case "Kristall-Totem","Auto-Totem","Armor HUD","Rüstung ausblenden"->"totem";case "Item-ESP","Spieler-ESP","Mob-ESP"->"coords";case "Fast Items"->"speed";default->"server";};
        c.drawGuiTexture(RenderPipelines.GUI_TEXTURED,Identifier.of("hera","icons/"+id),x,y,size,size,color);
    }
    public static void panel(DrawContext c,int x,int y,int w,int h) {
        round(c,x-4,y+5,w+8,h+3,13,0x30000000);
        round(c,x-1,y-1,w+2,h+2,11,0xFF394452);round(c,x,y,w,h,10,BG);
    }
    public static void label(DrawContext c,String value,int x,int y,int color) { c.drawText(MinecraftClient.getInstance().textRenderer,text(value),x,y,color,false); }
    public static void title(DrawContext c,String value,int x,int y,float scale,int color) {
        c.getMatrices().pushMatrix();c.getMatrices().translate(x,y);c.getMatrices().scale(scale,scale);
        c.drawText(MinecraftClient.getInstance().textRenderer,Text.literal(value).setStyle(BOLD),0,0,color,false);c.getMatrices().popMatrix();
    }
    public static String fit(String value,int width) {
        var renderer=MinecraftClient.getInstance().textRenderer;
        if(renderer.getWidth(text(value))<=width)return value;
        while(!value.isEmpty()&&renderer.getWidth(text(value+"…"))>width)value=value.substring(0,value.length()-1);
        return value+"…";
    }
    public static void toggle(DrawContext c,int x,int y,boolean on) {
        round(c,x,y,25,13,6,on?ACCENT:0xFF424A5B);round(c,x+(on?14:2),y+2,9,9,4,0xFFF3F5FA);
    }
}
