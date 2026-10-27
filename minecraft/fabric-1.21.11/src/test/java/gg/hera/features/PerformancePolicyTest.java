package gg.hera.features;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class PerformancePolicyTest {
    @Test void nearestSubsetMatchesFullSortForLargeUnorderedInput(){
        var random=new Random(472);var values=new ArrayList<Integer>();for(int i=0;i<5000;i++)values.add(random.nextInt(1_000_000));
        var nearest=new NearestTargets<Integer>(128,Integer::doubleValue,Integer::intValue);values.forEach(nearest::offer);
        assertEquals(values.stream().sorted().limit(128).toList(),nearest.snapshot());nearest.clear();assertTrue(nearest.snapshot().isEmpty());
    }
    @Test void budgetStopsAtBothWorkAndTimeLimits(){
        long[] time={0};var work=new WorkBudget(4,100,()->time[0]);for(int i=0;i<4;i++)assertTrue(work.step());assertFalse(work.step());
        var timed=new WorkBudget(4096,100,()->time[0]);assertTrue(timed.step());time[0]=100;assertFalse(timed.step());
    }
    @Test void reusableClippingMatchesReferenceAcrossViewportCases(){
        var r=new Random(128);float[] out=new float[4];
        for(int i=0;i<10000;i++){float x=r.nextFloat()*1000-400,y=r.nextFloat()*1000-400,ex=r.nextFloat()*1000-400,ey=r.nextFloat()*1000-400;float[] expected=reference(x,y,ex,ey,320,240);boolean actual=OverlayPolicy.clipInto(x,y,ex,ey,320,240,out);assertEquals(expected!=null,actual);if(actual)assertArrayEquals(expected,out,.001f);}
    }
    private float[] reference(float x,float y,float ex,float ey,int w,int h){float dx=ex-x,dy=ey-y,t0=0,t1=1;float[] p={-dx,dx,-dy,dy},q={x,w-x,y,h-y};for(int i=0;i<4;i++){if(p[i]==0){if(q[i]<0)return null;continue;}float r=q[i]/p[i];if(p[i]<0)t0=Math.max(t0,r);else t1=Math.min(t1,r);if(t0>t1)return null;}return new float[]{x+t0*dx,y+t0*dy,x+t1*dx,y+t1*dy};}
    @Test void commonTntLabelsAreReusedAndNeverNegative(){assertSame(TntLabels.at(80,0),TntLabels.at(80,0));assertEquals("TNT · 4.0 s",TntLabels.at(80,0));assertEquals("TNT · 0.0 s",TntLabels.at(-1,0));}
}
