package gg.hera.features;
/** Liang–Barsky screen clipping avoids huge or invalid GUI primitives. */
public final class OverlayPolicy {
    private OverlayPolicy(){}
    public static float[] clip(float x,float y,float ex,float ey,int w,int h){
        float[] result=new float[4];return clipInto(x,y,ex,ey,w,h,result)?result:null;
    }
    public static boolean clipInto(float x,float y,float ex,float ey,int w,int h,float[] out){
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(ex)||!Float.isFinite(ey)||w<=0||h<=0)return false;
        float dx=ex-x,dy=ey-y,t0=0,t1=1;
        for(int i=0;i<4;i++){float p=switch(i){case 0->-dx;case 1->dx;case 2->-dy;default->dy;};float q=switch(i){case 0->x;case 1->w-x;case 2->y;default->h-y;};if(p==0){if(q<0)return false;continue;}float r=q/p;if(p<0)t0=Math.max(t0,r);else t1=Math.min(t1,r);if(t0>t1)return false;}
        out[0]=x+t0*dx;out[1]=y+t0*dy;out[2]=x+t1*dx;out[3]=y+t1*dy;return true;
    }
}
