package gg.hera.features;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/** One cached texture, no file access or image decoding in the render loop. */
public final class CustomCrosshair {
    private static final Identifier ID=Identifier.of("hera","dynamic/crosshair");
    private static boolean ready;private static int imageWidth,imageHeight;
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("hera/crosshair.png");
    private CustomCrosshair(){}
    public static boolean hasImage(){return ready;}
    private static byte[] read(Path path)throws Exception{
        try(var input=Files.newInputStream(path)){byte[] bytes=input.readNBytes(CrosshairImage.MAX_BYTES+1);
            if(!CrosshairImage.valid(bytes))throw new IllegalArgumentException("PNG: max. 256 × 256 px und 256 KiB.");return bytes;}
    }
    public static CompletableFuture<String> saveCanvas(int[] pixels){
        var result=new CompletableFuture<String>();
        CompletableFuture.supplyAsync(()->{try{if(pixels.length!=1024)throw new IllegalArgumentException();var image=new java.awt.image.BufferedImage(32,32,java.awt.image.BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,32,32,pixels,0,32);var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",output);byte[] bytes=output.toByteArray();Files.createDirectories(FILE.getParent());var temporary=Files.createTempFile(FILE.getParent(),"crosshair-",".tmp");try{Files.write(temporary,bytes);Files.move(temporary,FILE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temporary);}return bytes;}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}}).whenComplete((bytes,error)->MinecraftClient.getInstance().execute(()->{try{if(error!=null)throw new IllegalStateException();upload(bytes);result.complete("");}catch(Exception e){result.complete("Fadenkreuz konnte nicht gespeichert werden");}}));return result;
    }
    public static void load(){
        CompletableFuture.supplyAsync(()->{try{return read(FILE);}catch(Exception e){return null;}})
            .thenAccept(bytes->{if(bytes!=null)MinecraftClient.getInstance().execute(()->{try{upload(bytes);}catch(Exception ignored){}});});
    }
    public static CompletableFuture<String> importImage(Path path){
        var result=new CompletableFuture<String>();
        CompletableFuture.supplyAsync(()->{try{return read(path);}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}})
            .whenComplete((bytes,error)->MinecraftClient.getInstance().execute(()->{
                if(error!=null){result.complete("PNG konnte nicht gelesen werden. Max. 256 × 256 px / 256 KiB.");return;}
                try(var image=NativeImage.read(bytes)){
                    if(image.getWidth()>256||image.getHeight()>256)throw new IllegalArgumentException();
                    Files.createDirectories(FILE.getParent());Path temporary=Files.createTempFile(FILE.getParent(),"crosshair-",".tmp");
                    try{Files.write(temporary,bytes);try{Files.move(temporary,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,FILE,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temporary);}
                    upload(bytes);result.complete("");
                }catch(Exception e){result.complete("PNG konnte nicht übernommen werden. Vorheriges Bild bleibt erhalten.");}
            }));return result;
    }
    private static void upload(byte[] bytes)throws Exception{
        NativeImage image=NativeImage.read(bytes);var texture=new NativeImageBackedTexture(()->"Hera custom crosshair",image);
        try{MinecraftClient.getInstance().getTextureManager().registerTexture(ID,texture);}catch(RuntimeException e){texture.close();throw e;}
        imageWidth=image.getWidth();imageHeight=image.getHeight();ready=true;
    }
    public static void draw(DrawContext c,int x,int y,ModuleOptions o){
        int alpha=Math.round(o.crosshairOpacity*2.55f),color=(o.crosshairColor&0xFFFFFF)|(alpha<<24);
        if(o.crosshairImage&&ready){int longest=Math.max(imageWidth,imageHeight),w=Math.max(1,Math.round(o.crosshairSize*(float)imageWidth/longest)),h=Math.max(1,Math.round(o.crosshairSize*(float)imageHeight/longest));
            c.drawTexture(RenderPipelines.GUI_TEXTURED,ID,x-w/2,y-h/2,0,0,w,h,imageWidth,imageHeight,imageWidth,imageHeight,(alpha<<24)|0xFFFFFF);return;}
        int t=o.crosshairThickness,g=o.crosshairGap,l=o.crosshairLength,a=t/2;
        rect(c,x-g-l,y-a,l,t,color,o.crosshairOutline,alpha);rect(c,x+g+1,y-a,l,t,color,o.crosshairOutline,alpha);
        rect(c,x-a,y-g-l,t,l,color,o.crosshairOutline,alpha);rect(c,x-a,y+g+1,t,l,color,o.crosshairOutline,alpha);
        if(o.crosshairDot)rect(c,x-a,y-a,t,t,color,o.crosshairOutline,alpha);
    }
    private static void rect(DrawContext c,int x,int y,int w,int h,int color,boolean outline,int alpha){
        if(outline)c.fill(x-1,y-1,x+w+1,y+h+1,alpha<<24);c.fill(x,y,x+w,y+h,color);
    }
}
