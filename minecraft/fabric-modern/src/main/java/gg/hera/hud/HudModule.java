package gg.hera.hud;

public final class HudModule {
    public String id;
    public boolean enabled = true;
    public float x;
    public float y;
    public float scale = 1;
    public int color = 0xFFDAC8FF;
    public int background = 0xBB15111D;
    public HudModule(String id, float x, float y) { this.id = id; this.x = x; this.y = y; }
    public void normalize() {
        x = Float.isFinite(x) ? Math.clamp(x, 0, 1) : 0.02f;
        y = Float.isFinite(y) ? Math.clamp(y, 0, 1) : 0.02f;
        scale = Float.isFinite(scale) ? Math.clamp(scale, 0.5f, 3) : 1;
    }
}
