package gg.hera.ui;
import java.awt.*;
import java.awt.font.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
/** Pure CPU atlas construction, isolated for headless quality/bounds tests. */
public final class FontRasterizer {
    public static final class Glyph {final int u,v,w,h,x,y;final float advance;Glyph(int u,int v,int w,int h,int x,int y,float advance){this.u=u;this.v=v;this.w=w;this.h=h;this.x=x;this.y=y;this.advance=advance;}}
    public record Raster(BufferedImage image,Glyph[] glyphs){}
    private FontRasterizer(){}
    public static Raster render(Font font){int width=font.getSize2D()<=40?512:1024,height=font.getSize2D()<=32?512:font.getSize2D()<=64?1024:2048;var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();var glyphs=new Glyph[0x2800];
        try{g.setFont(font);g.setColor(Color.WHITE);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);FontRenderContext context=g.getFontRenderContext();
            var codes=new LinkedHashSet<Integer>();for(int c=32;c<=255;c++)codes.add(c);"—−…∞×→↑↓‹›✓♫".codePoints().forEach(codes::add);int x=1,y=1,row=0;
            for(int code:codes){if(!font.canDisplay(code))continue;String str=new String(Character.toChars(code));var vector=font.createGlyphVector(context,str);var bounds=vector.getPixelBounds(context,0,0);float advance=vector.getGlyphMetrics(0).getAdvanceX();int w=bounds.width+4,h=bounds.height+4;if(x+w>=width){x=1;y+=row+2;row=0;}if(y+h>=height)throw new IllegalStateException("Font atlas full");g.drawGlyphVector(vector,x+2-bounds.x,y+2-bounds.y);glyphs[code]=new Glyph(x,y,w,h,bounds.x-2,bounds.y-2,advance);x+=w+2;row=Math.max(row,h);}
            return new Raster(image,glyphs);
        }finally{g.dispose();}
    }
}
