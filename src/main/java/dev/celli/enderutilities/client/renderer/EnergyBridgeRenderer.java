package dev.celli.enderutilities.client.renderer;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.celli.enderutilities.blockentity.EnergyBridgeBlockEntity;
import dev.celli.enderutilities.blockentity.EnergyBridgeBlockEntity.Type;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EnergyBridgeRenderer implements BlockEntityRenderer<EnergyBridgeBlockEntity> {

    public EnergyBridgeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EnergyBridgeBlockEntity teeb, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!teeb.getIsActive() || teeb.getLevel() == null) {
            return;
        }
        teeb.getBeamEndPoints();
        Type type = teeb.getBridgeType();
        double rot = (teeb.getLevel().getGameTime() % 100.0d) * Math.PI / 50.0d + (Math.PI / 50.0d * partialTicks);
        BlockPos pos = teeb.getBlockPos();
        poseStack.pushPose();
        poseStack.translate(0.5d, 0.0d, 0.5d);

        if (type == Type.TRANSMITTER) {
            renderBeamVertical(poseStack, bufferSource, teeb.beamYMin - pos.getY(), 0.0d, 0.2d, rot, 3.0d, teeb.getIsPowered());
            renderBeamVertical(poseStack, bufferSource, 1.0d, teeb.beamYMax - pos.getY(), 0.2d, rot, 3.0d, teeb.getIsPowered());
        } else if (type == Type.RECEIVER) {
            renderBeamVertical(poseStack, bufferSource, teeb.beamYMin - pos.getY(), 0.0d, 0.2d, rot, 3.0d, teeb.getIsPowered());
            renderBeamVertical(poseStack, bufferSource, 1.0d, teeb.beamYMax - pos.getY(), 0.2d, rot, -3.0d, teeb.getIsPowered());
        } else if (type == Type.RESONATOR) {
            Direction dirFront = teeb.getFacing();
            Direction dirSide = dirFront.getClockWise();

            poseStack.pushPose();
            poseStack.translate(0.5d * dirFront.getStepX(), 0.5d, 0.5d * dirFront.getStepZ());
            poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(90),
                    -dirSide.getStepX(), 0, -dirSide.getStepZ()));
            renderBeamVertical(poseStack, bufferSource, 0.0d, 2.0d, 0.2d, rot, 3.0d, teeb.getIsPowered());
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0.3d * dirSide.getStepX() - 0.2d * dirFront.getStepX(), 0.5d,
                    0.3d * dirSide.getStepZ() - 0.2d * dirFront.getStepZ());
            poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(90),
                    dirFront.getStepX(), 0, dirFront.getStepZ()));
            poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(45),
                    -dirSide.getStepX(), 0, -dirSide.getStepZ()));
            renderBeamVertical(poseStack, bufferSource, 0.0d, 4.2d, 0.14d, rot, 3.0d, teeb.getIsPowered());
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderBeamVertical(PoseStack poseStack, MultiBufferSource bufferSource, double yMin, double yMax, double radius, double rot, double flowSpeed, boolean powered) {
        double angle = 0.0d;
        double vScale = yMax - yMin;
        double v1 = -rot * flowSpeed;
        double v2 = (vScale * 2.0d) + v1;

        int innerColor;
        int outerColor;
        if (powered) {
            innerColor = FastColor.ARGB32.color(200, 160, 255, 230);
            outerColor = FastColor.ARGB32.color(80, 210, 255, 230);
        } else {
            innerColor = FastColor.ARGB32.color(200, 255, 40, 40);
            outerColor = FastColor.ARGB32.color(80, 255, 64, 48);
        }

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer inner = bufferSource.getBuffer(EnergyBridgeRenderTypes.INNER);
        for (int i = 0; i < 8; i++) {
            double tx1 = Math.sin(rot + angle) * radius;
            double tz1 = Math.cos(rot + angle) * radius;
            angle += Math.PI / 4.0d;
            double tx2 = Math.sin(rot + angle) * radius;
            double tz2 = Math.cos(rot + angle) * radius;
            quad(pose, inner, tx1, yMin, tz1, tx2, yMax, tz2, 0.125f, (float) v1, 0.875f, (float) v2, innerColor);
        }

        v1 = -rot * flowSpeed * 3.0d;
        v2 = (vScale * 2.0d) + v1;
        radius *= 2.0d;
        rot = Math.PI / 8.0d;
        VertexConsumer outer = bufferSource.getBuffer(EnergyBridgeRenderTypes.OUTER);
        for (int i = 0; i < 8; i++) {
            double tx1 = Math.sin(rot + angle) * radius;
            double tz1 = Math.cos(rot + angle) * radius;
            angle += Math.PI / 4.0d;
            double tx2 = Math.sin(rot + angle) * radius;
            double tz2 = Math.cos(rot + angle) * radius;
            quad(pose, outer, tx1, yMin, tz1, tx2, yMax, tz2, 0.125f, (float) v1, 0.875f, (float) v2, outerColor);
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, double tx1, double yMin, double tz1, double tx2, double yMax, double tz2,
            float u1, float v1, float u2, float v2, int color) {
        vertex(pose, consumer, (float) tx1, (float) yMin, (float) tz1, u1, v1, color);
        vertex(pose, consumer, (float) tx1, (float) yMax, (float) tz1, u1, v2, color);
        vertex(pose, consumer, (float) tx2, (float) yMax, (float) tz2, u2, v2, color);
        vertex(pose, consumer, (float) tx2, (float) yMin, (float) tz2, u2, v1, color);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, int color) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setLight(0xF000F0);
    }

    @Override
    public boolean shouldRenderOffScreen(EnergyBridgeBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(EnergyBridgeBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).multiply(1.0, 0.0, 1.0)
                .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(EnergyBridgeBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX() - 4.0, -256.0, pos.getZ() - 4.0, pos.getX() + 5.0, 1024.0, pos.getZ() + 5.0);
    }
}
