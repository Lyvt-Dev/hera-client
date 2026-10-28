package gg.hera.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WorkspaceLayoutTest {
    @Test void threePanesFitEverySupportedCanvas(){for(int[] size:new int[][]{{740,460},{960,540},{1280,720},{1920,1080}}){var l=WorkspaceLayout.of(size[0],size[1]);assertTrue(l.x()>=0);assertTrue(l.y()>=0);assertTrue(l.x()+l.width()<=size[0]);assertTrue(l.y()+l.height()<=size[1]);assertTrue(l.detailWidth()>=330);assertTrue(l.visibleRows()>=4);}}
    @Test void moduleRowsDoNotOverlapFooter(){var l=WorkspaceLayout.of(740,460);assertTrue(l.listY()+l.visibleRows()*60<=l.bottom());}
    @Test void listAndDetailHaveDistinctHitRegions(){var l=WorkspaceLayout.of(960,540);assertEquals(l.listX()+l.list(),l.detailX());assertEquals(l.x()+l.width(),l.detailX()+l.detailWidth());}
}
