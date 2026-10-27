package gg.hera;
import gg.hera.features.*;
import gg.hera.hud.*;
import gg.hera.ui.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import java.util.*;
/** Explicit development-only launch check. Does not authenticate or enter a world. */
final class DevelopmentSmoke {
    private DevelopmentSmoke(){}
    static void register(HudConfig hud,HudRenderer renderer,FeatureConfig features){
        List<String> ids=new ArrayList<>(ModuleCatalog.IDS);ids.addAll(HudConfig.IDS);int[] frame={-1};
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.getOverlay()!=null)return;
            if(frame[0]<0){if(!(c.currentScreen instanceof HeraTitleScreen))return;
                try {for(String name:List.of("net.minecraft.client.network.ClientPlayNetworkHandler","net.minecraft.client.render.entity.feature.ArmorFeatureRenderer","net.minecraft.client.render.entity.PlayerEntityRenderer","net.minecraft.client.render.entity.ItemEntityRenderer","net.minecraft.client.render.item.ItemRenderState$LayerRenderState","net.minecraft.client.input.KeyboardInput","net.minecraft.entity.Entity","net.minecraft.client.gui.screen.ingame.HandledScreen"))Class.forName(name);}
                catch(ClassNotFoundException e){throw new IllegalStateException(e);}
                frame[0]=0;return;
            }
            frame[0]++;
            if(frame[0]==3)net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory,"hera-0.6.1-title.png",c.getFramebuffer(),1,t->{});
            if(frame[0]==12)c.setScreen(new ModuleScreen(hud,renderer,features));
            if(frame[0]==15)net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory,"hera-0.6.1-workspace.png",c.getFramebuffer(),1,t->{});
            if(frame[0]>=20 && frame[0]%4==0){int i=frame[0]/4-5;if(i<ids.size()){var workspace=new ModuleScreen(hud,renderer,features);c.setScreen(workspace);workspace.select(ids.get(i));}
                else if(i==ids.size())c.setScreen(new ItemSelectionScreen(new ModuleScreen(hud,renderer,features),features));
                else if(i==ids.size()+1)c.setScreen(new ColorPickerScreen(new ModuleScreen(hud,renderer,features),0xFFF0A46E,v->{}));
                else {org.slf4j.LoggerFactory.getLogger("Hera").info("HERA_SMOKE_OK: menu, {} module settings, item picker and color picker",ids.size());c.scheduleStop();}
            }
        });
    }
}
