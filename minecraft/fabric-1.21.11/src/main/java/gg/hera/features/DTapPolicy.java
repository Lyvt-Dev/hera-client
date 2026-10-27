package gg.hera.features;
/** Conservative client estimates, never a promise of a pop or kill. */
public final class DTapPolicy {
    private DTapPolicy(){}
    public static int actionDelay(int configured,boolean cautious){return Math.max(cautious?4:1,Math.clamp(configured,1,10));}
    public static double requiredDamage(double health,double absorption,double minimum){return Double.isFinite(health)&&Double.isFinite(absorption)&&health>0&&absorption>=0?Math.max(minimum,health+absorption):Double.POSITIVE_INFINITY;}
    public static double rawDamage(double distance,double exposure){
        if(!Double.isFinite(distance)||!Double.isFinite(exposure)||distance<0||distance>=12)return 0;
        double impact=(1-distance/12)*Math.clamp(exposure,0,1);return Math.floor((impact*impact+impact)*42+1);
    }
    public static boolean suitable(double target,double self,double health,double minimum,double maximum,double reserve){return Double.isFinite(target)&&Double.isFinite(self)&&Double.isFinite(health)&&target>=minimum&&self<=maximum&&health-self>=reserve;}
}
