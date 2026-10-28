package gg.hera.hud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import gg.hera.ui.HeraTheme;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Shared measurement/render geometry. Values sampled at 4 Hz; input stays immediate. */
public final class HudRenderer {
    private final HudConfig config;private final Map<String,Sample> samples=new HashMap<>();
    private record Sample(Text[] lines,int[] widths,int width){}
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final net.minecraft.item.Item[] PREVIEW_ARMOR={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS};
    private static final DateTimeFormatter CLOCK=DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Text[] PERCENT=new Text[101],VANILLA_PERCENT=new Text[101];
    private static final Text[] VANILLA_KEYS={Text.literal("W"),Text.literal("A"),Text.literal("S"),Text.literal("D"),Text.literal("LMB"),Text.literal("RMB"),Text.literal("SPACE"),Text.literal("SPRINT")};
    private static final Text[] KEY_LABELS={HeraTheme.text("W"),HeraTheme.text("A"),HeraTheme.text("S"),HeraTheme.text("D"),HeraTheme.text("LMB"),HeraTheme.text("RMB"),HeraTheme.text("SPACE"),HeraTheme.text("SPRINT")};
    private final float[] keyAmount=new float[8],keyFrom=new float[8];private final boolean[] keyDown=new boolean[8];private final long[] keyChanged=new long[8];private long keyNow;
    private MediaState.Track musicTrack;private int musicPadding=-1;private boolean musicVanilla;private Text musicTitle,musicArtist,musicElapsed,musicRemaining,musicStatus;private long musicSecond=-1;
    private java.util.List<net.minecraft.util.Identifier> effectIcons=java.util.List.of();
    private final float[] frameMs=new float[120];private int frameCursor,frameCount;private long lastFrame,lastSample,sessionStart;private Object world,connection;
    static {for(int i=0;i<=100;i++){PERCENT[i]=HeraTheme.text(i+"%");VANILLA_PERCENT[i]=Text.literal(i+"%");}}
    public HudRenderer(HudConfig config){this.config=config;}
    public void invalidate(){samples.clear();lastSample=0;}
    private Text text(HudModule m,String s){return m.vanillaFont?Text.literal(s):HeraTheme.text(s);}
    private int accent(HudModule m){return m.useGlobalAccent?config.accent:m.accent;}
    private int alpha(HudModule m,int color){return HudGeometry.alpha(color,m.opacity);}
    public void tick(MinecraftClient c){var handler=c.getNetworkHandler();if(connection!=handler){connection=handler;sessionStart=System.nanoTime();}if(world!=c.world){world=c.world;frameCount=0;frameCursor=0;lastFrame=0;invalidate();}}
    private void refresh(MinecraftClient c){tick(c);long now=System.nanoTime();if(now-lastSample>250_000_000L){lastSample=now;samples.clear();}}
    public String value(HudModule m,MinecraftClient c){return String.join(" / ",values(m,c));}
    private List<String> values(HudModule m,MinecraftClient c){
        if(c.player==null||c.world==null)return m.id.equals("Keystrokes")?List.of("— CPS","— CPS"):List.of(m.id+" · Vorschau");
        return switch(m.id){
            case "FPS"->{var list=new ArrayList<String>();list.add(c.getCurrentFps()+" FPS");if(m.showFrameTime){float sum=0;for(int i=0;i<frameCount;i++)sum+=frameMs[i];list.add(frameCount==0?"Framezeit —":String.format(Locale.ROOT,"%.1f ms / Frame",sum/frameCount));}yield list;}
            case "Ping"->{var e=c.getNetworkHandler()==null?null:c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());yield List.of(e==null?"Ping —":c.isInSingleplayer()?"Lokal":e.getLatency()+" ms");}
            case "Koordinaten"->{String format="%."+m.precision+"f";var list=new ArrayList<String>();if(m.expanded){list.add(String.format(Locale.ROOT,"X "+format,c.player.getX()));list.add(String.format(Locale.ROOT,"Y "+format,c.player.getY()));list.add(String.format(Locale.ROOT,"Z "+format,c.player.getZ()));}else list.add(String.format(Locale.ROOT,"XYZ "+format+" / "+format+" / "+format,c.player.getX(),c.player.getY(),c.player.getZ()));if(m.showChunk)list.add("Chunk "+c.player.getChunkPos().x+" / "+c.player.getChunkPos().z);if(m.showBiome)list.add(biome(c));if(m.showFacing)list.add(c.player.getHorizontalFacing().asString());yield list;}
            case "Keystrokes"->{long now=System.nanoTime();yield List.of(ClickMeter.INPUT.count(0,now,1)+" CPS",ClickMeter.INPUT.count(1,now,1)+" CPS");}
            case "CPS"->{long now=System.nanoTime();int l=ClickMeter.INPUT.count(0,now,m.cpsInterval),r=ClickMeter.INPUT.count(1,now,m.cpsInterval);yield List.of(String.format(Locale.ROOT,"%.1f L / %.1f R CPS",l/(float)m.cpsInterval,r/(float)m.cpsInterval),String.format(Locale.ROOT,"%.1f CPS gesamt · %d s",(l+r)/(float)m.cpsInterval,m.cpsInterval));}
            case "Effekte"->{var list=new ArrayList<String>();for(var effect:c.player.getStatusEffects()){if(list.size()>=16)break;String duration=effect.isInfinite()?"∞":duration(effect.getDuration()/20);String name=effect.getEffectType().value().getName().getString();list.add(m.expanded?name+" "+(effect.getAmplifier()+1)+" · "+duration:duration);}yield list.isEmpty()?List.of("Keine Effekte"):list;}
            case "Sprint"->List.of(c.player.isSneaking()?"Schleichen":c.player.isSprinting()?"Sprinten":"Gehen");
            case "Session"->List.of("Session "+duration((System.nanoTime()-sessionStart)/1_000_000_000L));
            case "Uhr"->List.of(LocalTime.now().format(CLOCK));
            case "Richtung"->List.of(c.player.getHorizontalFacing().asString());
            case "Geschwindigkeit"->List.of(String.format(Locale.ROOT,"%.2f Blöcke/s",c.player.getVelocity().horizontalLength()*20));
            case "Biome"->List.of(biome(c));
            case "Server"->List.of(c.getCurrentServerEntry()==null?"Singleplayer":c.getCurrentServerEntry().address);
            case "Speicher"->List.of("RAM "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576+" / "+Runtime.getRuntime().maxMemory()/1048576+" MB");
            default->List.of(m.id);
        };
    }
    private static String biome(MinecraftClient c){return c.world.getBiome(c.player.getBlockPos()).getKey().map(k->k.getValue().getPath().replace('_',' ')).orElse("Biom —");}
    private static String duration(long seconds){return seconds>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
    private Sample sample(HudModule m,MinecraftClient c){refresh(c);return samples.computeIfAbsent(m.id,id->{if(id.equals("Effekte")&&c.player!=null){var icons=new ArrayList<net.minecraft.util.Identifier>();for(var effect:c.player.getStatusEffects()){if(icons.size()>=16)break;var effectId=net.minecraft.registry.Registries.STATUS_EFFECT.getId(effect.getEffectType().value());icons.add(net.minecraft.util.Identifier.of(effectId.getNamespace(),"mob_effect/"+effectId.getPath()));}effectIcons=List.copyOf(icons);}
        var lines=values(m,c).stream().map(s->text(m,s)).toArray(Text[]::new);int w=0;int[] measured=new int[lines.length];for(int i=0;i<lines.length;i++){measured[i]=HeraTheme.measure(lines[i],m.vanillaFont);w=Math.max(w,measured[i]);}return new Sample(lines,measured,w);});}
    private boolean mouse(HudModule m){return m.showMouse&&gg.hera.features.Esp.options().keyMouse;}
    private boolean space(HudModule m){return m.showSpace&&gg.hera.features.Esp.options().keySpace;}
    public int width(HudModule m,MinecraftClient c){return switch(m.id){case "Spotify"->200;case "Keystrokes"->76+2*m.padding;case "Armor HUD"->(m.vertical?m.showDurability?52:20:4*30)+2*m.padding;case "Inventar"->9*18+2*m.padding;default->sample(m,c).width+2*m.padding+(m.id.equals("Effekte")?20:0);};}
    public int height(HudModule m){return switch(m.id){case "Spotify"->52+2*m.padding;case "Keystrokes"->40+(mouse(m)?m.showCps?30:20:0)+(space(m)?20:0)+(m.showSprint?16:0)+2*m.padding;case "Armor HUD"->(m.vertical?4*24:m.showDurability?36:22)+2*m.padding;case "Inventar"->3*18+2*m.padding;default->{var s=sample(m,MinecraftClient.getInstance());yield s.lines.length*(m.id.equals("Effekte")?20:12)+2*m.padding+(m.id.equals("FPS")&&m.showGraph?24:0);}};}
    public int x(HudModule m,int screen,MinecraftClient c){return HudGeometry.position(m.x,m.anchorX,screen,Math.round(width(m,c)*m.scale));}
    public int y(HudModule m,int screen){return HudGeometry.position(m.y,m.anchorY,screen,Math.round(height(m)*m.scale));}
    public void render(DrawContext ctx,boolean editing){long start=HudTiming.enabled?System.nanoTime():0;render(ctx,editing,null);if(HudTiming.enabled&&!editing)HudTiming.record(System.nanoTime()-start);}
    public void render(DrawContext ctx,boolean editing,String selected){var c=MinecraftClient.getInstance();if((c.player==null||c.options.hudHidden)&&!editing)return;
        boolean frames=false;for(var module:config.modules)if(module.enabled&&module.id.equals("FPS")&&(module.showFrameTime||module.showGraph)){frames=true;break;}long now=System.nanoTime();
        if(frames&&!editing){if(lastFrame>0){frameMs[frameCursor++%frameMs.length]=(now-lastFrame)/1_000_000f;frameCount=Math.min(frameCount+1,frameMs.length);}lastFrame=now;}else lastFrame=0;
        for(var m:config.modules){if(!m.enabled&&(!editing||selected!=null&&!m.id.equals(selected)))continue;int w=width(m,c),h=height(m);ctx.getMatrices().pushMatrix();ctx.getMatrices().translate(x(m,ctx.getScaledWindowWidth(),c),y(m,ctx.getScaledWindowHeight()));ctx.getMatrices().scale(m.scale,m.scale);
            if(m.id.equals("Keystrokes"))keys(ctx,c,m);else{if(m.background!=0)HeraTheme.round(ctx,0,0,w,h,m.radius,alpha(m,m.background));switch(m.id){case "Spotify"->music(ctx,c,m);case "Armor HUD"->armor(ctx,c,m,editing);case "Inventar"->inventory(ctx,c,m);default->lines(ctx,c,m);}}
            ctx.getMatrices().popMatrix();}
    }
    private void lines(DrawContext ctx,MinecraftClient c,HudModule m){var s=sample(m,c);int color=m.color;if(m.id.equals("Ping")&&m.latencyColor&&c.player!=null&&c.getNetworkHandler()!=null&&!c.isInSingleplayer()){var e=c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());if(e!=null)color=e.getLatency()<80?0xFF70CBA4:e.getLatency()<180?0xFFF1B66B:0xFFE97676;}
        int row=m.id.equals("Effekte")?20:12;int icon=m.id.equals("Effekte")?20:0;for(int i=0;i<s.lines.length;i++)HeraTheme.hudText(ctx,s.lines[i],m.padding+icon+(s.width-s.widths[i])*m.alignment/2,m.padding+i*row+1,alpha(m,color),m.vanillaFont,m.textShadow);
        if(m.id.equals("Effekte")&&c.player!=null)for(int i=0;i<effectIcons.size();i++)ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED,effectIcons.get(i),m.padding,m.padding+i*20,16,16,alpha(m,0xFFFFFFFF));
        if(m.id.equals("FPS")&&m.showGraph){int width=Math.min(100,width(m,c)-m.padding*2);int y=height(m)-m.padding;for(int i=0;i<Math.min(frameCount,width);i++){float ms=frameMs[Math.floorMod(frameCursor-1-i,frameMs.length)];int bar=Math.clamp(Math.round(ms/2),1,22);ctx.fill(m.padding+width-i-1,y-bar,m.padding+width-i,y,alpha(m,accent(m)));}}
    }
    private void key(DrawContext ctx,MinecraftClient c,HudModule m,int label,int x,int y,int w,boolean down){if(keyDown[label]!=down){keyFrom[label]=keyAmount[label];keyDown[label]=down;keyChanged[label]=keyNow;}float progress=config.animations?Math.clamp((keyNow-keyChanged[label])/120_000_000f,0,1):1;float amount=keyFrom[label]+((down?1:0)-keyFrom[label])*progress;keyAmount[label]=amount;
        HeraTheme.round(ctx,x,y,w,label>=4&&label<=5&&m.showCps?28:18,Math.min(4,m.radius),alpha(m,blend(m.background,accent(m),amount)));var t=(m.vanillaFont?VANILLA_KEYS:KEY_LABELS)[label];HeraTheme.hudText(ctx,t,x+(w-HeraTheme.measure(t,m.vanillaFont))/2,y+4,alpha(m,down?0xFF111318:m.color),m.vanillaFont,false);}
    private static int blend(int a,int b,float t){int out=0;for(int shift=0;shift<=24;shift+=8)out|=Math.round((a>>>shift&255)*(1-t)+(b>>>shift&255)*t)<<shift;return out;}
    private void keys(DrawContext ctx,MinecraftClient c,HudModule m){keyNow=System.nanoTime();int p=m.padding;boolean input=c.currentScreen==null;key(ctx,c,m,0,p+26,p,24,input&&c.options.forwardKey.isPressed());key(ctx,c,m,1,p,p+20,24,input&&c.options.leftKey.isPressed());key(ctx,c,m,2,p+26,p+20,24,input&&c.options.backKey.isPressed());key(ctx,c,m,3,p+52,p+20,24,input&&c.options.rightKey.isPressed());int y=p+40;
        if(mouse(m)){key(ctx,c,m,4,p,y,37,input&&c.mouse.wasLeftButtonClicked());key(ctx,c,m,5,p+39,y,37,input&&c.mouse.wasRightButtonClicked());if(m.showCps){var sampled=sample(m,c);for(int i=0;i<Math.min(2,sampled.lines.length);i++)HeraTheme.hudText(ctx,sampled.lines[i],p+i*39+(37-HeraTheme.measure(sampled.lines[i],m.vanillaFont))/2,y+17,alpha(m,0xFFB8C0CD),m.vanillaFont,false);}y+=m.showCps?30:20;}
        if(space(m)){key(ctx,c,m,6,p,y,76,input&&c.options.jumpKey.isPressed());y+=20;}if(m.showSprint)HeraTheme.hudText(ctx,(m.vanillaFont?VANILLA_KEYS:KEY_LABELS)[7],p+18,y+3,alpha(m,input&&c.player!=null&&c.player.isSprinting()?accent(m):HeraTheme.MUTED),m.vanillaFont,false);
    }
    private void armor(DrawContext ctx,MinecraftClient c,HudModule m,boolean editing){for(int i=0;i<ARMOR.length;i++){var stack=c.player==null?editing?PREVIEW_ARMOR[i].getDefaultStack():net.minecraft.item.ItemStack.EMPTY:c.player.getEquippedStack(ARMOR[i]);int x=m.padding+(m.vertical?0:i*30),y=m.padding+(m.vertical?i*24:0);ctx.drawItem(stack,x,y);if(stack.isEmpty())continue;if(stack.isDamageable()){int percent=Math.clamp(Math.round(100f*(stack.getMaxDamage()-stack.getDamage())/stack.getMaxDamage()),0,100),col=percent<20?0xFFE97676:percent<40?0xFFF1B66B:m.color;if(m.showDurability&&gg.hera.features.Esp.options().armorDurability)HeraTheme.hudText(ctx,(m.vanillaFont?VANILLA_PERCENT:PERCENT)[percent],x+(m.vertical?22:0),y+(m.vertical?4:23),alpha(m,col),m.vanillaFont,false);if(m.showBars){int by=y+18;ctx.fill(x,by,x+20,by+2,alpha(m,0xFF373B44));ctx.fill(x,by,x+Math.round(percent*.2f),by+2,alpha(m,percent<20?0xFFE97676:accent(m)));}}}}
    private void music(DrawContext ctx,MinecraftClient c,HudModule m){var track=MediaState.current();int p=m.padding;long position=MediaState.position(track),second=position/1000;
        if(musicTrack!=track||musicPadding!=p||musicVanilla!=m.vanillaFont){musicTrack=track;musicPadding=p;musicVanilla=m.vanillaFont;musicSecond=-1;musicTitle=text(m,HeraTheme.fit(track.available()?track.title():"Keine Wiedergabe",200-p*2));String subtitle=track.available()?track.source()+(track.artist().isBlank()?"":" · "+track.artist())+(track.playing()?"":" · Pause"):"Spotify oder Browser öffnen";musicArtist=text(m,HeraTheme.fit(subtitle,200-p*2));musicStatus=text(m,track.available()?"Zeit nicht verfügbar":"Keine Windows-Medieninformationen");}
        HeraTheme.hudText(ctx,musicTitle,p,p+1,alpha(m,m.color),m.vanillaFont,m.textShadow);HeraTheme.hudText(ctx,musicArtist,p,p+16,alpha(m,HeraTheme.MUTED),m.vanillaFont,false);
        if(track.available()&&track.duration()>0){if(musicSecond!=second){musicSecond=second;musicElapsed=text(m,duration(second));musicRemaining=text(m,"−"+duration(Math.max(0,track.duration()-position)/1000));}HeraTheme.hudText(ctx,musicElapsed,p,p+34,alpha(m,HeraTheme.MUTED),m.vanillaFont,false);HeraTheme.hudText(ctx,musicRemaining,200-p-HeraTheme.measure(musicRemaining,m.vanillaFont),p+34,alpha(m,HeraTheme.MUTED),m.vanillaFont,false);HeraTheme.round(ctx,p,p+49,200-p*2,2,1,alpha(m,0xFF343942));int progress=(int)Math.round((200-p*2)*position/(double)track.duration());if(progress>0)HeraTheme.round(ctx,p,p+49,progress,2,1,alpha(m,accent(m)));}
        else HeraTheme.hudText(ctx,musicStatus,p,p+38,alpha(m,HeraTheme.MUTED),m.vanillaFont,false);
    }
    private void inventory(DrawContext ctx,MinecraftClient c,HudModule m){for(int i=0;i<27;i++){int x=m.padding+i%9*18,y=m.padding+i/9*18;ctx.fill(x,y,x+16,y+16,alpha(m,0x12FFFFFF));if(c.player!=null)ctx.drawItem(c.player.getInventory().getStack(i+9),x,y);}}
}
