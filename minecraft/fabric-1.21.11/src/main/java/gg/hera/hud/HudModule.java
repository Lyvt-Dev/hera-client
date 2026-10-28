package gg.hera.hud;
/** Per-profile appearance and anchor-relative placement. Existing schema-2 fields remain valid. */
public final class HudModule {
    public String id;public boolean enabled=false;public float x,y,scale=1,opacity=1;
    public int color=0xFFF4F2EF,background=0xD414171D,accent=0xFFF49B63;
    public int padding=6,radius=5,anchorX,anchorY,alignment,precision=1,cpsInterval=1;
    public boolean textShadow,mediaBrowser=true,useGlobalAccent=true;
    public boolean vanillaFont,vertical,expanded,showGraph,showFrameTime,latencyColor=true;
    public boolean showChunk,showBiome,showFacing,showDurability=true,showBars=true,showMouse=true,showSpace=true,showCps=true,showSprint=true;
    private HudModule(){useGlobalAccent=false;}
    public HudModule(String id,float x,float y){this.id=id;this.x=x;this.y=y;}
    public void normalize(){
        x=finite(x,0,1,.02f);y=finite(y,0,1,.02f);scale=finite(scale,.5f,3,1);opacity=finite(opacity,.1f,1,1);
        alignment=Math.clamp(alignment,0,2);padding=Math.clamp(padding,2,12);radius=Math.clamp(radius,0,8);anchorX=Math.clamp(anchorX,0,2);anchorY=Math.clamp(anchorY,0,2);precision=Math.clamp(precision,0,3);cpsInterval=Math.clamp(cpsInterval,1,5);
        color|=0xFF000000;accent|=0xFF000000;
    }
    private static float finite(float n,float min,float max,float fallback){return Float.isFinite(n)?Math.clamp(n,min,max):fallback;}
}
