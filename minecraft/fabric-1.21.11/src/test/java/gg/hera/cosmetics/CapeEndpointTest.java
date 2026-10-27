package gg.hera.cosmetics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CapeEndpointTest {
    @Test void acceptsLocalOriginsAndHttps(){for(String value:new String[]{"http://127.0.0.1:3847","http://192.168.178.20:3847","http://10.0.0.2:3847","http://localhost:3847","https://capes.example.com"})assertDoesNotThrow(()->CapeEndpoint.validate(value),value);}
    @Test void rejectsCredentialsPathsAndPublicPlaintext(){for(String value:new String[]{"http://example.com","http://169.254.169.254","http://localhost/admin","http://user:pass@localhost","file:///tmp/a","http://localhost?token=private"})assertThrows(IllegalArgumentException.class,()->CapeEndpoint.validate(value),value);}
}
