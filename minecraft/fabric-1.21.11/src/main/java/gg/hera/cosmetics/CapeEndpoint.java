package gg.hera.cosmetics;
import java.net.URI;
public final class CapeEndpoint {
    private CapeEndpoint(){}
    public static String validate(String value){var uri=URI.create(value.strip());String host=uri.getHost();if(host==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath().isEmpty()||uri.getPath().equals("/")))throw new IllegalArgumentException("Invalid cape endpoint");
        boolean local=host.equals("localhost")||host.equals("[::1]")||host.equals("::1");
        if(host.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")){String[] parts=host.split("\\.");int a=Integer.parseInt(parts[0]),b=Integer.parseInt(parts[1]);for(String part:parts)if(Integer.parseInt(part)>255)throw new IllegalArgumentException();local=a==127||a==10||a==192&&b==168||a==172&&b>=16&&b<=31;}
        if(!uri.getScheme().equals("https")&&!(uri.getScheme().equals("http")&&local))throw new IllegalArgumentException("HTTPS or LAN required");return value.strip().replaceAll("/+$","");
    }
}
