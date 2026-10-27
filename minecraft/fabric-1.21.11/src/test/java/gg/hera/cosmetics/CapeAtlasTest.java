package gg.hera.cosmetics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CapeAtlasTest {
    @Test void supportsOnlyBoundedStandardCapeAtlases(){assertTrue(CapeAtlas.dimensions(64,32));assertTrue(CapeAtlas.dimensions(512,256));assertFalse(CapeAtlas.dimensions(64,64));assertFalse(CapeAtlas.dimensions(1024,512));assertFalse(CapeAtlas.dimensions(96,48));assertFalse(CapeAtlas.boundedPng(new byte[33]));}
    @Test void neverAppliesCapeToOtherPlayersOrAnotherAccount(){String owner="1234567890abcdef1234567890abcdef";assertTrue(CapeAtlas.applies(owner,"12345678-90ab-cdef-1234-567890abcdef",true));assertFalse(CapeAtlas.applies(owner,owner,false));assertFalse(CapeAtlas.applies(owner,"ffffffffffffffffffffffffffffffff",true));assertFalse(CapeAtlas.applies("invalid",owner,true));}
}
