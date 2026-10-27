package gg.hera.features;
/** Common fuse values share labels instead of formatting every rendered frame. */
public final class TntLabels {
    private static final String[] LABELS=new String[1001];
    static {for(int i=0;i<LABELS.length;i++)LABELS[i]=format(i);}
    private TntLabels(){}
    private static String format(int n){return "TNT · "+(n/10)+"."+(n%10)+" s";}
    public static String at(int fuse,float tick){int n=Math.max(0,Math.round((fuse-tick)*.5f));return n<LABELS.length?LABELS[n]:format(n);}
}
