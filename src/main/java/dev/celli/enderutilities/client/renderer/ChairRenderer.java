package dev.celli.enderutilities.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.celli.enderutilities.entity.EntityChair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ChairRenderer extends EntityRenderer<EntityChair> {
    public ChairRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityChair entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    }

    @Override
    public ResourceLocation getTextureLocation(EntityChair entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
