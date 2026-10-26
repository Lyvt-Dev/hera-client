package gg.hera.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public final class HudRenderer {
    private final HudConfig config;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Map<String, String> sampled = new HashMap<>();
    private long lastSample;
    public HudRenderer(HudConfig config) { this.config = config; }
    public String value(HudModule m, MinecraftClient c) {
        long now = System.nanoTime();
        if (now - lastSample >= 250_000_000L || sampled.isEmpty()) {
            for (HudModule module : config.modules) sampled.put(module.id, sample(module, c));
            lastSample = now;
        }
        return sampled.getOrDefault(m.id, m.id);
    }
    private String sample(HudModule m, MinecraftClient c) {
        if (c.player == null || c.world == null) return m.id + " · keine Welt";
        return switch (m.id) {
            case "FPS" -> c.getCurrentFps() + " FPS";
            case "Koordinaten" -> String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f", c.player.getX(), c.player.getY(), c.player.getZ());
            case "Ping" -> {
                PlayerListEntry entry = c.getNetworkHandler() == null ? null : c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
                yield entry == null ? "Ping · nicht verfügbar" : entry.getLatency() + " ms";
            }
            case "Speicher" -> "RAM " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576 + " / " + Runtime.getRuntime().maxMemory() / 1048576 + " MB";
            case "Uhr" -> LocalTime.now().format(CLOCK);
            case "Richtung" -> c.player.getHorizontalFacing().asString();
            case "Geschwindigkeit" -> String.format(Locale.ROOT, "%.2f Blöcke/s", c.player.getVelocity().horizontalLength() * 20);
            case "Biome" -> c.world.getBiome(c.player.getBlockPos()).getKey().map(key -> key.getValue().toString()).orElse("Biome · unbekannt");
            case "Server" -> c.getCurrentServerEntry() == null ? "Singleplayer" : c.getCurrentServerEntry().address;
            default -> m.id;
        };
    }
    public int width(HudModule module, MinecraftClient client) { return client.textRenderer.getWidth(value(module, client)) + 12; }
    public void render(DrawContext ctx, boolean editing) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null && !editing || c.options.hudHidden && !editing) return;
        for (HudModule m : config.modules) {
            if (!m.enabled && !editing) continue;
            m.normalize();
            int x = Math.min(Math.round(m.x * ctx.getScaledWindowWidth()), Math.max(0, ctx.getScaledWindowWidth() - Math.round(width(m, c) * m.scale)));
            int y = Math.min(Math.round(m.y * ctx.getScaledWindowHeight()), Math.max(0, ctx.getScaledWindowHeight() - Math.round(21 * m.scale)));
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(x, y); ctx.getMatrices().scale(m.scale, m.scale);
            ctx.fill(0, 0, width(m, c), 21, m.background);
            if (editing) {
                int w = width(m, c), color = m.enabled ? 0xFFF0A46E : 0xFF554D62;
                ctx.fill(0, 0, w, 1, color); ctx.fill(0, 20, w, 21, color);
                ctx.fill(0, 0, 1, 21, color); ctx.fill(w - 1, 0, w, 21, color);
            }
            ctx.drawText(c.textRenderer, value(m, c), 6, 6, m.enabled ? m.color : 0xFF787080, true);
            ctx.getMatrices().popMatrix();
        }
    }
}
