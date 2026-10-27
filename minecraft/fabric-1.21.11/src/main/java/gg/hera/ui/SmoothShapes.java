package gg.hera.ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
/** Circle coverage is rasterized once per physical radius, then reused with linear sampling. */
final class SmoothShapes {
    private static final Identifier[] MASKS=new Identifier[97];
    private SmoothShapes(){}
    private static Identifier mask(int r){if(MASKS[r]!=null)return MASKS[r];int n=r*2;var image=new NativeImage(n,n,false);for(int y=0;y<n;y++)for(int x=0;x<n;x++){double distance=Math.hypot(x+.5-r,y+.5-r);int alpha=(int)Math.round(Math.clamp(r-distance+.5,0,1)*255);image.setColorArgb(x,y,alpha<<24|0xFFFFFF);}var id=Identifier.of("hera","dynamic/round_"+r);MinecraftClient.getInstance().getTextureManager().registerTexture(id,new SmoothTexture("Hera smooth corner",image));return MASKS[r]=id;}
    static void round(DrawContext c,int x,int y,int w,int h,int r,int color){if(w<=0||h<=0)return;r=Math.max(0,Math.min(r,Math.min(w,h)/2));if(r==0){c.fill(x,y,x+w,y+h,color);return;}
        var matrix=c.getMatrices();int physical=Math.clamp(Math.round((float)Math.hypot(matrix.m00(),matrix.m01())*MinecraftClient.getInstance().getWindow().getScaleFactor()*r),1,96);var id=mask(physical);
        c.fill(x+r,y,x+w-r,y+h,color);c.fill(x,y+r,x+r,y+h-r,color);c.fill(x+w-r,y+r,x+w,y+h-r,color);
        corner(c,id,x,y,r,physical,0,0,color);corner(c,id,x+w-r,y,r,physical,physical,0,color);corner(c,id,x,y+h-r,r,physical,0,physical,color);corner(c,id,x+w-r,y+h-r,r,physical,physical,physical,color);
    }
    private static void corner(DrawContext c,Identifier id,int x,int y,int r,int physical,int u,int v,int color){c.getMatrices().pushMatrix();c.getMatrices().translate(x,y);c.getMatrices().scale(r/(float)physical,r/(float)physical);c.drawTexture(RenderPipelines.GUI_TEXTURED,id,0,0,u,v,physical,physical,physical,physical,physical*2,physical*2,color);c.getMatrices().popMatrix();}
    static void clear(){for(int r=1;r<MASKS.length;r++)if(MASKS[r]!=null){MinecraftClient.getInstance().getTextureManager().destroyTexture(MASKS[r]);MASKS[r]=null;}}
}
