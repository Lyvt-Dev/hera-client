package gg.hera.hud;
/** Explicit development measurement of HUD command submission, not GPU frame time. */
public final class HudTiming {
    public static boolean enabled;private static final long[] samples=new long[8192];private static int size;
    public static void record(long nanos){if(size<samples.length)samples[size++]=nanos;}
    public static String report(){if(size==0)return "no samples";long[] sorted=java.util.Arrays.copyOf(samples,size);java.util.Arrays.sort(sorted);return String.format(java.util.Locale.ROOT,"%d samples, median %.3f ms, p95 %.3f ms (CPU command submission only)",size,sorted[size/2]/1_000_000d,sorted[(size-1)*95/100]/1_000_000d);}
    private HudTiming(){}
}
