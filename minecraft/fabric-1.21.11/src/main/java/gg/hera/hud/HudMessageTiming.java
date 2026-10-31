package gg.hera.hud;
/** Vanilla-duration fade curves, independent of render rate. */
public final class HudMessageTiming {
 private HudMessageTiming(){}
 public static float title(int remaining,int fadeIn,int stay,int fadeOut,float partial){
  if(remaining<=0)return 0;fadeIn=Math.max(0,fadeIn);stay=Math.max(0,stay);fadeOut=Math.max(0,fadeOut);
  float left=Math.max(0,remaining-Math.clamp(partial,0,1));float elapsed=(long)fadeIn+stay+fadeOut-left;
  if(fadeIn>0&&elapsed<fadeIn)return Math.clamp(elapsed/fadeIn,0,1);
  return fadeOut>0&&left<fadeOut?Math.clamp(left/fadeOut,0,1):1;
 }
 public static float action(int remaining,float partial){return Math.clamp((remaining-Math.clamp(partial,0,1))/20f,0,1);}
}
