package dev.celli.enderutilities.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.block.PortalPanelBlock;
import dev.celli.enderutilities.blockentity.InserterBlockEntity;
import dev.celli.enderutilities.blockentity.PortalPanelBlockEntity;
import dev.celli.enderutilities.entity.EntityChair;
import dev.celli.enderutilities.item.ItemBlockPlacementProperty;
import dev.celli.enderutilities.item.ItemChairWand;
import dev.celli.enderutilities.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Orig {@code RenderEventHandler}: placement-property HUD, portal panel names,
 * inserter part highlight, chair-wand debug boxes.
 */
@EventBusSubscriber(modid = EnderUtilities.MOD_ID, value = Dist.CLIENT)
public final class MachineOverlays {
    private static final AABB INSERTER_MAIN_NS = new AABB(0.25, 0.25, 0.0, 0.75, 0.75, 1.0);
    private static final AABB INSERTER_MAIN_WE = new AABB(0.0, 0.25, 0.25, 1.0, 0.75, 0.75);
    private static final AABB[] INSERTER_SIDES = {
            new AABB(0.25, 0.0, 0.25, 0.75, 0.25, 0.75),
            new AABB(0.25, 0.75, 0.25, 0.75, 1.0, 0.75),
            new AABB(0.25, 0.25, 0.0, 0.75, 0.75, 0.25),
            new AABB(0.25, 0.25, 0.75, 0.75, 0.75, 1.0),
            new AABB(0.0, 0.25, 0.25, 0.25, 0.75, 0.75),
            new AABB(0.75, 0.25, 0.25, 1.0, 0.75, 0.75)
    };

    private MachineOverlays() {
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemBlockPlacementProperty)) {
            stack = player.getOffhandItem();
        }
        if (!(stack.getItem() instanceof ItemBlockPlacementProperty item) || !item.hasPlacementProperty(stack)) {
            return;
        }
        List<Component> lines = item.hudLines(stack);
        int lineHeight = minecraft.font.lineHeight + 2;
        int x = 4;
        int y = minecraft.getWindow().getGuiScaledHeight() - (lineHeight * lines.size());
        for (Component line : lines) {
            int width = minecraft.font.width(line);
            event.getGuiGraphics().fill(x - 2, y - 2, x + width + 2, y + minecraft.font.lineHeight, 0x80000000);
            event.getGuiGraphics().drawString(minecraft.font, line, x, y, 0xFFFFFFFF, true);
            y += lineHeight;
        }
    }

    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        BlockHitResult hit = event.getTarget();
        BlockPos pos = hit.getBlockPos();
        AABB box = null;
        if (level.getBlockState(pos).is(ModBlocks.INSERTER.get())
                || level.getBlockState(pos).is(ModBlocks.INSERTER_FILTERED.get())) {
            box = pointedInserterBox(level, pos, player);
        } else if (level.getBlockState(pos).getBlock() instanceof PortalPanelBlock panel) {
            box = panel.getPointedHighlightBox(pos, level.getBlockState(pos).getValue(PortalPanelBlock.FACING), player);
        }
        if (box == null) {
            return;
        }
        event.setCanceled(true);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, consumer,
                box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z,
                box.maxX - camera.x, box.maxY - camera.y, box.maxZ - camera.z,
                0.0F, 0.0F, 0.0F, 0.4F);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        renderChairBoxes(minecraft, event, player, level);
        renderPortalPanelName(minecraft, event, player, level);
    }

    private static void renderChairBoxes(Minecraft minecraft, RenderLevelStageEvent event, Player player, Level level) {
        if (!(player.getMainHandItem().getItem() instanceof ItemChairWand)
                && !(player.getOffhandItem().getItem() instanceof ItemChairWand)) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (EntityChair chair : level.getEntitiesOfClass(EntityChair.class, player.getBoundingBox().inflate(128.0))) {
            AABB box = chair.getBoundingBox();
            LevelRenderer.renderLineBox(pose, consumer, box, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void renderPortalPanelName(Minecraft minecraft, RenderLevelStageEvent event, Player player, Level level) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof PortalPanelBlock panel)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PortalPanelBlockEntity te)) {
            return;
        }
        Direction facing = level.getBlockState(pos).getValue(PortalPanelBlock.FACING);
        Integer elementId = panel.getPointedElementId(pos, facing, player);
        String name = elementId != null && elementId >= 0 && elementId <= 7
                ? te.getTargetDisplayName(elementId)
                : te.getPanelDisplayName();
        if (name == null || name.isBlank()) {
            return;
        }
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        float angleH = 0f;
        float angleV = 0f;
        double frontOffset = 0.32D;
        if (facing.getAxis().isHorizontal()) {
            int frontX = facing.getStepX();
            int frontZ = facing.getStepZ();
            if (facing == Direction.NORTH || facing == Direction.WEST) {
                frontX = -frontX;
                frontZ = -frontZ;
                frontOffset = 1.0D - frontOffset;
            }
            x += frontX * frontOffset + frontZ * 0.5D;
            z += frontZ * frontOffset + frontX * 0.5D;
            y += 1.25D;
            angleH = facing.toYRot() + 180f;
        } else if (facing == Direction.DOWN) {
            frontOffset = 1.0D - frontOffset;
            x += 0.5D;
            z -= 0.25D;
            y += frontOffset;
            angleV = 90f;
        } else {
            x += 0.5D;
            z += 1.25D;
            y += frontOffset;
            angleV = -90f;
        }
        Vec3 camera = event.getCamera().getPosition();
        renderWorldLabel(minecraft, event.getPoseStack(), name, x - camera.x, y - camera.y, z - camera.z, angleH, angleV);
    }

    private static void renderWorldLabel(Minecraft minecraft, PoseStack pose, String text, double x, double y, double z,
            float angleH, float angleV) {
        Font font = minecraft.font;
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-angleH));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-angleV));
        pose.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        int half = font.width(text) / 2;
        font.drawInBatch(text, -half, 0, 0x20FFFFFF, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH,
                0x40000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(text, -half, 0, 0xFFFFFFFF, false, matrix, buffers, Font.DisplayMode.NORMAL,
                0, LightTexture.FULL_BRIGHT);
        buffers.endBatch();
        pose.popPose();
    }

    private static AABB pointedInserterBox(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        Map<String, AABB> boxes = new HashMap<>();
        if (be instanceof InserterBlockEntity inserter) {
            for (Direction side : inserter.getEnabledOutputSides()) {
                boxes.put("side-" + side.get3DDataValue(), INSERTER_SIDES[side.get3DDataValue()].move(pos));
            }
        }
        Direction facing = level.getBlockState(pos).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        AABB main = facing.getAxis() == Direction.Axis.Z ? INSERTER_MAIN_NS : INSERTER_MAIN_WE;
        boxes.put("main", main.move(pos));
        String key = dev.celli.enderutilities.util.PositionUtils.getPointedBox(player, 6.0, boxes);
        return key == null ? null : boxes.get(key);
    }
}
