package gg.hera.ui;
import gg.hera.features.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import java.nio.file.Path;
import java.util.List;
/** Native GLFW drag-and-drop: selected PNG is copied into this profile. */
public final class CrosshairScreen extends Screen {
    private final Screen parent;private final FeatureConfig config;private String status="PNG hier ins Fenster ziehen";private boolean importing;
    public CrosshairScreen(Screen parent,FeatureConfig config){super(HeraTheme.text("Custom Crosshair"));this.parent=parent;this.config=config;}
    @Override protected void init(){
        addDrawableChild(new HeraButton(width/2-130,height/2+54,126,24,"Fadenkreuz",false,()->config.options.crosshairImage=false));
        addDrawableChild(new HeraButton(width/2+4,height/2+54,126,24,"Eigenes PNG",false,()->{if(CustomCrosshair.hasImage())config.options.crosshairImage=true;else status="Ziehe zuerst ein PNG ins Fenster.";}));
        addDrawableChild(new HeraButton(width/2-55,height/2+86,110,24,"Fertig",true,this::close));
    }
    @Override public void onFilesDropped(List<Path> paths){
        if(importing)return;if(paths.size()!=1){status="Bitte genau ein PNG auswählen.";return;}importing=true;status="PNG wird importiert …";
        CustomCrosshair.importImage(paths.getFirst()).thenAccept(error->{importing=false;status=error.isEmpty()?"PNG importiert. Mit Fertig speichern.":error;if(error.isEmpty())config.options.crosshairImage=true;});
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xE510141C);int x=width/2,y=height/2;
        HeraTheme.title(c,"Dein Fadenkreuz",x-130,y-107,1.35f,HeraTheme.TEXT);
        HeraTheme.label(c,"Transparentes PNG · max. 256 × 256 px / 256 KiB",x-130,y-80,HeraTheme.MUTED);
        HeraTheme.panel(c,x-130,y-61,260,88);CustomCrosshair.draw(c,x,y-17,config.options);
        HeraTheme.label(c,HeraTheme.fit(status,260),x-130,y+36,HeraTheme.MUTED);super.render(c,mx,my,delta);
    }
    @Override public void close(){if(importing)return;try{config.save();client.setScreen(parent);}catch(java.io.IOException e){status="Speichern fehlgeschlagen.";}}
    @Override public boolean shouldPause(){return false;}
}
