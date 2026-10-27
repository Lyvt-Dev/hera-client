package gg.hera.features;

/** Persisted, client-only module settings. No gameplay module starts enabled. */
public final class ModuleOptions {
    public boolean dTap,dTapFeedback=true,dTapCautious=true,dTapAutoAim=true;
    public int dTapDelay=2;public float dTapTarget=6,dTapSelf=10,dTapReserve=8;
    public boolean customCrosshair,ownNametag,nametagSneaking=true;
    public boolean crosshairDot=true,crosshairOutline=true,crosshairImage;
    public int crosshairColor=0xFFFFFFFF,crosshairLength=5,crosshairGap=3,crosshairThickness=1,crosshairSize=24,crosshairOpacity=100;
    public boolean hideOwnArmor;
    public boolean hideHelmet=true,hideChest=true,hideLegs=true,hideBoots=true;
    public boolean hitboxes,tntTimer,itemHighlighter,noFog,timeChanger,weatherChanger,zoom,freeLook,fullBright,freecam;
    public boolean espTracers,espBoxes=true,espGlow=true,espLabels=true,hitboxPlayers=true,hitboxMobs=true,hitboxItems;
    public boolean tracerCenter;
    public boolean fastItems3d=true,fastItemsSides,fastItemsShadows=true,armorDurability=true,keyMouse=true,keySpace=true;
    public int espColor=0xFFF0A46E,hitboxColor=0xFF79D8FF,tntColor=0xFFFFAF6E,highlightColor=0xFFFFD76B;
    public int lineWidth=1,hitboxWidth=1,worldTime=6000,weather=0,totemDelay=10;
    public float zoomFactor=4,freecamSpeed=0.5f,crystalRange=8;
    public float lookDistance=4,brightness=1;
    public boolean fogWater=true,fogLava=true;
    public static final int[] COLORS={0xFFF0A46E,0xFF79D8FF,0xFFB7F58A,0xFFC4A3FF,0xFFFF7D9C,0xFFFFFFFF};
    public void normalize(){
        dTapDelay=Math.clamp(dTapDelay,1,10);dTapTarget=finite(dTapTarget,2,30,6);dTapSelf=finite(dTapSelf,2,30,10);dTapReserve=finite(dTapReserve,2,20,8);
        crosshairLength=Math.clamp(crosshairLength,1,16);crosshairGap=Math.clamp(crosshairGap,0,12);
        crosshairThickness=Math.clamp(crosshairThickness,1,5);crosshairSize=Math.clamp(crosshairSize,8,96);crosshairOpacity=Math.clamp(crosshairOpacity,10,100);crosshairColor|=0xFF000000;
        lineWidth=Math.clamp(lineWidth,1,4);hitboxWidth=Math.clamp(hitboxWidth,1,4);
        worldTime=Math.floorMod(worldTime,24000);weather=Math.clamp(weather,0,2);totemDelay=Math.clamp(totemDelay,2,40);
        zoomFactor=finite(zoomFactor,2,12,4);freecamSpeed=finite(freecamSpeed,.1f,3,.5f);crystalRange=finite(crystalRange,2,16,8);
        lookDistance=finite(lookDistance,1,8,4);brightness=finite(brightness,.2f,1,1);
        espColor|=0xFF000000;hitboxColor|=0xFF000000;tntColor|=0xFF000000;highlightColor|=0xFF000000;
    }
    private static float finite(float v,float min,float max,float fallback){return Float.isFinite(v)?Math.clamp(v,min,max):fallback;}
    public static int nextColor(int value){for(int i=0;i<COLORS.length;i++)if(COLORS[i]==value)return COLORS[(i+1)%COLORS.length];return COLORS[0];}
}
