package gg.hera.hud;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonParser;
/** Local typed-native snapshot. Never connects to Spotify or reads account tokens. */
public final class MediaState {
    public record Track(boolean available,String title,String artist,String source,boolean playing,long position,long duration,long updated){}
    private static final Track EMPTY=new Track(false,"Keine Wiedergabe","Spotify / Browser","",false,0,0,0);private static volatile Track current=EMPTY;private static volatile boolean busy;private static int ticks;
    private MediaState(){}
    public static Track current(){var value=current;return System.currentTimeMillis()-value.updated>5000?EMPTY:value;}
    public static long position(Track value){return positionAt(value,System.currentTimeMillis());}
    public static long positionAt(Track value,long now){long duration=Math.max(0,value.duration),base=Math.clamp(value.position,0,duration),extra=value.playing?Math.max(0,now-value.updated):0;return base+Math.min(duration-base,extra);}
    public static void tick(HudConfig config){if(busy||ticks++%20!=0||config.modules.stream().noneMatch(m->m.enabled&&m.id.equals("Spotify")))return;busy=true;
        CompletableFuture.supplyAsync(()->{try{Path file=FabricLoader.getInstance().getConfigDir().resolve("hera/media.json");if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))return EMPTY;byte[] bytes;try(var in=Files.newInputStream(file)){bytes=in.readNBytes(8193);}if(bytes.length>8192)return EMPTY;var j=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();if(j.get("version").getAsInt()!=1||!j.get("available").getAsBoolean())return EMPTY;return new Track(true,j.get("title").getAsString(),j.get("artist").getAsString(),j.get("source").getAsString(),j.get("playing").getAsBoolean(),j.get("positionMs").getAsLong(),j.get("durationMs").getAsLong(),j.get("updatedAtMs").getAsLong());}catch(Exception e){return EMPTY;}}).whenComplete((value,error)->{current=error==null?value:EMPTY;busy=false;});
    }
}
