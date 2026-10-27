package gg.hera.features;
import java.nio.ByteBuffer;
/** Validate PNG dimensions before native allocation. */
public final class CrosshairImage {
    public static final int MAX_BYTES=262144;
    private CrosshairImage(){}
    public static boolean valid(byte[] bytes){
        if(bytes==null||bytes.length<33||bytes.length>MAX_BYTES)return false;
        var b=ByteBuffer.wrap(bytes);
        if(b.getLong()!=0x89504E470D0A1A0AL||b.getInt()!=13||b.getInt()!=0x49484452)return false;
        int w=b.getInt(),h=b.getInt();return w>=1&&h>=1&&w<=256&&h<=256;
    }
}
