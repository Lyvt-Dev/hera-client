package gg.hera.mixin;
import gg.hera.features.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.joml.Matrix4f;
import java.nio.file.Path;
import java.lang.reflect.*;
import static org.junit.jupiter.api.Assertions.*;
class ItemModelScopeTest {
 @TempDir Path dir;
 private static final class Harness extends ItemModelMixin{}
 private Method wrapper()throws Exception{var method=ItemModelMixin.class.getDeclaredMethod("hera$model",LivingEntity.class,ItemStack.class,ItemDisplayContext.class,MatrixStack.class,OrderedRenderCommandQueue.class,int.class,Operation.class);method.setAccessible(true);return method;}
 private void invoke(ItemDisplayContext context,MatrixStack matrices,Operation<Void> operation)throws Exception{wrapper().invoke(new Harness(),null,null,context,matrices,null,0,operation);}
 @Test void mirroredTransformsAreScopedAcrossOriginalCancellation()throws Exception{
  var previous=Esp.config;try{var f=new FeatureConfig(dir.resolve("test.json"));Esp.config=f;f.options.presentation.put("Item Model",true);f.options.itemScale=2;f.options.itemX=.25f;f.options.itemMirrorLeft=true;var matrices=new MatrixStack();var before=new Matrix4f(matrices.peek().getPositionMatrix());int[] calls={0};invoke(ItemDisplayContext.FIRST_PERSON_LEFT_HAND,matrices,args->{calls[0]++;assertEquals(2,matrices.peek().getPositionMatrix().m00());assertEquals(-.25f,matrices.peek().getPositionMatrix().m30());return null;});assertEquals(1,calls[0]);assertEquals(before,matrices.peek().getPositionMatrix());assertTrue(matrices.isEmpty());}finally{Esp.config=previous;}
 }
 @Test void renderFailureRestoresTheCallerMatrix()throws Exception{
  var previous=Esp.config;try{var f=new FeatureConfig(dir.resolve("test.json"));Esp.config=f;f.options.presentation.put("Item Model",true);f.options.itemZ=.4f;var matrices=new MatrixStack();var before=new Matrix4f(matrices.peek().getPositionMatrix());var failure=assertThrows(InvocationTargetException.class,()->invoke(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,matrices,args->{throw new IllegalStateException("simulated other renderer failure");}));assertInstanceOf(IllegalStateException.class,failure.getCause());assertEquals(before,matrices.peek().getPositionMatrix());assertTrue(matrices.isEmpty());}finally{Esp.config=previous;}
 }
}
