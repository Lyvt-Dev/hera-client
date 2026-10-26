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
        FeatureConfig features = new FeatureConfig(FabricLoader.getInstance().getConfigDir().resolve("hera/features.json"));
        features.load(); CrystalTotem crystalTotem = new CrystalTotem(features);
        KeyBinding menu = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hera.modules", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.create(Identifier.of("hera", "client"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> { crystalTotem.tick(client); while (menu.wasPressed()) client.setScreen(new ModuleScreen(config, renderer, features)); });
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
            if (!(net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof HudEditorScreen)) renderer.render(ctx, false);
        });
    }
}
