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
        @Override protected void renderWidget(DrawContext c,int mx,int my,float d){boolean hover=active&&(isHovered()||isFocused());int x=getX(),y=getY();HeraTheme.round(c,x-1,y-1,width+2,height+2,9,hover?0xAAAD7858:0x55414B5B);HeraTheme.round(c,x,y,width,height,8,hover?0xEF293342:0xDF141D2A);HeraTheme.round(c,x+9,y+7,25,height-14,6,hover?0xFF614937:0xFF293444);HeraTheme.icon(c,icon,x+15,y+12,13,active?HeraTheme.ACCENT:HeraTheme.MUTED);HeraTheme.label(c,title,x+44,y+7,active?HeraTheme.TEXT:HeraTheme.MUTED);if(height>=35)HeraTheme.label(c,HeraTheme.fit(subtitle,width-66),x+44,y+23,HeraTheme.MUTED);HeraTheme.label(c,"›",x+width-15,y+(height-9)/2,hover?HeraTheme.ACCENT:HeraTheme.MUTED);}
    };}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        renderPanoramaBackground(c,delta);c.fillGradient(0,0,width,height,0x50404B62,0xDE07101D);c.fill(0,0,Math.min(width,left+buttonWidth+32),height,0x40101927);
        HeraTheme.logo(c,left-2,16,40);HeraTheme.title(c,"hera",left+46,24,2f,HeraTheme.TEXT);HeraTheme.label(c,"Deine Welt. Dein Setup.",left,67,HeraTheme.MUTED);
        String name=client.getSession().getUsername();int pw=Math.min(200,Math.max(158,HeraTheme.measure(HeraTheme.text(name),false)+64));int px=width-pw-18;
        HeraTheme.round(c,px-1,19,pw+2,46,10,0x554D596A);HeraTheme.round(c,px,20,pw,44,9,0xD8111925);
        HeraTheme.round(c,px+9,29,26,26,6,0xFF354151);
        if(skin!=null)PlayerSkinDrawer.draw(c,skin.get(),px+11,31,22);
        HeraTheme.label(c,HeraTheme.fit(name,pw-53),px+45,28,HeraTheme.TEXT);HeraTheme.label(c,"Minecraft-Konto",px+45,44,HeraTheme.MUTED);
        super.render(c,mx,my,delta);HeraTheme.label(c,"HERA / MINECRAFT 1.21.11",width-190,height-20,0xFFB0BBCB);
    }
    @Override public boolean shouldCloseOnEsc(){return false;}
}
