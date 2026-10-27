package gg.hera.hud;
/** Fixed storage, monotonic rolling input measurement. */
public final class ClickMeter {
    private final long[][] times={new long[1024],new long[1024]};private final int[] next=new int[2];
    public void record(int button,long now){if(button<0||button>1)return;times[button][next[button]++&1023]=now;}
    public int count(int button,long now,int seconds){int count=0;long cutoff=now-Math.clamp(seconds,1,5)*1_000_000_000L;for(long t:times[button])if(t>0&&t>cutoff&&t<=now)count++;return count;}
    public static final ClickMeter INPUT=new ClickMeter();
}
