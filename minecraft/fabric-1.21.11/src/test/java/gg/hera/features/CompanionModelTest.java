package gg.hera.features;
import com.google.gson.Gson;
import gg.hera.cosmetics.CompanionModel;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CompanionModelTest {
 private int cubes;
 private final Set<String> bones=new HashSet<>();
 private void visit(CompanionModel.Bone bone){
  assertTrue(bones.add(bone.id));cubes+=bone.cubes.length;
  for(var cube:bone.cubes)for(var quad:cube.quads){
   assertEquals(4,quad.vertices.length);
   for(var vertex:quad.vertices){assertEquals(5,vertex.length);for(float n:vertex)assertTrue(Float.isFinite(n));}
  }
  for(var child:bone.bones)visit(child);
 }
 @Test void allOwnerGeometryAndAnimationsArePackaged()throws Exception{
  try(var input=getClass().getResourceAsStream("/assets/hera/companion/model.json")){
   assertNotNull(input);
   var data=new Gson().fromJson(new InputStreamReader(input,StandardCharsets.UTF_8),CompanionModel.Data.class);
   for(var bone:data.bones)visit(bone);
   assertEquals(74,cubes);
   assertEquals(Set.of("idle","walk","run","swim","fly_hover","fly","fly_glide","hurt"),data.clips.keySet());
   for(var clip:data.clips.values()){
    assertTrue(clip.duration>0);assertFalse(clip.tracks.isEmpty());
    for(var entry:clip.tracks.entrySet()){
     assertTrue(bones.contains(entry.getKey()));
     for(var frames:entry.getValue().values()){
      assertEquals(Math.round(clip.duration*60)+1,frames.length);
      for(var frame:frames)for(float n:frame)assertTrue(Float.isFinite(n));
     }
    }
   }
  }
  assertNotNull(getClass().getResource("/assets/hera/companion/skin.png"));
  assertNotNull(getClass().getResource("/assets/hera/companion/glow.png"));
 }
}
