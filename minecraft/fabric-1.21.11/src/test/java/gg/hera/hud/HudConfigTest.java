package gg.hera.hud;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class HudConfigTest {
 @TempDir Path directory;
 @Test void newProfilesHaveNoEnabledModules(){var c=new HudConfig(directory.resolve("hud.json"));assertEquals(11,c.modules.size());assertTrue(c.modules.stream().noneMatch(m->m.enabled));}
 @Test void legacyAutoEnabledSettingsAreDisabledButLayoutIsKept() throws Exception {
  var file=directory.resolve("hud.json");Files.writeString(file,"[{\"id\":\"FPS\",\"enabled\":true,\"x\":0.3,\"y\":0.4,\"scale\":1}]");
  var c=new HudConfig(file);c.load();assertTrue(c.modules.stream().noneMatch(m->m.enabled));assertEquals(.3f,c.modules.getFirst().x);assertEquals(.4f,c.modules.getFirst().y);
 }
 @Test void explicitNewOptInPersistsWithoutEnablingOtherModules() throws Exception {
  var file=directory.resolve("hud.json");var c=new HudConfig(file);c.modules.getFirst().enabled=true;c.save();
  var reloaded=new HudConfig(file);reloaded.load();assertTrue(reloaded.modules.getFirst().enabled);assertEquals(1,reloaded.modules.stream().filter(m->m.enabled).count());
 }
}
