package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.client.renderer.GhostRenderTypes;
import dev.celli.enderutilities.config.ModClientConfig;
import dev.celli.enderutilities.data.WandModeOptions;
import dev.celli.enderutilities.item.ItemBuildersWand;
import dev.celli.enderutilities.item.ItemRuler;
import dev.celli.enderutilities.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

@EventBusSubscriber(modid = EnderUtilities.MOD_ID, value = Dist.CLIENT)
public final class RulerOverlay {
    /** Orig {@code RulerRenderer.COLORS}. */
    private static final int[] COLORS = {0x70FFFF, 0xFF70FF, 0xFFFF70, 0xA401CD, 0x3C3CC9, 0xD9850C, 0x13A43C, 0xED2235};
    private static final Map<Integer, List<BlockPos>> AXIS_COLUMNS = new HashMap<>();

    private RulerOverlay() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        ItemStack wand = heldWand(player);
        if (!wand.isEmpty()) {
            renderWandPreview(minecraft, event, player, wand);
            return;
        }
        ItemStack ruler = heldOrUnselectedRuler(player);
        if (!ruler.isEmpty()) {
            renderRuler(minecraft, event, player, ruler);
        }
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        ItemStack wand = heldWand(player);
        if (!wand.isEmpty()) {
            renderHud(event.getGuiGraphics(), minecraft, wandHud(wand, player));
            return;
        }
        ItemStack ruler = heldOrUnselectedRuler(player);
        if (!ruler.isEmpty()) {
            renderHud(event.getGuiGraphics(), minecraft, rulerHud(ruler, player));
        }
    }

    private static void renderRuler(Minecraft minecraft, RenderLevelStageEvent event, Player player, ItemStack stack) {
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        int selected = ItemRuler.locationSelection(stack);
        if (ItemRuler.renderAll(stack)) {
            int count = ItemRuler.locationCount(stack);
            for (int i = 0; i < count; i++) {
                if (i == selected || !ItemRuler.alwaysRenderLocation(stack, i)) {
                    continue;
                }
                int color = i < COLORS.length ? COLORS[i] : 0x70FFFF;
                renderPointPair(pose, consumer, player,
                        ItemRuler.position(stack, i, true).orElse(null),
                        ItemRuler.position(stack, i, false).orElse(null),
                        color);
            }
        }
        renderPointPair(pose, consumer, player,
                ItemRuler.position(stack, selected, true).orElse(null),
                ItemRuler.position(stack, selected, false).orElse(null),
                0xFFFFFF);

        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void renderPointPair(PoseStack pose, VertexConsumer consumer, Player player,
            BlockPos posStart, BlockPos posEnd, int color) {
        updatePositions(player, posStart, posEnd);
        renderPositions(pose, consumer, color);
        renderStartAndEnd(pose, consumer, posStart, posEnd);
    }

    private static void renderPositions(PoseStack pose, VertexConsumer consumer, int color) {
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        for (int a = 0; a < 3; a++) {
            List<BlockPos> column = AXIS_COLUMNS.get(a);
            if (column == null) {
                continue;
            }
            for (BlockPos pos : column) {
                LevelRenderer.renderLineBox(pose, consumer, new AABB(pos), r, g, b, 1.0f);
            }
        }
    }

    private static void renderStartAndEnd(PoseStack pose, VertexConsumer consumer, BlockPos posStart, BlockPos posEnd) {
        if (posStart != null) {
            LevelRenderer.renderLineBox(pose, consumer, new AABB(posStart), 1.0f, 0x11 / 255f, 0x11 / 255f, 1.0f);
        }
        if (posEnd != null) {
            LevelRenderer.renderLineBox(pose, consumer, new AABB(posEnd), 0x11 / 255f, 0x11 / 255f, 1.0f, 1.0f);
        }
    }

    private static void updatePositions(Player player, BlockPos posStart, BlockPos posEnd) {
        AXIS_COLUMNS.clear();
        if (posStart == null && posEnd == null) {
            return;
        }
        if (posStart == null) {
            posStart = posEnd;
            posEnd = player.blockPosition();
        } else if (posEnd == null) {
            posEnd = player.blockPosition();
        }
        BlockPos[] pos = {posStart, posEnd};
        int[] done = {0, 0, 0};
        for (int i = 0; i < 3; i++) {
            BlockPosAligner aligner = new BlockPosAligner(pos[0], pos[1], player);
            BlockPos aligned = aligner.alignedPointAlongLongestAxis();
            int furthest = aligner.furthestPoint;
            if (aligner.axisLength > 0) {
                boolean includeStart = !aligned.equals(pos[0]) && !aligned.equals(pos[1]);
                AXIS_COLUMNS.put(aligner.longestAxis, column(aligned, pos[furthest], aligner.longestAxis, includeStart, false));
                done[aligner.longestAxis] = 1;
            }
            pos[furthest] = aligned;
        }
        for (int i = 0; i < 3; i++) {
            if (done[i] == 0) {
                AXIS_COLUMNS.remove(i);
            }
        }
    }

    private static List<BlockPos> column(BlockPos posNear, BlockPos posFar, int axis, boolean includeStart, boolean includeEnd) {
        List<BlockPos> list = new ArrayList<>();
        int[] p1 = {posNear.getX(), posNear.getY(), posNear.getZ()};
        int[] p2 = {posFar.getX(), posFar.getY(), posFar.getZ()};
        int inc = p1[axis] < p2[axis] ? 1 : -1;
        if (!includeStart) {
            p1[axis] += inc;
        }
        if (!includeEnd) {
            p2[axis] -= inc;
        }
        int maxLength = 160;
        if (p1[axis] <= p2[axis]) {
            for (int i = 0; i < maxLength && p1[axis] <= p2[axis]; i++) {
                list.add(new BlockPos(p1[0], p1[1], p1[2]));
                p1[axis] += 1;
            }
        } else {
            for (int i = 0; i < maxLength && p1[axis] >= p2[axis]; i++) {
                list.add(new BlockPos(p1[0], p1[1], p1[2]));
                p1[axis] -= 1;
            }
        }
        return list;
    }

    private static void renderWandPreview(Minecraft minecraft, RenderLevelStageEvent event, Player player, ItemStack wand) {
        ItemBuildersWand.Mode mode = ItemBuildersWand.Mode.fromStack(wand);
        BlockPos start = wandStart(wand, player, mode);
        if (start == null) {
            return;
        }
        BlockPos end = wandEnd(wand, mode);
        List<BlockPos> ghosts = ItemBuildersWand.previewPositions(wand, player.level(), start, faceOf(wand, start, player));
        boolean renderGhost = ItemBuildersWand.option(wand, mode, WandModeOptions.RENDER_GHOST);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();

        if (renderGhost && !mode.isAreaMode() && !ghosts.isEmpty()) {
            renderGhostModels(minecraft, event, ghosts, wand);
        }

        var buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        if (!renderGhost && !mode.isAreaMode()) {
            for (BlockPos ghost : ghosts) {
                if (ghost.equals(start) || (end != null && ghost.equals(end))) {
                    continue;
                }
                float expand = mode == ItemBuildersWand.Mode.REPLACE ? 0.001f : 0f;
                LevelRenderer.renderLineBox(pose, lines, box(ghost, expand), 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }

        if (mode == ItemBuildersWand.Mode.STACK && start != null && end != null) {
            renderStackedArea(pose, lines, wand, start, end);
            renderStartEndBoxes(pose, lines, mode, start, end, 0x22 / 255f, 1.0f, 0x22 / 255f);
        } else {
            renderStartEndBoxes(pose, lines, mode, start, end, 1.0f, 1.0f, 1.0f);
        }

        if (mode == ItemBuildersWand.Mode.MOVE_DST) {
            BlockPos srcStart = wand.get(ModDataComponents.WAND_MOVE_ORIGIN);
            BlockPos srcEnd = wand.get(ModDataComponents.RULER_END);
            if (srcStart != null && srcEnd != null) {
                AABB area = enclosing(srcStart, srcEnd);
                LevelRenderer.renderLineBox(pose, lines, area, 1.0f, 0x55 / 255f, 0x55 / 255f, 0xCC / 255f);
                LevelRenderer.renderLineBox(pose, lines, box(srcStart, 0.001f), 1.0f, 0x11 / 255f, 0x11 / 255f, 1.0f);
                LevelRenderer.renderLineBox(pose, lines, box(srcEnd, 0.001f), 0x11 / 255f, 0x11 / 255f, 1.0f, 1.0f);
            }
        }

        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void renderStartEndBoxes(PoseStack pose, VertexConsumer lines, ItemBuildersWand.Mode mode,
            BlockPos start, BlockPos end, float r, float g, float b) {
        if (start != null && end != null && mode.isAreaMode()) {
            LevelRenderer.renderLineBox(pose, lines, enclosing(start, end), r, g, b, 0xCC / 255f);
        }
        float expand = mode == ItemBuildersWand.Mode.REPLACE
                || mode == ItemBuildersWand.Mode.REPLACE_3D
                || mode.hasTwoPlacableCorners() ? 0.001f : 0f;
        if (start != null) {
            LevelRenderer.renderLineBox(pose, lines, box(start, expand), 1.0f, 0x11 / 255f, 0x11 / 255f, 1.0f);
        }
        if (end != null && (mode.isAreaMode() || mode == ItemBuildersWand.Mode.WALLS || mode == ItemBuildersWand.Mode.CUBE)) {
            LevelRenderer.renderLineBox(pose, lines, box(end, expand), 0x11 / 255f, 0x11 / 255f, 1.0f, 1.0f);
        }
    }

    private static void renderStackedArea(PoseStack pose, VertexConsumer lines, ItemStack wand, BlockPos start, BlockPos end) {
        WandModeOptions.Area3D area = ItemBuildersWand.options(wand).stackArea();
        int xp = area.xp();
        int yp = area.yp();
        int zp = area.zp();
        int xn = area.xn();
        int yn = area.yn();
        int zn = area.zn();
        int sx = Math.abs(end.getX() - start.getX()) + 1;
        int sy = Math.abs(end.getY() - start.getY()) + 1;
        int sz = Math.abs(end.getZ() - start.getZ()) + 1;
        AABB original = enclosing(start, end);
        if (xp + xn + yp + yn + zp + zn == 0) {
            return;
        }
        for (int y = -yn; y <= yp; y++) {
            for (int x = -xn; x <= xp; x++) {
                for (int z = -zn; z <= zp; z++) {
                    AABB aabb = original.move(x * sx, y * sy, z * sz);
                    if (x != 0 || y != 0 || z != 0) {
                        LevelRenderer.renderLineBox(pose, lines, aabb, 1f, 1f, 1f, 0xCC / 255f);
                    } else {
                        LevelRenderer.renderLineBox(pose, lines, aabb, 0.5f, 1f, 0.5f, 0xCC / 255f);
                    }
                }
            }
        }
    }

    private static List<Component> rulerHud(ItemStack stack, Player player) {
        List<Component> lines = new ArrayList<>();
        BlockPos posStart = ItemRuler.start(stack).orElse(null);
        BlockPos posEnd = ItemRuler.end(stack).orElse(null);
        if (posStart == null && posEnd == null) {
            return lines;
        }
        if (posStart == null) {
            posStart = posEnd;
            posEnd = player.blockPosition();
        } else if (posEnd == null) {
            posEnd = player.blockPosition();
        }
        int lenX = Math.abs(posStart.getX() - posEnd.getX());
        int lenY = Math.abs(posStart.getY() - posEnd.getY());
        int lenZ = Math.abs(posStart.getZ() - posEnd.getZ());
        boolean dimensions = !ItemRuler.distanceDifference(stack);
        if (dimensions) {
            lenX += 1;
            lenY += 1;
            lenZ += 1;
        }
        ChatFormatting green = ChatFormatting.GREEN;
        String modeKey = dimensions
                ? "enderutilities.tooltip.item.ruler.dimensions"
                : "enderutilities.tooltip.item.ruler.difference";
        lines.add(Component.translatable("enderutilities.tooltip.item.mode")
                .append(": ")
                .append(Component.translatable(modeKey).withStyle(green))
                .append(" - X: ")
                .append(Component.literal(String.valueOf(lenX)).withStyle(green))
                .append(", Y: ")
                .append(Component.literal(String.valueOf(lenY)).withStyle(green))
                .append(", Z: ")
                .append(Component.literal(String.valueOf(lenZ)).withStyle(green)));
        lines.add(hudCoord("enderutilities.tooltip.item.start", posStart));
        lines.add(hudCoord("enderutilities.tooltip.item.end", posEnd));
        return lines;
    }

    private static List<Component> wandHud(ItemStack stack, Player player) {
        List<Component> lines = new ArrayList<>();
        ItemBuildersWand.Mode mode = ItemBuildersWand.Mode.fromStack(stack);
        ChatFormatting green = ChatFormatting.GREEN;
        ChatFormatting red = ChatFormatting.RED;
        ChatFormatting aqua = ChatFormatting.AQUA;
        int index = ItemBuildersWand.selection(stack);
        if (mode == ItemBuildersWand.Mode.REPLACE_3D) {
            lines.add(Component.translatable("enderutilities.tooltip.item.build.replacement")
                    .append(String.format(" [%d/%d]: ", index + 1, ItemBuildersWand.MAX_TEMPLATE_SLOTS))
                    .append(blockTypeName(stack, index, false)));
            int secondary = ItemBuildersWand.secondarySelection(stack);
            lines.add(Component.translatable("enderutilities.tooltip.item.build.target")
                    .append(String.format(" [%d/%d]: ", secondary + 1, ItemBuildersWand.MAX_TEMPLATE_SLOTS))
                    .append(blockTypeName(stack, secondary, true)));
            boolean bind = ItemBuildersWand.option(stack, mode, WandModeOptions.BIND_MODE);
            lines.add(Component.translatable("enderutilities.tooltip.item.build.bindmode")
                    .append(": ")
                    .append(Component.translatable(bind ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                            .withStyle(bind ? green : red)));
        } else if (mode.isAreaMode()) {
            if (mode == ItemBuildersWand.Mode.COPY || mode == ItemBuildersWand.Mode.PASTE) {
                String name = stack.getOrDefault(ModDataComponents.WAND_TEMPLATE, dev.celli.enderutilities.data.WandTemplate.EMPTY).name();
                if (name.isEmpty()) {
                    name = "N/A";
                }
                lines.add(Component.translatable("enderutilities.tooltip.item.template")
                        .append(String.format(" [%d/%d]: ", index + 1, ItemBuildersWand.MAX_TEMPLATE_SLOTS))
                        .append(Component.literal(name).withStyle(ChatFormatting.ITALIC)));
            } else {
                lines.add(Component.translatable("enderutilities.tooltip.item.area")
                        .append(String.format(": [%d/%d]", index + 1, ItemBuildersWand.MAX_TEMPLATE_SLOTS)));
            }
            if (mode == ItemBuildersWand.Mode.COPY) {
                boolean cnb = ItemBuildersWand.option(stack, mode, WandModeOptions.CHISELS_AND_BITS_CROSSWORLD);
                lines.add(Component.translatable("enderutilities.tooltip.item.build.chiselsandbits.crossworld")
                        .append(": ")
                        .append(Component.translatable(cnb ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                                .withStyle(cnb ? green : red)));
            } else {
                Direction facing = ItemBuildersWand.areaFacing(stack);
                Component facingText = facing != null
                        ? Component.literal(facing.getSerializedName()).withStyle(green)
                        : Component.literal("N/A").withStyle(red);
                boolean mirrored = ItemBuildersWand.mirrored(stack);
                Component mirrorText = mirrored
                        ? Component.literal(ItemBuildersWand.mirror(stack) == Mirror.FRONT_BACK ? "x" : "z").withStyle(green)
                        : Component.translatable("enderutilities.tooltip.item.no").withStyle(red);
                lines.add(Component.translatable("enderutilities.tooltip.item.rotation")
                        .append(": ")
                        .append(facingText)
                        .append(" - ")
                        .append(Component.translatable("enderutilities.tooltip.item.mirror"))
                        .append(": ")
                        .append(mirrorText));
            }
            if (mode == ItemBuildersWand.Mode.DELETE) {
                boolean entities = ItemBuildersWand.affectEntities(stack);
                lines.add(Component.translatable("enderutilities.tooltip.item.entities")
                        .append(": ")
                        .append(Component.translatable(entities ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                                .withStyle(entities ? green : red)));
            } else if (mode == ItemBuildersWand.Mode.PASTE || mode == ItemBuildersWand.Mode.MOVE_DST || mode == ItemBuildersWand.Mode.STACK) {
                boolean replace = ItemBuildersWand.replaceExisting(stack);
                MutableComponent line = Component.translatable("enderutilities.tooltip.item.replace")
                        .append(": ")
                        .append(Component.translatable(replace ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                                .withStyle(replace ? green : red));
                if (mode != ItemBuildersWand.Mode.MOVE_DST) {
                    boolean entities = ItemBuildersWand.affectEntities(stack);
                    line = line.append(" - ")
                            .append(Component.translatable("enderutilities.tooltip.item.entities"))
                            .append(": ")
                            .append(Component.translatable(entities ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                                    .withStyle(entities ? green : red));
                }
                lines.add(line);
            }
        } else {
            if (index >= 0) {
                lines.add(Component.translatable("enderutilities.tooltip.item.block")
                        .append(String.format(" [%d/%d]: ", index + 1, ItemBuildersWand.MAX_TEMPLATE_SLOTS))
                        .append(blockTypeName(stack, index, false)));
            } else {
                Component type = index == WandModeOptions.BLOCK_TYPE_TARGETED
                        ? Component.translatable("enderutilities.tooltip.item.blocktype.targeted").withStyle(aqua)
                        : Component.translatable("enderutilities.tooltip.item.blocktype.adjacent").withStyle(aqua);
                lines.add(Component.translatable("enderutilities.tooltip.item.block").append(": ").append(type));
            }
            if (mode != ItemBuildersWand.Mode.WALLS && mode != ItemBuildersWand.Mode.CUBE) {
                if (mode != ItemBuildersWand.Mode.REPLACE) {
                    boolean flipped = ItemBuildersWand.option(stack, mode, WandModeOptions.AREA_FLIPPED);
                    boolean move = ItemBuildersWand.option(stack, mode, WandModeOptions.MOVE_POSITION);
                    Component flip = flipped
                            ? Component.literal(String.valueOf(ItemBuildersWand.flipAxis(stack))).withStyle(green)
                            : Component.translatable("enderutilities.tooltip.item.no").withStyle(red);
                    lines.add(Component.translatable("enderutilities.tooltip.item.area.flipped")
                            .append(": ")
                            .append(flip)
                            .append(" - ")
                            .append(Component.translatable("enderutilities.tooltip.item.move"))
                            .append(": ")
                            .append(Component.translatable(move ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                                    .withStyle(move ? green : red)));
                }
                if (mode == ItemBuildersWand.Mode.COLUMN || mode == ItemBuildersWand.Mode.LINE
                        || mode == ItemBuildersWand.Mode.PLANE || mode == ItemBuildersWand.Mode.EXTEND_AREA
                        || mode == ItemBuildersWand.Mode.CIRCLE) {
                    boolean through = ItemBuildersWand.option(stack, mode, WandModeOptions.CONTINUE_THROUGH);
                    boolean replace = ItemBuildersWand.replaceExisting(stack);
                    lines.add(Component.translatable("enderutilities.tooltip.item.continuethrough")
                            .append(": ")
                            .append(Component.translatable(through ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                                    .withStyle(through ? green : red))
                            .append(" - ")
                            .append(Component.translatable("enderutilities.tooltip.item.replace"))
                            .append(": ")
                            .append(Component.translatable(replace ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                                    .withStyle(replace ? green : red)));
                } else if (mode == ItemBuildersWand.Mode.EXTEND_CONTINUOUS
                        || (mode == ItemBuildersWand.Mode.REPLACE
                        && ItemBuildersWand.option(stack, mode, WandModeOptions.REPLACE_MODE_IS_AREA))) {
                    boolean diagonals = ItemBuildersWand.option(stack, mode, WandModeOptions.ALLOW_DIAGONALS);
                    lines.add(Component.translatable("enderutilities.tooltip.item.builderswand.allowdiagonals")
                            .append(": ")
                            .append(Component.translatable(diagonals ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                                    .withStyle(diagonals ? green : red)));
                }
            }
        }
        String modeName = Component.translatable(mode.tooltip()).getString();
        if (mode == ItemBuildersWand.Mode.REPLACE) {
            boolean area = ItemBuildersWand.option(stack, mode, WandModeOptions.REPLACE_MODE_IS_AREA);
            modeName += " (" + Component.translatable(area
                    ? "enderutilities.tooltip.item.area"
                    : "enderutilities.tooltip.item.single").getString() + ")";
        }
        int modeId = mode.ordinal() + 1;
        int maxMode = ItemBuildersWand.Mode.countFor(player);
        lines.add(Component.translatable("enderutilities.tooltip.item.mode")
                .append(String.format(" [%d/%d]: ", modeId, maxMode))
                .append(Component.literal(modeName).withStyle(green)));
        return lines;
    }

    private static Component blockTypeName(ItemStack wand, int index, boolean secondary) {
        if (index >= 0) {
            ItemStack bound = ItemBuildersWand.options(wand).boundBlock(index);
            if (!bound.isEmpty() && bound.getItem() != Blocks.AIR.asItem()) {
                return bound.getHoverName().copy().withStyle(ChatFormatting.GREEN);
            }
            return Component.literal("N/A").withStyle(ChatFormatting.RED);
        }
        if (index == WandModeOptions.BLOCK_TYPE_TARGETED) {
            return Component.translatable("enderutilities.tooltip.item.blocktype.targeted").withStyle(ChatFormatting.AQUA);
        }
        return Component.translatable("enderutilities.tooltip.item.blocktype.adjacent").withStyle(ChatFormatting.AQUA);
    }

    private static Component hudCoord(String label, BlockPos pos) {
        ChatFormatting green = ChatFormatting.GREEN;
        return Component.translatable(label)
                .append(": x: ")
                .append(Component.literal(String.valueOf(pos.getX())).withStyle(green))
                .append(", y: ")
                .append(Component.literal(String.valueOf(pos.getY())).withStyle(green))
                .append(", z: ")
                .append(Component.literal(String.valueOf(pos.getZ())).withStyle(green));
    }

    private static void renderHud(GuiGraphics graphics, Minecraft minecraft, List<Component> lines) {
        if (lines.isEmpty()) {
            return;
        }
        int lineHeight = minecraft.font.lineHeight + 2;
        int x = 4;
        int y = minecraft.getWindow().getGuiScaledHeight() - (lineHeight * lines.size());
        for (Component line : lines) {
            int width = minecraft.font.width(line);
            graphics.fill(x - 2, y - 2, x + width + 2, y + minecraft.font.lineHeight, 0x80000000);
            graphics.drawString(minecraft.font, line, x, y, 0xFFFFFFFF, true);
            y += lineHeight;
        }
    }

    private static void renderGhostModels(Minecraft minecraft, RenderLevelStageEvent event, List<BlockPos> ghosts, ItemStack wand) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        BlockState state = ItemBuildersWand.ghostBlockState(wand, minecraft.level,
                ghosts.isEmpty() ? null : ghosts.getFirst());
        if (state == null || state.isAir()) {
            VertexConsumer lines = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
            PoseStack pose = event.getPoseStack();
            Vec3 camera = event.getCamera().getPosition();
            pose.pushPose();
            pose.translate(-camera.x, -camera.y, -camera.z);
            for (BlockPos ghost : ghosts) {
                LevelRenderer.renderLineBox(pose, lines, new AABB(ghost), 0.85F, 0.85F, 1.0F, 0.55F);
            }
            pose.popPose();
            minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
            return;
        }
        var dispatcher = minecraft.getBlockRenderer();
        var buffers = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        if (!ModClientConfig.BUILDERS_WAND_USE_TRANSLUCENT_GHOST_BLOCKS.get()) {
            renderGhostPass(dispatcher, buffers, pose, camera, state, ghosts, minecraft,
                    GhostRenderTypes.OPAQUE, 0xFF, false);
            buffers.endBatch(GhostRenderTypes.OPAQUE);
            return;
        }
        int alpha = ModClientConfig.ghostAlphaByte();
        renderGhostPass(dispatcher, buffers, pose, camera, state, ghosts, minecraft,
                GhostRenderTypes.DEPTH, alpha, false);
        buffers.endBatch(GhostRenderTypes.DEPTH);
        renderGhostPass(dispatcher, buffers, pose, camera, state, ghosts, minecraft,
                GhostRenderTypes.COLOR, alpha, true);
        buffers.endBatch(GhostRenderTypes.COLOR);
    }

    private static void renderGhostPass(
            net.minecraft.client.renderer.block.BlockRenderDispatcher dispatcher,
            net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,
            PoseStack pose,
            Vec3 camera,
            BlockState state,
            List<BlockPos> ghosts,
            Minecraft minecraft,
            RenderType type,
            int alpha,
            boolean tint
    ) {
        VertexConsumer consumer = tint
                ? new AlphaVertexConsumer(buffers.getBuffer(type), alpha)
                : buffers.getBuffer(type);
        var random = net.minecraft.util.RandomSource.create();
        int rendered = 0;
        for (BlockPos ghost : ghosts) {
            if (rendered++ >= 1024) {
                break;
            }
            pose.pushPose();
            pose.translate(ghost.getX() - camera.x, ghost.getY() - camera.y, ghost.getZ() - camera.z);
            if (minecraft.level != null && !minecraft.level.getBlockState(ghost).isAir()) {
                pose.translate(-0.001, -0.001, -0.001);
                pose.scale(1.002F, 1.002F, 1.002F);
            }
            dispatcher.renderBatched(state, ghost, minecraft.level, pose, consumer, false, random);
            pose.popPose();
        }
    }

    private static BlockPos wandStart(ItemStack wand, Player player, ItemBuildersWand.Mode mode) {
        BlockPos start = wand.get(ModDataComponents.RULER_START);
        if (start != null) {
            return start;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        if (player.isShiftKeyDown() && blockHit.getDirection() == Direction.UP && mode != ItemBuildersWand.Mode.REPLACE) {
            return null;
        }
        if (mode == ItemBuildersWand.Mode.REPLACE) {
            return blockHit.getBlockPos();
        }
        return blockHit.getBlockPos().relative(blockHit.getDirection());
    }

    private static BlockPos wandEnd(ItemStack wand, ItemBuildersWand.Mode mode) {
        if (mode.isAreaMode() || mode == ItemBuildersWand.Mode.WALLS || mode == ItemBuildersWand.Mode.CUBE) {
            return wand.get(ModDataComponents.RULER_END);
        }
        return null;
    }

    private static Direction faceOf(ItemStack wand, BlockPos origin, Player player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (hit instanceof BlockHitResult blockHit) {
            return blockHit.getDirection();
        }
        return Direction.UP;
    }

    private static ItemStack heldWand(Player player) {
        for (ItemStack stack : player.getHandSlots()) {
            if (stack.getItem() instanceof ItemBuildersWand) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack heldOrUnselectedRuler(Player player) {
        for (ItemStack stack : player.getHandSlots()) {
            if (stack.getItem() instanceof ItemRuler) {
                return stack;
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ItemRuler && ItemRuler.renderWhenUnselected(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static AABB enclosing(BlockPos a, BlockPos b) {
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX()) + 1;
        int maxY = Math.max(a.getY(), b.getY()) + 1;
        int maxZ = Math.max(a.getZ(), b.getZ()) + 1;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static AABB box(BlockPos pos, float expand) {
        return new AABB(pos).inflate(expand);
    }

    private static final class BlockPosAligner {
        private final double[] playerPos;
        private int longestAxis;
        private int axisLength;
        private int furthestPoint;
        private final int[][] points;

        private BlockPosAligner(BlockPos p1, BlockPos p2, Player player) {
            this.playerPos = new double[] {player.getX(), player.getY() + player.getEyeHeight(), player.getZ()};
            this.points = new int[][] {
                    {p1.getX(), p1.getY(), p1.getZ()},
                    {p2.getX(), p2.getY(), p2.getZ()}
            };
        }

        private int longestAxis() {
            int longest = 0;
            int length = Math.abs(points[0][0] - points[1][0]);
            for (int i = 1; i < 3; i++) {
                int tmp = Math.abs(points[0][i] - points[1][i]);
                if (tmp > length) {
                    longest = i;
                    length = tmp;
                }
            }
            longestAxis = longest;
            axisLength = length;
            return longest;
        }

        private int furthestPointIndexOnLongestAxis() {
            int axisId = longestAxis();
            double len0 = Math.abs(playerPos[axisId] - (points[0][axisId] + 0.5d));
            double len1 = Math.abs(playerPos[axisId] - (points[1][axisId] + 0.5d));
            furthestPoint = len0 > len1 ? 0 : 1;
            return furthestPoint;
        }

        private BlockPos alignedPointAlongLongestAxis() {
            int far = furthestPointIndexOnLongestAxis();
            int near = far ^ 0x1;
            int[] p = {points[far][0], points[far][1], points[far][2]};
            p[longestAxis] = points[near][longestAxis];
            return new BlockPos(p[0], p[1], p[2]);
        }
    }

    private static final class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer parent;
        private final int alpha;

        private AlphaVertexConsumer(VertexConsumer parent, int alpha) {
            this.parent = parent;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return parent.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return parent.setColor(r, g, b, alpha);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return parent.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return parent.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return parent.setUv2(u, v);
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return parent.setNormal(x, y, z);
        }
    }
}
