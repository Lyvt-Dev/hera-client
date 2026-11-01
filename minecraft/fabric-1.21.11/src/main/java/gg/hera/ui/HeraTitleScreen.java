package gg.hera.ui;
import gg.hera.features.FeatureConfig;
import gg.hera.hud.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.player.SkinTextures;
import java.util.function.Supplier;

/** Own Hera composition; uses the game's panorama without redistributing it. */
public final class HeraTitleScreen extends Screen {
    private final HudConfig hud;private final HudRenderer renderer;private final FeatureConfig features;
    private int left,top,buttonWidth,rowHeight;
    private Supplier<SkinTextures> skin;
    public HeraTitleScreen(HudConfig hud,HudRenderer renderer,FeatureConfig features){super(HeraTheme.text("Hera"));this.hud=hud;this.renderer=renderer;this.features=features;}
    @Override protected void init(){
        // Minecraft caches/fetches skins asynchronously; never block the menu.
        skin=client.getSkinProvider().supplySkinTextures(client.getGameProfile(),true);
        left=Math.max(18,width/22);buttonWidth=Math.min(218,Math.max(152,width/3));rowHeight=height<300?34:42;top=Math.max(86,(height-rowHeight*4)/2);
        addDrawableChild(action(top,"Einzelspieler","Deine Welten","Biome",()->client.setScreen(new SelectWorldScreen(this))));
        var multiplayer=action(top+rowHeight,"Mehrspieler","Gemeinsam spielen","Server",()->client.setScreen(new MultiplayerScreen(this)));multiplayer.active=client.isMultiplayerEnabled();addDrawableChild(multiplayer);
        addDrawableChild(action(top+rowHeight*2,"Dein Hera","Module & HUD gestalten","Koordinaten",()->client.setScreen(new ModuleScreen(hud,renderer,features))));
        addDrawableChild(action(top+rowHeight*3,"Einstellungen","Minecraft konfigurieren","Speicher",()->client.setScreen(new OptionsScreen(this,client.options))));
        addDrawableChild(new HeraButton(left,height-34,buttonWidth,23,"Spiel beenden",false,client::scheduleStop));
    }
    private HeraButton action(int y,String title,String subtitle,String icon,Runnable press){return new HeraButton(left,y,buttonWidth,rowHeight-5,title,false,press){
        @Override protected void renderWidget(DrawContext c,int mx,int my,float d){boolean hover=active&&(isHovered()||isFocused());int x=getX()+(hover?3:0),y=getY();
            HeraTheme.round(c,x-1,y-1,width+2,height+2,11,hover?0xCCF49B63:0x33FFFFFF);HeraTheme.round(c,x,y,width,height,10,hover?0xF01A1E26:0xD90F1218);
            int tile=height-14;HeraTheme.round(c,x+8,y+7,tile,tile,7,hover?HeraTheme.ACCENT:0xFF20252E);HeraTheme.icon(c,icon,x+8+(tile-13)/2,y+7+(tile-13)/2,13,!active?HeraTheme.MUTED:hover?0xFF21150E:HeraTheme.ACCENT);
            int tx=x+tile+18;HeraTheme.label(c,title,tx,height>=35?y+8:y+(height-9)/2,active?HeraTheme.TEXT:HeraTheme.MUTED);if(height>=35)HeraTheme.label(c,HeraTheme.fit(subtitle,width-tile-44),tx,y+22,HeraTheme.MUTED);
            HeraTheme.label(c,"›",x+width-16,y+(height-9)/2,hover?HeraTheme.ACCENT:0xFF6C7480);}
    };}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        renderPanoramaBackground(c,delta);c.fillGradient(0,0,width,height,0x30050709,0xD005070A);
        int panel=Math.min(width,left+buttonWidth+left);c.fill(0,0,panel,height,0xB00B0D11);c.fill(panel,0,panel+1,height,0x22FFFFFF);
        c.fillGradient(0,0,panel,90,0x22F49B63,0x00F49B63);
        HeraTheme.logo(c,left-2,18,40);HeraTheme.title(c,"hera",left+46,26,2f,HeraTheme.TEXT);
        c.fill(left,66,left+34,68,HeraTheme.ACCENT);HeraTheme.label(c,"Deine Welt. Dein Setup.",left,74,HeraTheme.MUTED);
        String name=client.getSession().getUsername();int pw=Math.min(210,Math.max(160,HeraTheme.measure(HeraTheme.text(name),false)+70));int px=width-pw-18;
        HeraTheme.round(c,px-1,19,pw+2,46,12,0x33FFFFFF);HeraTheme.round(c,px,20,pw,44,11,0xE00F1218);
        HeraTheme.round(c,px+9,29,26,26,7,HeraTheme.ACCENT_SOFT);
        if(skin!=null)PlayerSkinDrawer.draw(c,skin.get(),px+11,31,22);
        HeraTheme.label(c,HeraTheme.fit(name,pw-60),px+45,28,HeraTheme.TEXT);HeraTheme.label(c,"Minecraft-Konto",px+45,44,HeraTheme.MUTED);
        super.render(c,mx,my,delta);
        String version="Hera · Minecraft 1.21.11";int vw=HeraTheme.measure(HeraTheme.text(version),false);
        HeraTheme.round(c,width-vw-30,height-28,vw+16,17,8,0xC00F1218);HeraTheme.label(c,version,width-vw-22,height-23,0xFFB0BBCB);
    }
    @Override public boolean shouldCloseOnEsc(){return false;}
}
