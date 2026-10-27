package gg.hera.features;
/** Ordinary visible camera orientation; no spoofed rotations or placement packets. */
public final class DTapAim {
    private DTapAim(){}
    public static float yaw(double x,double z){return (float)(Math.toDegrees(Math.atan2(z,x))-90);}
    public static float pitch(double x,double y,double z){return (float)-Math.toDegrees(Math.atan2(y,Math.hypot(x,z)));}
    public static float difference(float target,float current){float d=(target-current)%360;if(d>=180)d-=360;if(d< -180)d+=360;return d;}
    public static float step(float current,float target,float max){return current+Math.clamp(difference(target,current),-max,max);}
}
