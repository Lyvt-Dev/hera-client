package gg.hera.cosmetics;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import java.nio.file.*;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/** Local-only cape; never changes another player or sends cosmetic packets. */
public final class LocalCape {
    private static final Identifier TEXTURE=Identifier.of("hera","dynamic/local_cape");
    private static AssetInfo.TextureAsset asset;
    private static java.util.UUID owner;private static String fingerprint="";
    private static SkinTextures cachedOriginal,cachedOverride;
    private static boolean loading;
    private static int ticks;
    private record Update(String fingerprint,String owner,byte[] png){}
    private LocalCape(){}
    public static void tick(MinecraftClient client){
        if(loading||ticks++%40!=0)return;
        loading=true;
        String previous=fingerprint;
        CompletableFuture.supplyAsync(()->read(previous)).whenComplete((update,error)->client.execute(()->{
            try {
                if(error!=null){clear(client);fingerprint="";return;}
                if(update==null)return;
                clear(client);fingerprint=update.fingerprint;
                if(update.png==null)return;
                NativeImage image=NativeImage.read(update.png);
                if(!CapeAtlas.dimensions(image.getWidth(),image.getHeight())){image.close();return;}
                var texture=new NativeImageBackedTexture(()->"Hera local cape",image);
                try {client.getTextureManager().registerTexture(TEXTURE,texture);}catch(RuntimeException e){texture.close();throw e;}
                asset=new AssetInfo.TextureAssetInfo(TEXTURE,TEXTURE);String id=update.owner;owner=java.util.UUID.fromString(id.substring(0,8)+"-"+id.substring(8,12)+"-"+id.substring(12,16)+"-"+id.substring(16,20)+"-"+id.substring(20));
            } catch(Exception ignored){clear(client);} finally {loading=false;}
        }));
    }
    private static Update read(String previous){
        try {
            Path file=FabricLoader.getInstance().getConfigDir().resolve("hera/cape.json");
            if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))return previous.isEmpty()?null:new Update("","",null);
            var metadata=Files.readAttributes(file,java.nio.file.attribute.BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
            String stamp=metadata.lastModifiedTime()+":"+metadata.size()+":"+metadata.fileKey();
            if(stamp.equals(previous))return null;
            if(metadata.size()>2*1024*1024)return new Update(stamp,"",null);
            byte[] content;try(var input=Files.newInputStream(file)){content=input.readNBytes(2*1024*1024+1);}
            if(content.length>2*1024*1024)return new Update(stamp,"",null);
            String text=new String(content,java.nio.charset.StandardCharsets.UTF_8);
            var json=JsonParser.parseString(text).getAsJsonObject();
            if(json.get("version").getAsInt()!=1||!json.get("enabled").getAsBoolean())return new Update(stamp,"",null);
            String owner=json.get("owner").getAsString();
            if(!owner.matches("[a-fA-F0-9]{32}"))return new Update(stamp,"",null);
            String encoded=json.get("pngBase64").getAsString();if(encoded.length()>1400000)return new Update(stamp,"",null);
            byte[] bytes=Base64.getDecoder().decode(encoded);
            return new Update(stamp,owner,CapeAtlas.boundedPng(bytes)?bytes:null);
        } catch(Exception ignored){return new Update("invalid","",null);}
    }
    private static void clear(MinecraftClient client){if(asset!=null)client.getTextureManager().destroyTexture(TEXTURE);asset=null;owner=null;cachedOriginal=null;cachedOverride=null;}
    public static SkinTextures apply(AbstractClientPlayerEntity player,SkinTextures original){
        var client=MinecraftClient.getInstance();
        if(asset==null||player!=client.player||!player.getUuid().equals(owner))return original;
        if(cachedOriginal!=original){cachedOriginal=original;cachedOverride=new SkinTextures(original.body(),asset,original.elytra(),original.model(),original.secure());}
        return cachedOverride;
    }
}
