package gg.hera.hud;

public final class HudModule {
    public String id;
    public boolean enabled = false;
    public float x;
    public float y;
    public float scale = 1;
    public int color = 0xFFE9EDF5;
    public int background = 0xA61A202B;
    public HudModule(String id, float x, float y) { this.id = id; this.x = x; this.y = y; }
    public void normalize() {
        x = Float.isFinite(x) ? Math.clamp(x, 0, 1) : 0.02f;
        y = Float.isFinite(y) ? Math.clamp(y, 0, 1) : 0.02f;
        scale = Float.isFinite(scale) ? Math.clamp(scale, 0.5f, 3) : 1;
    }
}
