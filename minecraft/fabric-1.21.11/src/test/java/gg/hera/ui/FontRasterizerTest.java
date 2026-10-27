package gg.hera.ui;
import org.junit.jupiter.api.Test;
import java.awt.Font;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class FontRasterizerTest {
    @Test void bundledFacesProduceAntialiasedGlyphsWithinAtlas()throws Exception{for(String face:new String[]{"medium","bold"})for(float size:new float[]{24,40,64}){Font font;try(var stream=Files.newInputStream(Path.of("src/main/resources/assets/hera/font/nunito-"+face+".ttf"))){font=Font.createFont(Font.TRUETYPE_FONT,stream).deriveFont(size);}var raster=FontRasterizer.render(font);assertNotNull(raster.glyphs()['A']);assertNotNull(raster.glyphs()['ä']);int partial=0;for(var glyph:raster.glyphs())if(glyph!=null){assertTrue(glyph.u>=0&&glyph.v>=0&&glyph.u+glyph.w<=raster.image().getWidth()&&glyph.v+glyph.h<=raster.image().getHeight());assertTrue(glyph.advance>=0);for(int y=glyph.v;y<glyph.v+glyph.h;y++)for(int x=glyph.u;x<glyph.u+glyph.w;x++){int a=raster.image().getRGB(x,y)>>>24;if(a>0&&a<255)partial++;}}assertTrue(partial>100,"AA coverage must contain intermediate alpha values");raster.image().flush();}}
}
