package gg.hera.features;
/** Cross-clip transition state. Capture the last displayed pose only when changing an existing clip. */
public final class CompanionPoseTransition {
 private int session=-1,revision=-1;private boolean initial=true,blending,capture;
 public void reset(){session=revision=-1;initial=true;blending=capture=false;}
 public boolean capturePrevious(){return capture;}
 public float update(int nextSession,int nextRevision,int age,float partial,int ticks){
  capture=false;if(nextSession!=session){session=nextSession;revision=-1;initial=true;blending=false;}
  if(nextRevision!=revision){capture=!initial;blending=capture;revision=nextRevision;}
  float amount=blending?CompanionAnimationMath.transition(age,partial,ticks):1;if(amount>=1)blending=false;initial=false;return amount;
 }
}
