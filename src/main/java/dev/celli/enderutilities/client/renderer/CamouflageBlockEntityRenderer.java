package dev.celli.enderutilities.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.celli.enderutilities.blockentity.CamouflageHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CamouflageBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    public CamouflageBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, int packedOverlay) {
        if (!(blockEntity instanceof CamouflageHolder holder)) {
            return;
        }
        BlockState camouflage = holder.getCamouflage();
        if (camouflage == null) {
            return;
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(camouflage, poseStack, buffer, packedLight, packedOverlay);
    }
}
