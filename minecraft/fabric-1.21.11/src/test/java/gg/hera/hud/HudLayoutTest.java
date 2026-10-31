package gg.hera.hud;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HudLayoutTest {
 @Test void allAnchorsKeepLargeModulesInsideSmallViewportsWithoutChangingStoredLayout(){
  for(int anchor=0;anchor<3;anchor++)for(int w:new int[]{320,640,1920})for(int h:new int[]{180,360,1080}){
   var m=new HudModule("Scoreboard",.95f,.95f);m.anchorX=m.anchorY=anchor;m.scale=3;
   var b=HudGeometry.layout(m,640,400,w,h);
   assertTrue(b.scale()>0&&b.scale()<=3);assertTrue(b.x()>=0&&b.y()>=0);assertTrue(b.x()+b.width()<=w);assertTrue(b.y()+b.height()<=h);
   assertEquals(3,m.scale);assertEquals(.95f,m.x);assertEquals(.95f,m.y);
  }
 }
 @Test void regularModulesRetainRequestedScale(){var m=new HudModule("FPS",.1f,.1f);m.scale=1.5f;var b=HudGeometry.layout(m,100,30,1280,720);assertEquals(1.5f,b.scale());assertEquals(150,b.width());assertEquals(45,b.height());}
 @Test void importedWidthIsBoundedAndOldConfigurationsReceiveDefault(){var m=new com.google.gson.Gson().fromJson("{\"id\":\"FPS\"}",HudModule.class);assertEquals(320,m.maxWidth);m.maxWidth=Integer.MAX_VALUE;m.normalize();assertEquals(640,m.maxWidth);}
}
