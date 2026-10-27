package gg.hera;
import gg.hera.hud.*;
import gg.hera.features.FeatureConfig;
import gg.hera.ui.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
/** Explicit development-only singleplayer smoke world. Never runs in a production build. */
final class HudWorldTest {
    static void register(HudConfig hud,HudRenderer renderer,FeatureConfig features){boolean[] started={false};int[] tick={0};String snapshot=hud.snapshot();
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.getOverlay()!=null)return;
            if(!started[0]){if(c.currentScreen instanceof TitleScreen||c.currentScreen instanceof HeraTitleScreen){started[0]=true;
                var data=net.minecraft.resource.DataConfiguration.SAFE_MODE;
                var info=new net.minecraft.world.level.LevelInfo("Hera HUD QA",net.minecraft.world.GameMode.CREATIVE,false,net.minecraft.world.Difficulty.NORMAL,true,new net.minecraft.world.rule.GameRules(data.enabledFeatures()),data);
                c.createIntegratedServerLoader().createAndStart("hera-hud-qa-"+System.currentTimeMillis(),info,net.minecraft.world.gen.GeneratorOptions.createTestWorld(),net.minecraft.world.gen.WorldPresets::createTestOptions,c.currentScreen);
            }return;}
            if(c.player==null||c.world==null)return;int n=++tick[0];
            if(n==20){c.setScreen(null);c.options.getGuiScale().setValue(2);c.getWindow().setWindowedSize(1280,720);hud.applyPreset("Hera Default");int i=0;for(var m:hud.modules){m.enabled=true;m.anchorX=0;m.anchorY=0;m.x=.02f+(i/8)*.38f;m.y=.10f+(i%8)*.08f;m.scale=1;if(m.id.equals("Effekte"))m.expanded=true;if(m.id.equals("FPS")){m.showFrameTime=true;m.showGraph=true;}i++;}renderer.invalidate();
                c.getNetworkHandler().sendChatCommand("item replace entity @s armor.head with minecraft:diamond_helmet");c.getNetworkHandler().sendChatCommand("effect give @s minecraft:speed 300 1");}
            if(n==40){int x=12,y=32,col=0;for(var m:hud.modules){int h=renderer.height(m);if(y+h>c.getWindow().getScaledHeight()-35){x+=205;y=32;col++;}m.x=x/(float)c.getWindow().getScaledWidth();m.y=y/(float)c.getWindow().getScaledHeight();y+=h+6;}HudTiming.enabled=true;}if(n==100){HudTiming.enabled=false;org.slf4j.LoggerFactory.getLogger("Hera").info("HERA_HUD_TIMING: {}",HudTiming.report());}if(n==100)shot(c,"hud-1280x720");if(n==110)c.setScreen(new HudEditorScreen(hud,renderer,features));if(n==125)shot(c,"editor-1280x720");
            if(n==140){c.setScreen(null);c.getWindow().setWindowedSize(1920,1080);}if(n==180)shot(c,"hud-1920x1080");if(n==190)c.setScreen(new HudEditorScreen(hud,renderer,features));if(n==210)shot(c,"editor-1920x1080");
            if(n==220){c.setScreen(null);c.options.getGuiScale().setValue(3);c.getWindow().setWindowedSize(2560,1440);}if(n==260)shot(c,"hud-2560x1440");if(n==270)c.setScreen(new HudEditorScreen(hud,renderer,features));if(n==290)shot(c,"editor-2560x1440");
            if(n==305){hud.restore(snapshot);renderer.invalidate();org.slf4j.LoggerFactory.getLogger("Hera").info("HERA_HUD_WORLD_OK: real singleplayer HUD/editor at three sizes; no multiplayer or D-Tap acceptance claimed");c.scheduleStop();}
        });
    }
    private static void shot(net.minecraft.client.MinecraftClient c,String name){net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory,"hera-0.9.0-"+name+".png",c.getFramebuffer(),1,t->{});}
}
