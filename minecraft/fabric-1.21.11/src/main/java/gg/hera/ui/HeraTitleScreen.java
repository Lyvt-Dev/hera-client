package gg.hera.ui;
import gg.hera.features.FeatureConfig;
import gg.hera.hud.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.DrawContext;

/** Own Hera composition; uses the game's panorama without redistributing it. */
public final class HeraTitleScreen extends Screen {
    private final HudConfig hud;private final HudRenderer renderer;private final FeatureConfig features;
    private int left,top,buttonWidth,rowHeight;
    public HeraTitleScreen(HudConfig hud,HudRenderer renderer,FeatureConfig features){super(HeraTheme.text("Hera"));this.hud=hud;this.renderer=renderer;this.features=features;}
    @Override protected void init(){
        left=Math.max(18,width/22);buttonWidth=Math.min(218,Math.max(152,width/3));rowHeight=height<300?34:42;top=Math.max(86,(height-rowHeight*4)/2);
        addDrawableChild(action(top,"Einzelspieler","Deine Welten","Biome",()->client.setScreen(new SelectWorldScreen(this))));
        var multiplayer=action(top+rowHeight,"Mehrspieler","Gemeinsam spielen","Server",()->client.setScreen(new MultiplayerScreen(this)));multiplayer.active=client.isMultiplayerEnabled();addDrawableChild(multiplayer);
        addDrawableChild(action(top+rowHeight*2,"Dein Hera","Module & HUD gestalten","Koordinaten",()->client.setScreen(new ModuleScreen(hud,renderer,features))));
        addDrawableChild(action(top+rowHeight*3,"Einstellungen","Minecraft konfigurieren","Speicher",()->client.setScreen(new OptionsScreen(this,client.options))));
        addDrawableChild(new HeraButton(left,height-34,buttonWidth,23,"Spiel beenden",false,client::scheduleStop));
    }
    private HeraButton action(int y,String title,String subtitle,String icon,Runnable press){return new HeraButton(left,y,buttonWidth,rowHeight-5,title,false,press){
        @Override protected void renderWidget(DrawContext c,int mx,int my,float d){boolean hover=isHovered()||isFocused();HeraTheme.round(c,getX(),getY(),width,height,7,hover?0xEF263344:0xD5111925);HeraTheme.round(c,getX()+1,getY()+1,3,height-2,1,hover?HeraTheme.ACCENT:0x553A485C);HeraTheme.round(c,getX()+10,getY()+7,23,height-14,6,hover?0xFF604838:0xFF293647);HeraTheme.icon(c,icon,getX()+15,getY()+12,13,active?HeraTheme.ACCENT:HeraTheme.MUTED);HeraTheme.label(c,title,getX()+43,getY()+7,active?HeraTheme.TEXT:HeraTheme.MUTED);if(height>=35)HeraTheme.label(c,HeraTheme.fit(subtitle,width-57),getX()+43,getY()+23,HeraTheme.MUTED);}
    };}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        renderPanoramaBackground(c,delta);c.fillGradient(0,0,width,height,0x77404B62,0xE307101D);c.fill(0,0,Math.min(width,left+buttonWidth+32),height,0x50101927);
        HeraTheme.round(c,left,22,29,29,8,HeraTheme.ACCENT);HeraTheme.title(c,"H",left+8,29,1.35f,0xFF211A16);HeraTheme.title(c,"hera",left+39,23,2f,HeraTheme.TEXT);HeraTheme.label(c,"Deine Welt. Dein Setup.",left,62,HeraTheme.MUTED);
        String name=client.getSession().getUsername();int pw=Math.min(200,Math.max(140,Math.max(textRenderer.getWidth(HeraTheme.text(name)),textRenderer.getWidth(HeraTheme.text("Minecraft-Konto")))+48));HeraTheme.round(c,width-pw-18,20,pw,37,7,0xBB111925);HeraTheme.round(c,width-pw-8,29,17,17,5,0xFF394454);HeraTheme.label(c,HeraTheme.fit(name,pw-45),width-pw+18,27,HeraTheme.TEXT);HeraTheme.label(c,"Minecraft-Konto",width-pw+18,42,HeraTheme.MUTED);
        super.render(c,mx,my,delta);HeraTheme.label(c,"HERA / MINECRAFT 1.21.11",width-190,height-20,0xFFB0BBCB);
    }
    @Override public boolean shouldCloseOnEsc(){return false;}
}
