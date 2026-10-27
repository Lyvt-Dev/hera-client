package gg.hera.ui;
import java.awt.*;
import java.awt.font.*;
import java.awt.image.BufferedImage;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
/** Cached high-resolution AA glyphs. No per-frame AWT calls, rasterization or uploads. */
public final class SmoothFont {
    private static final int[] SIZES={24,40,64};
    private static final Face[][] FACES=new Face[2][3];private static boolean failed;
    private static final class Face {final FontRasterizer.Glyph[] glyphs;final Identifier id;final int width,height;final float unit;Face(boolean bold,int size)throws Exception{unit=size/10f;
        id=Identifier.of("hera","dynamic/smooth_font_"+(bold?"bold":"medium")+"_"+size);
        Font font;try(var stream=MinecraftClient.getInstance().getResourceManager().getResource(Identifier.of("hera","font/nunito-"+(bold?"bold":"medium")+".ttf")).orElseThrow().getInputStream()){font=Font.createFont(Font.TRUETYPE_FONT,stream).deriveFont((float)size);}
        var raster=FontRasterizer.render(font);glyphs=raster.glyphs();var image=raster.image();width=image.getWidth();height=image.getHeight();var nativeImage=new NativeImage(width,height,false);for(int py=0;py<height;py++)for(int px=0;px<width;px++)nativeImage.setColorArgb(px,py,image.getRGB(px,py)|0xFFFFFF);image.flush();
        MinecraftClient.getInstance().getTextureManager().registerTexture(id,new SmoothTexture("Hera smooth font",nativeImage));
    }}
    private SmoothFont(){}
    private static Face face(boolean bold,int variant){if(failed)return null;int index=bold?1:0;try{if(FACES[index][variant]==null)FACES[index][variant]=new Face(bold,SIZES[variant]);return FACES[index][variant];}catch(Exception|LinkageError e){failed=true;org.slf4j.LoggerFactory.getLogger("Hera").warn("Smooth font unavailable; using Minecraft font renderer");return null;}}
    private static boolean supports(Face face,String s){if(face==null)return false;for(int i=0;i<s.length();){int c=s.codePointAt(i);if(c>=face.glyphs.length||face.glyphs[c]==null)return false;i+=Character.charCount(c);}return true;}
    public static int width(String s,boolean bold){var face=face(bold,1);if(!supports(face,s))return -1;float w=0;for(int i=0;i<s.length();){int c=s.codePointAt(i);w+=face.glyphs[c].advance;i+=Character.charCount(c);}return (int)Math.ceil(w/face.unit);}
    public static boolean draw(DrawContext c,String s,int x,int y,int color,boolean bold){double pixels=10*Math.hypot(c.getMatrices().m00(),c.getMatrices().m01())*MinecraftClient.getInstance().getWindow().getScaleFactor();var face=face(bold,pixels<=14?0:pixels<=24?1:2);if(!supports(face,s))return false;c.getMatrices().pushMatrix();c.getMatrices().translate(x,y+8);c.getMatrices().scale(1/face.unit,1/face.unit);float pen=0;
        for(int i=0;i<s.length();){int cp=s.codePointAt(i);var glyph=face.glyphs[cp];if(cp!=32)c.drawTexture(RenderPipelines.GUI_TEXTURED,face.id,Math.round(pen)+glyph.x,glyph.y,glyph.u,glyph.v,glyph.w,glyph.h,glyph.w,glyph.h,face.width,face.height,color);pen+=glyph.advance;i+=Character.charCount(cp);}c.getMatrices().popMatrix();return true;}
    public static void clear(){var c=MinecraftClient.getInstance();for(int i=0;i<FACES.length;i++)for(int j=0;j<FACES[i].length;j++){if(FACES[i][j]!=null)c.getTextureManager().destroyTexture(FACES[i][j].id);FACES[i][j]=null;}failed=false;}
}
