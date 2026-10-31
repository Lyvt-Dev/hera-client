package gg.hera.hud;
public final class HudGeometry {
    private HudGeometry(){}
    public record Bounds(int x,int y,int width,int height,float scale){}
    public static Bounds layout(HudModule m,int logicalWidth,int logicalHeight,int screenWidth,int screenHeight){
        float requested=Float.isFinite(m.scale)&&m.scale>0?m.scale:1;
        float scale=Math.min(requested,Math.min(Math.max(1,screenWidth-8)/(float)Math.max(1,logicalWidth),Math.max(1,screenHeight-8)/(float)Math.max(1,logicalHeight)));
        int width=Math.max(1,Math.round(logicalWidth*scale)),height=Math.max(1,Math.round(logicalHeight*scale));
        return new Bounds(position(m.x,m.anchorX,screenWidth,width),position(m.y,m.anchorY,screenHeight,height),width,height,scale);
    }
    public static int position(float offset,int anchor,int screen,int size){double p=switch(anchor){case 1->screen-size-offset*screen;case 2->(screen-size)/2d+(offset-.5)*screen;default->offset*screen;};return (int)Math.clamp(Math.round(p),0,Math.max(0,screen-size));}
    public static float offset(double position,int anchor,int screen,int size){if(screen<=0)return 0;return (float)Math.clamp(switch(anchor){case 1->(screen-size-position)/screen;case 2->.5+(position-(screen-size)/2d)/screen;default->position/screen;},0,1);}
    public static int snap(int position,int size,int screen,int other,int otherSize){int[] targets={0,(screen-size)/2,screen-size,other,other+otherSize-size,other+(otherSize-size)/2};for(int n:targets)if(Math.abs(position-n)<=4)return Math.clamp(n,0,Math.max(0,screen-size));return Math.clamp(position,0,Math.max(0,screen-size));}
    public static int alpha(int color,float opacity){return (color&0xFFFFFF)|(Math.clamp(Math.round((color>>>24)*opacity),0,255)<<24);}
}
