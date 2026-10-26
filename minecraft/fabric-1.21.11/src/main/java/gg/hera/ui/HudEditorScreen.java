package gg.hera.ui;

import gg.hera.hud.*;
import gg.hera.features.FeatureConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.io.IOException;

public final class HudEditorScreen extends Screen {
    private final HudConfig config;
    private final HudRenderer renderer;
    private final FeatureConfig features;
    private HudModule selected;
    private double offsetX, offsetY;
    private boolean dragging;
    private String error = "";
    public HudEditorScreen(HudConfig config, HudRenderer renderer, FeatureConfig features) { super(Text.literal("Hera · HUD Editor")); this.config = config; this.renderer = renderer; this.features = features; }
    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x66100B18);
        for (int x = 0; x < width; x += 8) ctx.fill(x, 0, x + 1, height, 0x118F6EC0);
        for (int y = 0; y < height; y += 8) ctx.fill(0, y, width, y + 1, 0x118F6EC0);
        renderer.render(ctx, true);
        ctx.drawCenteredTextWithShadow(textRenderer, "Ziehen · Shift: 8px-Raster · Mausrad: Größe · Rechtsklick: An/Aus", width / 2, height - 35, 0xFFE1D2F6);
        ctx.drawCenteredTextWithShadow(textRenderer, "C: Farbe · B: Hintergrund · R: Layout zurücksetzen · Esc: Speichern", width / 2, height - 22, 0xFFAD9ABB);
        if (!error.isEmpty()) ctx.drawCenteredTextWithShadow(textRenderer, error, width / 2, 15, 0xFFFF9DB6);
    }
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double x = click.x(), y = click.y(); int button = click.button();
        for (int i = config.modules.size() - 1; i >= 0; i--) {
            HudModule m = config.modules.get(i);
            double px = Math.min(m.x * width, Math.max(0, width - renderer.width(m, client) * m.scale));
            double py = Math.min(m.y * height, Math.max(0, height - 21 * m.scale));
            if (x >= px && x <= px + renderer.width(m, client) * m.scale && y >= py && y <= py + 21 * m.scale) {
                selected = m; offsetX = x - px; offsetY = y - py;
                if (button == 1) m.enabled = !m.enabled;
                dragging = button == 0; return true;
            }
        }
        selected = null; return false;
    }
    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        double x = click.x(), y = click.y();
        if (selected == null || !dragging) return false;
        double px = x - offsetX, py = y - offsetY;
        if (click.hasShift()) { px = Math.round(px / 8) * 8; py = Math.round(py / 8) * 8; }
        selected.x = (float) Math.clamp(px / width, 0, 1); selected.y = (float) Math.clamp(py / height, 0, 1); return true;
    }
    @Override public boolean mouseReleased(Click click) { dragging = false; return true; }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (selected == null) return false;
        selected.scale = Math.clamp(selected.scale + (float) vertical * .1f, .5f, 3); return true;
    }
    @Override public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_R) { config.reset(); selected = null; return true; }
        if (selected != null && key == GLFW.GLFW_KEY_C) { selected.color = selected.color == 0xFFF0A46E ? 0xFF9AE8E2 : 0xFFF0A46E; return true; }
        if (selected != null && key == GLFW.GLFW_KEY_B) { selected.background = selected.background == 0 ? 0xBB1B1E26 : 0; return true; }
        return super.keyPressed(input);
    }
    @Override public void close() { try { config.save(); client.setScreen(new ModuleScreen(config, renderer, features)); } catch (IOException e) { error = "Layout konnte nicht gespeichert werden."; } }
    @Override public boolean shouldPause() { return false; }
}
