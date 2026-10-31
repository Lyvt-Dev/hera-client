package gg.hera.features;
/** Local visual override policy. Does not mutate world/server properties. */
public final class WorldVisualPolicy {
 public enum Medium {AIR,WATER,LAVA,POWDER_SNOW}
 private WorldVisualPolicy(){}
 public static long time(long original,int time){long delta=Math.floorMod(time,24000)-Math.floorMod(original,24000L);if(delta>0&&original>Long.MAX_VALUE-delta)return Long.MAX_VALUE;if(delta<0&&original<Long.MIN_VALUE-delta)return Long.MIN_VALUE;return original+delta;}
 public static float brightness(float original,float requested){return Math.max(original,Math.clamp(requested,0,1));}
 public static float rain(int mode,float strength){return mode==0?0:Math.clamp(strength,0,1);}
 public static float thunder(int mode,float rain,float strength){return mode==2?Math.clamp(rain,0,1)*Math.clamp(strength,0,1):0;}
 public static boolean removeFog(ModuleOptions options,boolean clearLava,Medium medium){if(clearLava&&medium==Medium.LAVA)return true;if(!options.noFog)return false;return switch(medium){case AIR->options.fogAir;case WATER->options.fogWater;case LAVA->options.fogLava;case POWDER_SNOW->options.fogPowderSnow;};}
 public static String timeLabel(int time){int ticks=Math.floorMod(time,24000),hour=(ticks/1000+6)%24,minute=ticks%1000*60/1000;return (hour<10?"0":"")+hour+":"+(minute<10?"0":"")+minute;}
}
