package gg.hera.ui;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
final class SmoothTexture extends NativeImageBackedTexture {
    SmoothTexture(String label,NativeImage image){super(()->label,image);sampler=RenderSystem.getSamplerCache().get(FilterMode.LINEAR);}
}
