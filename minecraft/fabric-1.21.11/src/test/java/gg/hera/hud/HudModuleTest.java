package gg.hera.hud;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HudModuleTest {
 @Test void oldCustomAccentIsRetainedWhileNewModulesFollowHera() {
  var old=new com.google.gson.Gson().fromJson("{\"id\":\"FPS\",\"accent\":-16711936}",HudModule.class);
  assertFalse(old.useGlobalAccent);assertEquals(0xFF00FF00,old.accent);
  assertTrue(new HudModule("FPS",0,0).useGlobalAccent);
 }
 @Test void normalizesImportedCoordinates() { HudModule m = new HudModule("FPS", -3, 7); m.scale = Float.NaN; m.normalize(); assertEquals(0, m.x); assertEquals(1, m.y); assertEquals(1, m.scale); }
 @Test void limitsScaling() { HudModule m = new HudModule("FPS", .5f, .5f); m.scale = 100; m.normalize(); assertEquals(3, m.scale); }
}
