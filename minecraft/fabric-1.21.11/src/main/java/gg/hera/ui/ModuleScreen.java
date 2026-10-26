package gg.hera.ui;

import gg.hera.hud.*;
import gg.hera.features.FeatureConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.io.IOException;

public final class ModuleScreen extends Screen {
    private final HudConfig config;
    private final HudRenderer renderer;
    private final FeatureConfig features;
    private String search = "";
    private TextFieldWidget field;
    private String error = "";
    private int scroll;
    public ModuleScreen(HudConfig config, HudRenderer renderer, FeatureConfig features) { super(Text.literal("Hera · Module")); this.config = config; this.renderer = renderer; this.features = features; }
    @Override protected void init() {
        int x = width / 2 - 130;
        field = new TextFieldWidget(textRenderer, x, 42, 260, 20, Text.literal("Module suchen"));
        field.setText(search); field.setPlaceholder(Text.literal("Module suchen …"));
        field.setChangedListener(value -> { search = value; scroll = 0; rebuildButtons(); });
        addDrawableChild(field); rebuildButtons();
    }
    private void rebuildButtons() {
        clearChildren(); addDrawableChild(field);
        int y = 76, matched = 0, shown = 0;
        int visible = Math.max(1, (height - 150) / 24);
        for (String id : java.util.stream.Stream.concat(java.util.stream.Stream.of("Kristall-Totem"), config.modules.stream().map(m -> m.id)).toList()) {
            HudModule m = config.modules.stream().filter(module -> module.id.equals(id)).findFirst().orElse(null);
            if (!id.toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))) continue;
            if (matched++ < scroll) continue;
            if (shown++ >= visible) break;
            addDrawableChild(ButtonWidget.builder(Text.literal(id + "  ·  " + ((m == null ? features.crystalTotem : m.enabled) ? "AN" : "AUS")), button -> {
                if (m == null) features.crystalTotem = !features.crystalTotem; else m.enabled = !m.enabled;
                button.setMessage(Text.literal(id + "  ·  " + ((m == null ? features.crystalTotem : m.enabled) ? "AN" : "AUS")));
            }).dimensions(width / 2 - 130, y, 260, 20).build());
            y += 24;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("HUD-Layout bearbeiten"), b -> client.setScreen(new HudEditorScreen(config, renderer, features))).dimensions(width / 2 - 130, height - 54, 260, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Speichern & schließen"), b -> close()).dimensions(width / 2 - 130, height - 29, 260, 20).build());
    }
    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xEE14161C);
        ctx.drawCenteredTextWithShadow(textRenderer, "HERA / MODULE", width / 2, 20, 0xFFF0A46E);
        super.render(ctx, mx, my, delta);
        if (!error.isEmpty()) ctx.drawCenteredTextWithShadow(textRenderer, error, width / 2, height - 68, 0xFFFF9DB6);
    }
    @Override public void close() { try { config.save(); features.save(); super.close(); } catch (IOException e) { error = "Konfiguration konnte nicht gespeichert werden."; } }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        long count = java.util.stream.Stream.concat(java.util.stream.Stream.of("Kristall-Totem"), config.modules.stream().map(m -> m.id)).filter(id -> id.toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))).count();
        int visible = Math.max(1, (height - 150) / 24);
        scroll = Math.clamp(scroll - (int) Math.signum(vertical), 0, Math.max(0, (int) count - visible));
        rebuildButtons(); return true;
    }
    @Override public boolean shouldPause() { return false; }
}
