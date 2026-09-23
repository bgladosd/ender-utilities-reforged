package dev.celli.enderutilities.client.renderer;

import dev.celli.enderutilities.entity.EntityEnderArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class EnderArrowRenderer extends ArrowRenderer<EntityEnderArrow> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public EnderArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityEnderArrow entity) {
        return TEXTURE;
    }
}
