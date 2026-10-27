package gg.hera.features;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;

public final class Esp {
    public static FeatureConfig config;
    private static final ModuleOptions DEFAULTS=new ModuleOptions();
    private static FeatureConfig selectedConfig;
    private static java.util.Set<String> selection=java.util.Set.of();
    private static final java.util.Set<net.minecraft.item.Item> selected=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private Esp() {}
    public static ModuleOptions options(){return config==null?DEFAULTS:config.options;}
    public static void tick(){var f=config;if(f==null)return;if(selectedConfig==f&&selection.equals(f.markedItems))return;selectedConfig=f;selection=java.util.Set.copyOf(f.markedItems);selected.clear();for(String name:selection){var id=net.minecraft.util.Identifier.tryParse(name);if(id==null)continue;var item=Registries.ITEM.get(id);if(Registries.ITEM.getId(item).equals(id))selected.add(item);}}
    public static boolean selected(net.minecraft.item.Item item){return selected.contains(item);}
    public static boolean glowing(Entity entity){return config!=null&&config.options.espGlow&&WorldOverlay.cached(entity)&&marked(entity);}
    public static boolean marked(Entity entity) {
        var f=config;if(f==null)return false;
        if(entity instanceof ItemEntity item){if(!f.itemEsp||!selected(item.getStack().getItem()))return false;}
        else if(entity instanceof PlayerEntity){if(!f.playerEsp)return false;}
        else if(!(f.mobEsp&&entity instanceof MobEntity))return false;
        var c=MinecraftClient.getInstance();
        if(c.player==null || c.world==null || entity==c.player || entity.getEntityWorld()!=c.world || entity.isRemoved())return false;
        if(entity.squaredDistanceTo(c.player)>(double)f.espRange*f.espRange)return false;
        return true;
    }
}
