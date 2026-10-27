package gg.hera.fastitems;

import gg.hera.fastitems.mixin.ItemRenderStateAccessor;
import gg.hera.fastitems.mixin.LayerRenderStateAccessor;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.util.math.Direction;
import net.minecraft.item.ItemStack;

public final class FastItemsRenderer {
    private FastItemsRenderer() {}
    private static final boolean STANDALONE=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("fastitems");

    /**
     * Operates AFTER vanilla resolves components, model selectors, tint and foil.
     * Quad lists belong to this render state, not the shared baked resource model.
     * No hand, GUI, item-frame or item-display rendering is changed.
     */
    public static void prepare(ItemStack stack, ItemEntityRenderState state) {
        FastItemsState extension = (FastItemsState) state;
        extension.fastitems$setBillboard(false); // Render states can be reused: always reset.
        var config = gg.hera.features.Esp.config;
        if (config == null || !config.fastItems || STANDALONE || state.itemRenderState.isEmpty()) return;

        ItemRenderStateAccessor access = (ItemRenderStateAccessor) state.itemRenderState;
        int count = access.fastitems$activeLayerCount();
        ItemRenderState.LayerRenderState[] layers = access.fastitems$layers();
        if (count == 0) return;
        boolean flat = true;
        for (int i = 0; i < count; i++) {
            LayerRenderStateAccessor layer = (LayerRenderStateAccessor) layers[i];
            // Shields, chests, skulls and other special renderers keep vanilla behavior.
            // A single special layer also excludes the rest of a composite model.
            if (layer.fastitems$specialRenderer() != null || layers[i].getQuads().isEmpty()) return;
            if(flat && !(config.options.fastItems3d&&config.options.fastItemsSides))flat=isFlat(layers[i], layer);
        }
        if(!flat && !config.options.fastItems3d)return;
        extension.fastitems$setBillboard(true);
        if(!config.options.fastItemsShadows){state.shadowRadius=0;state.shadowPieces.clear();}
        if (!flat || config.options.fastItemsSides) return;

        // Keep vanilla extents for bobbing, stacking and bounding-box calculations.
        // Removing vertices must not shift the entity or collapse stack spacing.
        state.itemRenderState.getModelBoundingBox();
        for (int i = 0; i < count; i++) {
            layers[i].getQuads().removeIf(quad -> quad.face() != Direction.SOUTH);
        }
    }

    private static boolean isFlat(ItemRenderState.LayerRenderState layer, LayerRenderStateAccessor access) {
        var transform = access.fastitems$transform();
        // Rotated or mirrored custom ground transforms may expose the culled face.
        // Treat those as 3D instead of risking invisible geometry.
        if (access.fastitems$usesBlockLight() || transform.rotation().lengthSquared() > 0.000001f
                || transform.scale().x() <= 0 || transform.scale().y() <= 0 || transform.scale().z() <= 0) return false;
        float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        boolean front = false, back = false;
        for (BakedQuad quad : layer.getQuads()) {
            front |= quad.face() == Direction.SOUTH;
            back |= quad.face() == Direction.NORTH;
            for (int vertex = 0; vertex < 4; vertex++) {
                var p = quad.getPosition(vertex);
                if (!Float.isFinite(p.x()) || !Float.isFinite(p.y()) || !Float.isFinite(p.z())) return false;
                minZ = Math.min(minZ, p.z());
                maxZ = Math.max(maxZ, p.z());
            }
        }
        // Generated sprites have depth 1/16. A thin custom model is accepted only
        // when it has both front and back faces; arbitrary thick geometry is preserved.
        return front && back && maxZ - minZ <= 0.0626f;
    }
}
