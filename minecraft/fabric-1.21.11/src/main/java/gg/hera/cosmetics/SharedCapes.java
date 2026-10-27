package gg.hera.cosmetics;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import java.nio.file.*;
import java.net.URI;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Public UUID cosmetics, bounded async fetch/decode and at most one texture upload per tick. */
public final class SharedCapes {
    private static final Map<UUID,Entry> entries=new LinkedHashMap<>();private static final ArrayDeque<Decoded> pending=new ArrayDeque<>();
    private static long lastContact;
    private static boolean busy,online;private static int ticks,cursor,epoch;private static Object world;private static String endpoint="";
    private static final class Entry {final String hash;final Identifier texture;final AssetInfo.TextureAsset asset;SkinTextures original,override;Entry(UUID id,String hash){this.hash=hash;texture=Identifier.of("hera","dynamic/shared_cape/"+id.toString().replace("-",""));asset=new AssetInfo.TextureAssetInfo(texture,texture);}}
    private record Decoded(UUID owner,String hash,NativeImage image){}
    private record Batch(String endpoint,List<UUID> ids,List<Decoded> capes){}
    private SharedCapes(){}
    public static void tick(MinecraftClient c){
        if(c.world!=world){world=c.world;epoch++;clear(c);}
        if(c.world==null||c.player==null)return;
        if(online&&System.nanoTime()-lastContact>10_000_000_000L)clear(c);
        if(!pending.isEmpty()){var item=pending.removeFirst();try{var entry=new Entry(item.owner,item.hash);var texture=new NativeImageBackedTexture(()->"Hera shared cape",item.image);try{c.getTextureManager().registerTexture(entry.texture,texture);}catch(RuntimeException e){texture.close();throw e;}entries.put(item.owner,entry);}catch(Exception ignored){item.image.close();}}
        if(busy||ticks++%40!=0)return;
        var visible=new ArrayList<UUID>(64);for(var player:c.world.getPlayers()){if(player!=c.player)visible.add(player.getUuid());if(visible.size()>=64)break;}
        var keep=new HashSet<>(visible);for(var iterator=entries.entrySet().iterator();iterator.hasNext();){var e=iterator.next();if(!keep.contains(e.getKey())){c.getTextureManager().destroyTexture(e.getValue().texture);iterator.remove();}}
        var ids=new ArrayList<UUID>(8);if(!visible.isEmpty()){cursor=Math.floorMod(cursor,visible.size());for(int i=0;i<Math.min(8,visible.size());i++)ids.add(visible.get((cursor+i)%visible.size()));cursor+=ids.size();}
        var hashes=new HashMap<UUID,String>();entries.forEach((id,e)->hashes.put(id,e.hash));String previous=endpoint;int generation=epoch;busy=true;
        CompletableFuture.supplyAsync(()->fetch(ids,hashes)).whenComplete((batch,error)->c.execute(()->{
            busy=false;if(generation!=epoch){if(batch!=null)batch.capes.forEach(d->{if(d.image!=null)d.image.close();});return;}
            if(error!=null||batch==null){clear(c);return;}
            if(!endpoint.equals(batch.endpoint))clear(c);endpoint=batch.endpoint;lastContact=System.nanoTime();
            online=!batch.endpoint.isEmpty();
            if(!online){clear(c);return;}
            for(UUID id:batch.ids){boolean present=batch.capes.stream().anyMatch(d->d.owner.equals(id));if(!present){var e=entries.remove(id);if(e!=null)c.getTextureManager().destroyTexture(e.texture);}}
            for(var item:batch.capes){if(item.image==null)continue;while(pending.size()>=8)pending.removeFirst().image.close();pending.addLast(item);}
        }));
    }
    private static Batch fetch(List<UUID> ids,Map<UUID,String> hashes){var decoded=new ArrayList<Decoded>();try{
        Path file=FabricLoader.getInstance().getConfigDir().resolve("hera/cape-service.json");if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))return new Batch("",ids,List.of());byte[] config;try(var in=Files.newInputStream(file)){config=in.readNBytes(2049);}if(config.length>2048)throw new IllegalArgumentException();var json=JsonParser.parseString(new String(config,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();if(json.get("version").getAsInt()!=1||!json.get("enabled").getAsBoolean())return new Batch("",ids,List.of());
        String base=CapeEndpoint.validate(json.get("baseUrl").getAsString());String query=ids.stream().map(id->id.toString().replace("-","")).collect(java.util.stream.Collectors.joining(","));
        URI uri=URI.create(base+(ids.isEmpty()?"/health":"/v1/capes?ids="+query+"&known="+ids.stream().filter(hashes::containsKey).map(id->id.toString().replace("-","")+":"+hashes.get(id)).collect(java.util.stream.Collectors.joining(","))));byte[] bytes=download(uri);var data=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();if(ids.isEmpty()){if(!data.get("service").getAsString().equals("hera-capes"))throw new IllegalArgumentException();return new Batch(base,ids,List.of());}
        if(data.get("version").getAsInt()!=1)throw new IllegalArgumentException();var capes=data.getAsJsonObject("capes");
        for(UUID id:ids){var value=capes.get(id.toString().replace("-",""));if(value==null||value.isJsonNull())continue;var cape=value.getAsJsonObject();String hash=cape.get("sha256").getAsString();if(!hash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException();if(hash.equals(hashes.get(id))){decoded.add(new Decoded(id,hash,null));continue;}String encoded=cape.get("pngBase64").getAsString();if(encoded.length()>1400000)throw new IllegalArgumentException();byte[] png=Base64.getDecoder().decode(encoded);if(!CapeAtlas.boundedPng(png)||!java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(png)).equals(hash))throw new IllegalArgumentException();NativeImage image=NativeImage.read(png);if(!CapeAtlas.dimensions(image.getWidth(),image.getHeight())){image.close();throw new IllegalArgumentException();}decoded.add(new Decoded(id,hash,image));}
        return new Batch(base,ids,decoded);
    }catch(Exception e){for(var item:decoded)if(item.image!=null)item.image.close();throw new java.util.concurrent.CompletionException(e);}}
    private static byte[] download(URI uri)throws Exception{
        var connection=(HttpURLConnection)uri.toURL().openConnection();connection.setConnectTimeout(3000);connection.setReadTimeout(2000);connection.setInstanceFollowRedirects(false);long start=System.nanoTime();
        try{if(connection.getResponseCode()!=200)throw new java.io.IOException();try(var in=connection.getInputStream();var out=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>6*1024*1024||System.nanoTime()-start>6_000_000_000L)throw new java.io.IOException();out.write(buffer,0,n);}return out.toByteArray();}}finally{connection.disconnect();}
    }
    private static void clear(MinecraftClient c){for(var entry:entries.values())c.getTextureManager().destroyTexture(entry.texture);entries.clear();while(!pending.isEmpty())pending.removeFirst().image.close();online=false;endpoint="";}
    public static SkinTextures apply(AbstractClientPlayerEntity player,SkinTextures original){var c=MinecraftClient.getInstance();if(!online||player==c.player)return original;var entry=entries.get(player.getUuid());if(entry==null)return original;if(entry.original!=original){entry.original=original;entry.override=new SkinTextures(original.body(),entry.asset,original.elytra(),original.model(),original.secure());}return entry.override;}
}
