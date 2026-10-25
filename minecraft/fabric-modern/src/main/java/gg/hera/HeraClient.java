package gg.hera;

import gg.hera.hud.*;
import gg.hera.ui.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class HeraClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        HudConfig config = new HudConfig(FabricLoader.getInstance().getConfigDir().resolve("hera/hud.json"));
        config.load(); HudRenderer renderer = new HudRenderer(config);
        KeyBinding menu = KeyBindingHelper.registerKeyBinding(new KeyBinding("Hera Module", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "Hera"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> { while (menu.wasPressed()) client.setScreen(new ModuleScreen(config, renderer)); });
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            if (!(net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof HudEditorScreen)) renderer.render(ctx, false);
        });
    }
}
