package gg.hera.cosmetics;
import java.nio.ByteBuffer;
import java.util.Arrays;
public final class CapeAtlas {
    private CapeAtlas(){}
    public static boolean dimensions(int w,int h){return w>=64&&w<=512&&(w&(w-1))==0&&h==w/2;}
    public static boolean boundedPng(byte[] bytes){
        if(bytes.length<33||bytes.length>1024*1024||!Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return false;
        var b=ByteBuffer.wrap(bytes);return b.getInt(8)==13&&b.getInt(12)==0x49484452&&dimensions(b.getInt(16),b.getInt(20));
    }
    public static boolean applies(String owner,String player,boolean ownPlayer){return ownPlayer&&owner!=null&&owner.matches("[a-fA-F0-9]{32}")&&owner.equalsIgnoreCase(player.replace("-",""));}
}
