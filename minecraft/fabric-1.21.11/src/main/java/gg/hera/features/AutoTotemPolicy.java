package gg.hera.features;
public final class AutoTotemPolicy {
    private AutoTotemPolicy(){}
    public static int handlerSlot(int inventorySlot){
        if(inventorySlot<0||inventorySlot>=36)throw new IllegalArgumentException("Not a main inventory slot");
        return inventorySlot<9?36+inventorySlot:inventorySlot;
    }
}
