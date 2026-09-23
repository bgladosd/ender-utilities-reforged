package dev.celli.enderutilities.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.celli.enderutilities.blockentity.BarrelBlockEntity;
import dev.celli.enderutilities.util.EUStringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BarrelRenderer implements BlockEntityRenderer<BarrelBlockEntity> {
    public BarrelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BarrelBlockEntity barrel, float partialTick, PoseStack pose,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState camouflage = barrel.getCamouflage();
        if (camouflage != null) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(camouflage, pose, buffer, packedLight, packedOverlay);
        }
        ItemStack stored = barrel.getRenderStack();
        if (stored.isEmpty()) {
            return;
        }
        for (Direction side : Direction.values()) {
            if (!barrel.hasLabel(side)) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
            if (side.getAxis().isVertical()) {
                pose.mulPose(Axis.XP.rotationDegrees(side == Direction.UP ? -90 : 90));
            }
            pose.translate(0.0, 0.05, 0.501);
            pose.scale(0.4F, 0.4F, 0.02F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stored, ItemDisplayContext.FIXED,
                    packedLight, packedOverlay, pose, buffer, barrel.getLevel(), 0);
            pose.popPose();

            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
            if (side.getAxis().isVertical()) {
                pose.mulPose(Axis.XP.rotationDegrees(side == Direction.UP ? -90 : 90));
            }
            pose.translate(0.0, -0.22, 0.502);
            pose.scale(0.012F, -0.012F, 0.012F);
            String count = EUStringUtils.formatNumberWithKSeparators(stored.getCount());
            Font font = Minecraft.getInstance().font;
            float x = -font.width(count) / 2.0F;
            font.drawInBatch(count, x, 0, 0xFFFFFF, false, pose.last().pose(), buffer,
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }
}
