package dev.celli.enderutilities.client.renderer;

import dev.celli.enderutilities.entity.EntityEndermanFighter;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.geom.ModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class EndermanFighterRenderer extends MobRenderer<EntityEndermanFighter, EndermanModel<EntityEndermanFighter>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman.png");

    public EndermanFighterRenderer(EntityRendererProvider.Context context) {
        super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
    }

    @Override
    public void render(EntityEndermanFighter entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model.creepy = entity.isScreaming();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityEndermanFighter entity) {
        return TEXTURE;
    }
}
