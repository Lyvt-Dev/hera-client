package gg.hera.features;
/** Monotonic, frame-rate-independent optional zoom easing. */
public final class ZoomTransition {
 private float amount;private long previous;private boolean initialized;
 public void reset(){amount=0;initialized=false;}
 public float sample(boolean engaged,boolean smooth,long now){
  if(!initialized){initialized=true;previous=now;}
  double seconds=Math.clamp((now-previous)/1_000_000_000d,0,.25);previous=now;float target=engaged?1:0;
  amount=smooth?(float)(amount+(target-amount)*(1-Math.exp(-20*seconds))):target;
  if(Math.abs(amount-target)<.001f)amount=target;
  return amount;
 }
}
