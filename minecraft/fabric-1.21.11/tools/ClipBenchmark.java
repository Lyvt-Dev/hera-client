import gg.hera.features.OverlayPolicy;
import java.lang.management.ManagementFactory;
/** Isolated CPU/allocation probe, not an in-game FPS benchmark. */
public final class ClipBenchmark {
    private static volatile double sink;
    // Hera 0.6.1's own previous clipping implementation, retained for comparison.
    private static float[] before(float x,float y,float ex,float ey,int w,int h){
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(ex)||!Float.isFinite(ey)||w<=0||h<=0)return null;
        float dx=ex-x,dy=ey-y,t0=0,t1=1;float[] p={-dx,dx,-dy,dy},q={x,w-x,y,h-y};
        for(int i=0;i<4;i++){if(p[i]==0){if(q[i]<0)return null;continue;}float r=q[i]/p[i];if(p[i]<0)t0=Math.max(t0,r);else t1=Math.min(t1,r);if(t0>t1)return null;}
        return new float[]{x+t0*dx,y+t0*dy,x+t1*dx,y+t1*dy};
    }
    private static double run(boolean after,int n){float[] reuse=new float[4];double sum=0;
        for(int i=0;i<n;i++){int seed=i*1664525+1013904223;float x=(seed&2047)-512,y=((seed>>>11)&1023)-256,ex=((seed>>>5)&2047)-512,ey=((seed>>>17)&1023)-256;
            if(after){if(OverlayPolicy.clipInto(x,y,ex,ey,1280,720,reuse))sum+=reuse[0]+reuse[1]+reuse[2]+reuse[3];}
            else {float[] out=before(x,y,ex,ey,1280,720);if(out!=null)sum+=out[0]+out[1]+out[2]+out[3];}
        }sink=sum;return sum;
    }
    public static void main(String[] args){boolean after=args[0].equals("after");for(int i=0;i<5;i++)run(after,200_000);
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long id=Thread.currentThread().threadId();
        int n=1_000_000;long bytes=bean.getThreadAllocatedBytes(id),start=System.nanoTime();double sum=run(after,n);long elapsed=System.nanoTime()-start,allocated=bean.getThreadAllocatedBytes(id)-bytes;
        System.out.printf(java.util.Locale.ROOT,"%s: %.2f ns/segment, %.2f bytes/segment, checksum %.3f%n",args[0],elapsed/(double)n,allocated/(double)n,sum);
    }
}
