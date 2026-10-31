package gg.hera.features;
/** Animation sampling and cross-clip blending without render or world dependencies. */
public final class CompanionAnimationMath {
 private CompanionAnimationMath(){}
 public static float sample(float[][] values,double seconds,int axis){double frame=Double.isFinite(seconds)?Math.clamp(seconds*60,0,values.length-1):0;int a=(int)frame,b=Math.min(a+1,values.length-1);return (float)(values[a][axis]+(values[b][axis]-values[a][axis])*(frame-a));}
 public static float blend(float from,float to,float amount){return from+(to-from)*Math.clamp(amount,0,1);}
 public static float angle(float from,float to,float amount){float difference=(to-from)%360;if(difference>180)difference-=360;if(difference< -180)difference+=360;return from+difference*Math.clamp(amount,0,1);}
 public static float transition(int age,float partial,int ticks){return ticks<=0?1:Math.clamp((age+partial)/ticks,0,1);}
}
