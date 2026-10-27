package gg.hera.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;

public final class HeraTheme {
    public static final int BG=0xFF111318, PANEL=0xFF1B1E25, RAISED=0xFF242830, LINE=0xFF30353D;
    public static final int TEXT=0xFFF5F6F8, MUTED=0xFFA4ACB9, ACCENT=0xFFF49B63;
    private static final Style FONT=Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("hera","medium")));
    private static final Style BOLD=Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("hera","bold")));
    private static final Identifier LOGO=Identifier.of("hera","brand/hera");
    private static final Identifier[] ROUND=new Identifier[15];
    private static final java.util.Map<String,Identifier> ICONS=new java.util.HashMap<>();
    static {for(int i=1;i<ROUND.length;i++)ROUND[i]=Identifier.of("hera","round/"+i);for(String name:java.util.List.of("fps","coords","ping","memory","clock","direction","speed","biome","totem","server"))ICONS.put(name,Identifier.of("hera","icons/"+name));}
    private HeraTheme() {}
    /** Owner-supplied Hera identity, shared with the launcher. */
    public static void logo(DrawContext c,int x,int y,int size) {
        c.drawGuiTexture(RenderPipelines.GUI_TEXTURED,LOGO,x,y,size,size);
    }
    public static MutableText text(String value) { return Text.literal(value).setStyle(FONT); }
    public static void round(DrawContext c,int x,int y,int w,int h,int r,int color) {
        if(w<=0||h<=0)return;
        r=Math.max(0,Math.min(14,Math.min(r,Math.min(w,h)/2)));
        if(r==0){c.fill(x,y,x+w,y+h,color);return;}
        c.drawGuiTexture(RenderPipelines.GUI_TEXTURED,ROUND[r],x,y,w,h,color);
    }
    public static void icon(DrawContext c,String name,int x,int y,int size,int color){
        String id=switch(name){case "FPS","Zoom"->"fps";case "Koordinaten","Hitboxes","Item Highlighter"->"coords";case "Ping"->"ping";case "Speicher","Full Bright"->"memory";case "Uhr","TNT Timer","Time Changer"->"clock";case "Richtung","Free Look"->"direction";case "Geschwindigkeit","Freecam","Keystrokes"->"speed";case "Biome","Weather Changer","No Fog"->"biome";case "Kristall-Totem","Auto-Totem","Armor HUD","Rüstung ausblenden"->"totem";case "Item-ESP","Spieler-ESP","Mob-ESP"->"coords";case "Fast Items"->"speed";default->"server";};
        c.drawGuiTexture(RenderPipelines.GUI_TEXTURED,ICONS.get(id),x,y,size,size,color);
    }
    public static void panel(DrawContext c,int x,int y,int w,int h) {
        round(c,x-1,y-1,w+2,h+2,6,LINE);round(c,x,y,w,h,5,BG);
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
