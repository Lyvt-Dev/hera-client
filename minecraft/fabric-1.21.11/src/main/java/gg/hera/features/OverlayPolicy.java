package gg.hera.features;
/** Liang–Barsky screen clipping avoids huge or invalid GUI primitives. */
public final class OverlayPolicy {
    private OverlayPolicy(){}
    public static float[] clip(float x,float y,float ex,float ey,int w,int h){
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(ex)||!Float.isFinite(ey)||w<=0||h<=0)return null;
        float dx=ex-x,dy=ey-y,t0=0,t1=1;float[] p={-dx,dx,-dy,dy},q={x,w-x,y,h-y};
        for(int i=0;i<4;i++){if(p[i]==0){if(q[i]<0)return null;continue;}float r=q[i]/p[i];if(p[i]<0)t0=Math.max(t0,r);else t1=Math.min(t1,r);if(t0>t1)return null;}
        return new float[]{x+t0*dx,y+t0*dy,x+t1*dx,y+t1*dy};
    }
}
