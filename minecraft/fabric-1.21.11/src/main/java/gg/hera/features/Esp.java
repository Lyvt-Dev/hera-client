package gg.hera.features;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;

public final class Esp {
    public static FeatureConfig config;
    private Esp() {}
    public static boolean marked(Entity entity) {
        var c=MinecraftClient.getInstance();var f=config;
        if(f==null || c.player==null || c.world==null || entity==c.player || entity.getEntityWorld()!=c.world || entity.isRemoved())return false;
        if(entity.squaredDistanceTo(c.player)>(double)f.espRange*f.espRange)return false;
        if(entity instanceof ItemEntity item)return f.itemEsp && ItemSelection.matches(f.markedItems,Registries.ITEM.getId(item.getStack().getItem()).toString());
        if(entity instanceof PlayerEntity)return f.playerEsp;
        return f.mobEsp && entity instanceof MobEntity;
    }
}
