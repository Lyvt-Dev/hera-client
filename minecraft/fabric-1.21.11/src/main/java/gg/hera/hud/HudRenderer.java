package gg.hera.hud;

import net.minecraft.client.MinecraftClient;
import gg.hera.ui.HeraTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import gg.hera.features.Esp;
import net.minecraft.entity.EquipmentSlot;

public final class HudRenderer {
    private final HudConfig config;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Map<String, String> sampled = new HashMap<>();
    private final Map<String,net.minecraft.text.Text> labels=new HashMap<>();
    private final Map<String,Integer> widths=new HashMap<>();
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final Map<String,net.minecraft.text.Text> KEYS=new HashMap<>();
    private static final net.minecraft.text.Text[] DURABILITY=new net.minecraft.text.Text[101];
    static {for(String key:java.util.List.of("W","A","S","D","LMB","RMB","SPACE"))KEYS.put(key,HeraTheme.text(key));for(int i=0;i<=100;i++)DURABILITY[i]=HeraTheme.text(i+"%");}
    private long lastSample;
    public HudRenderer(HudConfig config) { this.config = config; }
    public void invalidate(){sampled.clear();labels.clear();widths.clear();lastSample=0;}
    public String value(HudModule m, MinecraftClient c) {
        long now = System.nanoTime();
        if (now - lastSample >= 250_000_000L || sampled.isEmpty()) {
            sampled.clear();labels.clear();widths.clear();
            lastSample = now;
        }
        return sampled.computeIfAbsent(m.id,id->sample(m,c));
    }
    private String sample(HudModule m, MinecraftClient c) {
        if (c.player == null || c.world == null) return m.id + " · keine Welt";
        return switch (m.id) {
            case "FPS" -> c.getCurrentFps() + " FPS";
            case "Koordinaten" -> String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f", c.player.getX(), c.player.getY(), c.player.getZ());
            case "Ping" -> {
                PlayerListEntry entry = c.getNetworkHandler() == null ? null : c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
                yield entry == null ? "Ping · nicht verfügbar" : entry.getLatency() + " ms";
            }
            case "Speicher" -> "RAM " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576 + " / " + Runtime.getRuntime().maxMemory() / 1048576 + " MB";
            case "Uhr" -> LocalTime.now().format(CLOCK);
            case "Richtung" -> c.player.getHorizontalFacing().asString();
            case "Geschwindigkeit" -> String.format(Locale.ROOT, "%.2f Blöcke/s", c.player.getVelocity().horizontalLength() * 20);
            case "Biome" -> c.world.getBiome(c.player.getBlockPos()).getKey().map(key -> key.getValue().toString()).orElse("Biome · unbekannt");
            case "Server" -> c.getCurrentServerEntry() == null ? "Singleplayer" : c.getCurrentServerEntry().address;
            default -> m.id;
        };
    }
    private net.minecraft.text.Text label(HudModule m,MinecraftClient c){String value=value(m,c);return labels.computeIfAbsent(m.id,id->HeraTheme.text(value));}
    public int width(HudModule module, MinecraftClient client) { return switch(module.id){case "Keystrokes"->82;case "Armor HUD"->120;default->{var text=label(module,client);yield widths.computeIfAbsent(module.id,id->client.textRenderer.getWidth(text)+16);}}; }
    public int height(HudModule module){return switch(module.id){case "Keystrokes"->44+(Esp.options().keySpace?22:0)+(Esp.options().keyMouse?22:0);case "Armor HUD"->Esp.options().armorDurability?38:24;default->20;};}
    public void render(DrawContext ctx, boolean editing) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null && !editing || c.options.hudHidden && !editing) return;
        for (HudModule m : config.modules) {
            if (!m.enabled && !editing) continue;
            if(editing)m.normalize();int width=width(m,c),height=height(m);
            int x = Math.min(Math.round(m.x * ctx.getScaledWindowWidth()), Math.max(0, ctx.getScaledWindowWidth() - Math.round(width * m.scale)));
            int y = Math.min(Math.round(m.y * ctx.getScaledWindowHeight()), Math.max(0, ctx.getScaledWindowHeight() - Math.round(height * m.scale)));
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(x, y); ctx.getMatrices().scale(m.scale, m.scale);
            if(m.id.equals("Keystrokes")){keys(ctx,c,m);ctx.getMatrices().popMatrix();continue;}
            if(m.id.equals("Armor HUD")){armor(ctx,c,m);ctx.getMatrices().popMatrix();continue;}
            if(m.background!=0) {
                HeraTheme.round(ctx,0,0,width,20,7,0x28FFFFFF);
                HeraTheme.round(ctx,1,1,width-2,18,6,m.background);
            }
            ctx.drawText(c.textRenderer,label(m,c),8,6,m.enabled?m.color:0xFF8892A4,false);
            ctx.getMatrices().popMatrix();
        }
    }
    private void key(DrawContext ctx,HudModule m,String label,int x,int y,int width,boolean down){HeraTheme.round(ctx,x,y,width,20,4,down?m.color:m.background);var c=MinecraftClient.getInstance();var text=KEYS.get(label);int tw=c.textRenderer.getWidth(text);ctx.drawText(c.textRenderer,text,x+(width-tw)/2,y+6,down?0xFF18202A:m.color,false);}
    private void keys(DrawContext ctx,MinecraftClient c,HudModule m){boolean input=c.currentScreen==null;
        key(ctx,m,"W",28,0,26,input&&c.options.forwardKey.isPressed());key(ctx,m,"A",0,22,26,input&&c.options.leftKey.isPressed());key(ctx,m,"S",28,22,26,input&&c.options.backKey.isPressed());key(ctx,m,"D",56,22,26,input&&c.options.rightKey.isPressed());
        if(Esp.options().keyMouse){key(ctx,m,"LMB",0,44,40,input&&c.mouse.wasLeftButtonClicked());key(ctx,m,"RMB",42,44,40,input&&c.mouse.wasRightButtonClicked());}
        if(Esp.options().keySpace)key(ctx,m,"SPACE",0,Esp.options().keyMouse?66:44,82,input&&c.options.jumpKey.isPressed());
    }
    private void armor(DrawContext ctx,MinecraftClient c,HudModule m){
        HeraTheme.round(ctx,0,0,120,height(m),6,m.background);if(c.player==null){HeraTheme.label(ctx,"Armor HUD",8,8,m.color);return;}
        for(int i=0;i<ARMOR.length;i++){var stack=c.player.getEquippedStack(ARMOR[i]);int x=5+i*29;ctx.drawItem(stack,x,4);if(Esp.options().armorDurability && !stack.isEmpty() && stack.isDamageable()){int percent=Math.clamp(Math.round(100f*(stack.getMaxDamage()-stack.getDamage())/stack.getMaxDamage()),0,100);ctx.drawText(c.textRenderer,DURABILITY[percent],x,25,percent<20?0xFFFF7D9C:m.color,false);}}
    }
}
