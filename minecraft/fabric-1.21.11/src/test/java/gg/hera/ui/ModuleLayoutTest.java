package gg.hera.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ModuleLayoutTest {
    @Test void cardsAndFooterStayWithinCommonGuiScales(){
        for(int[] size:new int[][]{{320,240},{426,240},{640,360},{854,480},{1280,720}}){
            var l=ModuleLayout.of(size[0],size[1]);
            assertTrue(l.x()>=0&&l.y()>=0);assertTrue(l.x()+l.width()<=size[0]);assertTrue(l.y()+l.height()<=size[1]);
            assertTrue(l.cardWidth()>100);assertTrue(l.gridY()+(l.rows()-1)*(l.cardHeight()+8)+l.cardHeight()<=l.y()+l.height()-35);
        }
    }
    @Test void everyModuleCanBeReachedWithoutEmptyExtraPages(){var l=ModuleLayout.of(426,240);assertEquals(0,l.maxPage(0));assertEquals(0,l.maxPage(l.pageSize()));assertEquals(1,l.maxPage(l.pageSize()+1));assertTrue((l.maxPage(10)+1)*l.pageSize()>=10);}
}
