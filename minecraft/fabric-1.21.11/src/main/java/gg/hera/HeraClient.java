package gg.hera;

import gg.hera.hud.*;
import gg.hera.features.*;
import net.minecraft.util.Identifier;
import gg.hera.ui.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.TitleScreen;

public final class HeraClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        HudConfig config = new HudConfig(FabricLoader.getInstance().getConfigDir().resolve("hera/hud.json"));
        config.load(); HudRenderer renderer = new HudRenderer(config);
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener(){
            public Identifier getFabricId(){return Identifier.of("hera","render_caches");}
            public void reload(net.minecraft.resource.ResourceManager manager){renderer.invalidate();WorldOverlay.clearTextCache();}
        });
        FeatureConfig features = new FeatureConfig(FabricLoader.getInstance().getConfigDir().resolve("hera/features.json"));
        features.load(); CustomCrosshair.load(); CrystalTotem crystalTotem = new CrystalTotem(features);
        if(FabricLoader.getInstance().isDevelopmentEnvironment() && Boolean.getBoolean("hera.smokeTest"))DevelopmentSmoke.register(config,renderer,features);
        if(FabricLoader.getInstance().isDevelopmentEnvironment() && Boolean.getBoolean("hera.hudWorldTest"))HudWorldTest.register(config,renderer,features);
        Esp.config=features; AutoTotem autoTotem=new AutoTotem(features); ContainerMarkers containers=new ContainerMarkers(features);DTap dTap=new DTap(features);
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player,world,hand,entity,hit)->{if(world.isClient() && player==net.minecraft.client.MinecraftClient.getInstance().player && entity instanceof net.minecraft.entity.player.PlayerEntity target)dTap.hit(target);return net.minecraft.util.ActionResult.PASS;});
        var category=KeyBinding.Category.create(Identifier.of("hera", "client"));
        CameraController.zoomKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hera.zoom",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_C,category));
        CameraController.lookKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hera.freelook",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_LEFT_ALT,category));
        CameraController.camKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hera.freecam",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_V,category));
        KeyBinding menu = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hera.modules", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> { renderer.tick(client);Esp.tick();WorldOverlay.tick(client);gg.hera.cosmetics.LocalCape.tick(client);CameraController.tick(client);dTap.tick(client);if(!dTap.busy()){crystalTotem.tick(client);autoTotem.tick(client);}containers.tick(client); while (menu.wasPressed()) client.setScreen(new ModuleScreen(config, renderer, features)); });
        ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{if(screen instanceof TitleScreen && !client.isDemo())client.execute(()->{if(client.currentScreen==screen)client.setScreen(new HeraTitleScreen(config,renderer,features));});});
        // Explicit developer-only title-screen preview; never active in production or entering a world.
        if (FabricLoader.getInstance().isDevelopmentEnvironment() && Boolean.getBoolean("hera.previewUi")) {
            boolean[] opened = {false};
            ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
                if (!opened[0] && screen instanceof TitleScreen) {
                    opened[0] = true;
                    client.execute(() -> client.setScreen(new ModuleScreen(config, renderer, features)));
                }
            });
        }
        HudElementRegistry.addLast(Identifier.of("hera", "hud"), (ctx, tickCounter) -> {
            WorldOverlay.render(ctx);containers.render(ctx);ItemHighlights.render(ctx);
            if (!(net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof HudEditorScreen)) renderer.render(ctx, false);
        });
    }
}
