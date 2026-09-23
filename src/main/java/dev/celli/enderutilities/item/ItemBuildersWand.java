package dev.celli.enderutilities.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.data.WandModeOptions;
import dev.celli.enderutilities.data.WandTemplate;
import dev.celli.enderutilities.event.tasks.PlayerTaskScheduler;
import dev.celli.enderutilities.event.tasks.TaskBuildersWand;
import dev.celli.enderutilities.event.tasks.TaskTemplatePlaceBlocks;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EUStringUtils;
import dev.celli.enderutilities.util.EnderItemTooltips;
import dev.celli.enderutilities.util.InventoryHelper;
import dev.celli.enderutilities.util.WandTemplateStore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class ItemBuildersWand extends ItemLocationBoundModular {
    public static final int ENDER_CHARGE_COST = 2;
    public static final int MAX_TEMPLATE_SLOTS = WandModeOptions.MAX_BLOCKS;
    public static final int INSTANT_PLACE_LIMIT = 60;

    public enum ReplaceMode {
        NOTHING,
        EVERYTHING,
        WITH_NON_AIR
    }

    public enum Mode {
        EXTEND_CONTINUOUS("enderutilities.tooltip.item.build.extend.continuous", false, false, false),
        EXTEND_AREA("enderutilities.tooltip.item.build.extend.area", false, false, false),
        COLUMN("enderutilities.tooltip.item.build.column", false, false, false),
        LINE("enderutilities.tooltip.item.build.line", false, false, false),
        PLANE("enderutilities.tooltip.item.build.plane", false, false, false),
        CIRCLE("enderutilities.tooltip.item.build.circle", false, false, false),
        WALLS("enderutilities.tooltip.item.build.walls", false, true, true),
        CUBE("enderutilities.tooltip.item.build.cube", false, true, true),
        REPLACE("enderutilities.tooltip.item.build.replace", false, false, false),
        REPLACE_3D("enderutilities.tooltip.item.build.replace.3d", true, true, true),
        MOVE_SRC("enderutilities.tooltip.item.build.move.source", true, true, false),
        MOVE_DST("enderutilities.tooltip.item.build.move.destination", true, false, true),
        COPY("enderutilities.tooltip.item.build.copy", true, true, true),
        PASTE("enderutilities.tooltip.item.build.paste", true, false, true),
        STACK("enderutilities.tooltip.item.build.stack", true, true, true),
        DELETE("enderutilities.tooltip.item.build.delete", true, true, true, true);

        private final String tooltip;
        private final boolean areaMode;
        private final boolean twoCorners;
        private final boolean useDelay;
        private final boolean creativeOnly;

        Mode(String tooltip, boolean areaMode, boolean twoCorners, boolean useDelay) {
            this(tooltip, areaMode, twoCorners, useDelay, false);
        }

        Mode(String tooltip, boolean areaMode, boolean twoCorners, boolean useDelay, boolean creativeOnly) {
            this.tooltip = tooltip;
            this.areaMode = areaMode;
            this.twoCorners = twoCorners;
            this.useDelay = useDelay;
            this.creativeOnly = creativeOnly;
        }

        public String tooltip() {
            return tooltip;
        }

        public boolean isAreaMode() {
            return areaMode;
        }

        public boolean hasTwoPlacableCorners() {
            return twoCorners;
        }

        public boolean hasUseDelay() {
            return useDelay;
        }

        public boolean isCreativeOnly() {
            return creativeOnly;
        }

        public static Mode fromStack(ItemStack stack) {
            return values()[Mth.clamp(stack.getOrDefault(ModDataComponents.ITEM_MODE, 0) & 0xFF, 0, values().length - 1)];
        }

        public static int countFor(Player player) {
            int count = values().length;
            if (!player.getAbilities().instabuild) {
                for (Mode mode : values()) {
                    if (mode.creativeOnly) {
                        count--;
                    }
                }
            }
            return count;
        }

        public static void cycle(ItemStack stack, boolean reverse, Player player) {
            int max = countFor(player) - 1;
            int current = stack.getOrDefault(ModDataComponents.ITEM_MODE, 0) & 0xFF;
            int next = current + (reverse ? -1 : 1);
            if (next < 0) {
                next = max;
            } else if (next > max) {
                next = 0;
            }
            stack.set(ModDataComponents.ITEM_MODE, next);
        }
    }

    private final Map<UUID, Long> lastLeftClick = new HashMap<>();

    public ItemBuildersWand(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static WandModeOptions options(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WAND_OPTIONS, WandModeOptions.EMPTY);
    }

    public static void setOptions(ItemStack stack, WandModeOptions options) {
        stack.set(ModDataComponents.WAND_OPTIONS, options);
    }

    public static boolean option(ItemStack stack, Mode mode, int bit) {
        return options(stack).option(mode.ordinal(), bit);
    }

    public static void toggleOption(ItemStack stack, Mode mode, int bit) {
        setOptions(stack, options(stack).toggle(mode.ordinal(), bit));
    }

    public static int selection(ItemStack stack) {
        return options(stack).selection(Mode.fromStack(stack).ordinal());
    }

    public static int templateSlot(ItemStack stack) {
        return Mth.clamp(selection(stack), 0, MAX_TEMPLATE_SLOTS - 1);
    }

    public static boolean replaceExisting(ItemStack stack) {
        return option(stack, Mode.fromStack(stack), WandModeOptions.REPLACE_EXISTING);
    }

    public static boolean affectEntities(ItemStack stack) {
        return option(stack, Mode.fromStack(stack), WandModeOptions.AFFECT_ENTITIES);
    }

    public static boolean mirrored(ItemStack stack) {
        return option(stack, Mode.fromStack(stack), WandModeOptions.MIRRORED);
    }

    public static int secondarySelection(ItemStack stack) {
        return options(stack).secondarySelection(Mode.fromStack(stack).ordinal());
    }

    public static Direction areaFacing(ItemStack stack) {
        return options(stack).facing(Mode.fromStack(stack).ordinal());
    }

    public static Mirror mirror(ItemStack stack) {
        int ordinal = options(stack).mirrorOrdinal(Mode.fromStack(stack).ordinal());
        Mirror[] values = Mirror.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return Mirror.NONE;
        }
        return values[ordinal];
    }

    public static Direction flipAxis(ItemStack stack) {
        Direction facing = areaFacing(stack);
        return facing != null ? facing : Direction.UP;
    }

    public static int maxAreaDimension(Player player) {
        return player.getAbilities().instabuild ? 256 : 64;
    }

    public static boolean areaWithinLimit(Player player, BlockPos start, BlockPos end) {
        int limit = maxAreaDimension(player);
        return Math.abs(end.getX() - start.getX()) <= limit
                && Math.abs(end.getY() - start.getY()) <= limit
                && Math.abs(end.getZ() - start.getZ()) <= limit;
    }

    public static List<BlockPos> previewPositions(ItemStack wand, Level level, BlockPos origin, Direction face) {
        Mode mode = Mode.fromStack(wand);
        if (origin == null || face == null) {
            return List.of();
        }
        if (mode == Mode.WALLS || mode == Mode.CUBE) {
            BlockPos end = wand.get(ModDataComponents.RULER_END);
            if (end == null) {
                return List.of();
            }
            return cubePositions(origin, end, mode == Mode.WALLS);
        }
        return areaPositions(wand, level, origin, face);
    }

    public static boolean tooFar(Player player, BlockPos pos) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 160 * 160;
    }

    public static boolean hasEnoughCharge(ItemStack stack, Player player) {
        return player.getAbilities().instabuild || ModularHelper.useEnderCharge(stack, ENDER_CHARGE_COST, true);
    }

    public static void advanceStart(ItemStack wand, Direction face) {
        Mode mode = Mode.fromStack(wand);
        if (mode == Mode.WALLS || mode == Mode.CUBE || mode == Mode.REPLACE) {
            return;
        }
        BlockPos start = wand.get(ModDataComponents.RULER_START);
        if (start != null && face != null) {
            wand.set(ModDataComponents.RULER_START, start.relative(face));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        if (isInventoryTarget(level, pos, side)) {
            return super.useOn(context);
        }
        Mode mode = Mode.fromStack(stack);
        if (mode.hasTwoPlacableCorners()) {
            if (!level.isClientSide) {
                if (mode == Mode.REPLACE_3D && player.isShiftKeyDown() && option(stack, mode, WandModeOptions.BIND_MODE)) {
                    bindSelectedBlock(stack, level, pos, player, true);
                } else {
                    BlockPos stored = player.isShiftKeyDown() ? pos : pos.relative(side);
                    setCorner(stack, stored, false, player);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown() && side == Direction.UP && mode != Mode.REPLACE) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            BlockPos target = mode == Mode.REPLACE ? pos : pos.relative(side);
            return executeWand(new UseOnContext(player, context.getHand(),
                    new BlockHitResult(context.getClickLocation(), side, target, false)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && cancelRunningTask(player)) {
            return ItemEnderUtilities.handledWithoutUseAnim(stack, false);
        }
        if (stack.get(ModDataComponents.RULER_START) == null) {
            return super.use(level, player, hand);
        }
        Mode mode = Mode.fromStack(stack);
        if (!level.isClientSide && !mode.hasUseDelay()) {
            BlockPos start = stack.get(ModDataComponents.RULER_START);
            executeWand(new UseOnContext(player, hand,
                    new BlockHitResult(Vec3.atCenterOf(start), Direction.UP, start, false)));
            return ItemEnderUtilities.handledWithoutUseAnim(stack, false);
        }
        if (mode.hasUseDelay() && stack.get(ModDataComponents.RULER_END) != null) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    private boolean cancelRunningTask(Player player) {
        boolean removed = false;
        if (PlayerTaskScheduler.getInstance().hasTask(player, TaskBuildersWand.class)) {
            PlayerTaskScheduler.getInstance().removeTask(player, TaskBuildersWand.class);
            removed = true;
        }
        if (PlayerTaskScheduler.getInstance().hasTask(player, TaskTemplatePlaceBlocks.class)) {
            PlayerTaskScheduler.getInstance().removeTask(player, TaskTemplatePlaceBlocks.class);
            removed = true;
        }
        return removed;
    }

    private static boolean isInventoryTarget(Level level, BlockPos pos, Direction side) {
        if (level.getBlockState(pos).getBlock() instanceof EnderChestBlock) {
            return true;
        }
        return InventoryHelper.inventoryAt(level, pos, side) != null;
    }

    private void setCorner(ItemStack stack, BlockPos pos, boolean start, Player player) {
        Mode mode = Mode.fromStack(stack);
        if (!start && (mode == Mode.PASTE || mode == Mode.MOVE_DST || mode == Mode.REPLACE)) {
            return;
        }
        if (mode == Mode.REPLACE && !option(stack, mode, WandModeOptions.REPLACE_MODE_IS_AREA)) {
            return;
        }
        var key = start ? ModDataComponents.RULER_START : ModDataComponents.RULER_END;
        BlockPos old = stack.get(key);
        if (old != null && old.equals(pos)) {
            stack.remove(key);
        } else {
            stack.set(key, pos.immutable());
        }
        if (mode.isAreaMode()) {
            int bits = options(stack).optionBits(mode.ordinal()) & ~WandModeOptions.MIRRORED;
            WandModeOptions next = options(stack).withOptionBits(mode.ordinal(), bits)
                    .withMirror(mode.ordinal(), Mirror.NONE.ordinal());
            BlockPos startPos = stack.get(ModDataComponents.RULER_START);
            BlockPos endPos = stack.get(ModDataComponents.RULER_END);
            if (startPos != null && endPos != null) {
                next = next.withFacing(mode.ordinal(), facingFrom(startPos, endPos));
            }
            setOptions(stack, next);
        }
    }

    private static Direction facingFrom(BlockPos start, BlockPos end) {
        int dx = end.getX() - start.getX();
        int dz = end.getZ() - start.getZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private boolean debounce(Player player, Level level) {
        Long last = lastLeftClick.get(player.getUUID());
        long now = level.getGameTime();
        if (last != null && now - last < 4) {
            lastLeftClick.put(player.getUUID(), now);
            return true;
        }
        lastLeftClick.put(player.getUUID(), now);
        return false;
    }

    public void onLeftClickBlock(Player player, Level level, ItemStack stack, BlockPos pos, Direction face) {
        if (level.isClientSide || debounce(player, level)) {
            return;
        }
        Mode mode = Mode.fromStack(stack);
        if (player.isShiftKeyDown() && (!mode.isAreaMode()
                || (mode == Mode.REPLACE_3D && option(stack, mode, WandModeOptions.BIND_MODE)))) {
            bindSelectedBlock(stack, level, pos, player, false);
            return;
        }
        if (mode == Mode.REPLACE) {
            if (option(stack, mode, WandModeOptions.REPLACE_MODE_IS_AREA)) {
                setCorner(stack, pos, true, player);
            }
            return;
        }
        BlockPos stored = player.isShiftKeyDown() ? pos : pos.relative(face);
        setCorner(stack, stored, true, player);
    }

    private static void bindSelectedBlock(ItemStack wand, Level level, BlockPos pos, Player player, boolean secondary) {
        int sel = secondary ? secondarySelection(wand) : selection(wand);
        if (sel < 0) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        ItemStack picked = new ItemStack(state.getBlock());
        if (picked.isEmpty()) {
            return;
        }
        if (secondary) {
            setOptions(wand, options(wand).withBoundBlock(sel, picked, state)
                    .withSecondarySelection(Mode.fromStack(wand).ordinal(), sel));
        } else {
            setOptions(wand, options(wand).withBoundBlock(sel, picked, state));
        }
        player.displayClientMessage(Component.translatable("enderutilities.tooltip.item.block").append(": ")
                .append(picked.getHoverName()), true);
    }

    private static List<BlockPos> cubePositions(BlockPos start, BlockPos end, boolean wallsOnly) {
        int minX = Math.min(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxX = Math.max(start.getX(), end.getX());
        int maxY = Math.max(start.getY(), end.getY());
        int maxZ = Math.max(start.getZ(), end.getZ());
        if (maxX - minX > 128 || maxY - minY > 128 || maxZ - minZ > 128) {
            return List.of();
        }
        List<BlockPos> positions = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    if (wallsOnly && !edge) {
                        continue;
                    }
                    positions.add(new BlockPos(x, y, z));
                    if (positions.size() >= 4096) {
                        return positions;
                    }
                }
            }
        }
        return positions;
    }

    public void placeHelperBlock(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        Vec3 look = player.getLookAngle();
        BlockPos pos = BlockPos.containing(player.getEyePosition().add(look.scale(2.0)));
        player.level().setBlock(pos, Blocks.SPRUCE_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false)
                .setValue(LeavesBlock.DISTANCE, 1), 3);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide || !(living instanceof Player player)) {
            return;
        }
        if (getUseDuration(stack, living) - timeLeft < 20) {
            return;
        }
        BlockPos end = stack.get(ModDataComponents.RULER_END);
        if (end == null) {
            end = stack.get(ModDataComponents.RULER_START);
        }
        if (end == null) {
            return;
        }
        InteractionResult result = executeWand(new UseOnContext(player, player.getUsedItemHand(),
                new BlockHitResult(Vec3.atCenterOf(end), Direction.UP, end, false)));
        if (result == InteractionResult.CONSUME || result == InteractionResult.SUCCESS) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 0.4f, 0.7f);
        }
    }

    private InteractionResult executeWand(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Mode mode = Mode.fromStack(stack);
        if (tooFar(player, context.getClickedPos())) {
            player.displayClientMessage(Component.translatable("enderutilities.chat.message.areatoofar"), true);
            return InteractionResult.FAIL;
        }
        if (!hasEnoughCharge(stack, player)) {
            player.displayClientMessage(Component.translatable("enderutilities.chat.message.notenoughendercharge"), true);
            return InteractionResult.FAIL;
        }
        if (PlayerTaskScheduler.getInstance().hasTask(player, TaskBuildersWand.class)
                || PlayerTaskScheduler.getInstance().hasTask(player, TaskTemplatePlaceBlocks.class)) {
            PlayerTaskScheduler.getInstance().removeTask(player, TaskBuildersWand.class);
            PlayerTaskScheduler.getInstance().removeTask(player, TaskTemplatePlaceBlocks.class);
            return InteractionResult.CONSUME;
        }
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        ItemStack placeStack = placeStack(player, stack, context.getLevel(), clicked);
        BlockState replace = context.getLevel().getBlockState(clicked);
        switch (mode) {
            case COLUMN -> placeColumn(context, placeStack, clicked.relative(face), face);
            case LINE -> placeLine(context, placeStack);
            case PLANE, EXTEND_AREA, EXTEND_CONTINUOUS -> {
                List<BlockPos> flood = floodPlane(context.getLevel(), stack, clicked.relative(face), face);
                if (flood.size() > 1 || mode != Mode.PLANE) {
                    schedulePlaces(context, placeStack, flood, replaceExisting(stack), null);
                } else {
                    placePlane(context, placeStack, clicked, face);
                }
            }
            case CIRCLE -> placeCircle(context, placeStack, clicked.relative(face), face);
            case WALLS -> placeCube(context, placeStack, false, true);
            case CUBE -> placeCube(context, placeStack, false, false);
            case REPLACE, REPLACE_3D -> replaceArea(context, placeStack, replace, mode == Mode.REPLACE_3D);
            case COPY, MOVE_SRC -> copyTemplate(context, stack, mode == Mode.MOVE_SRC);
            case PASTE, MOVE_DST -> pasteTemplate(context, stack, mode == Mode.MOVE_DST);
            case STACK -> stackTemplate(context, stack, placeStack);
            case DELETE -> {
                if (!player.getAbilities().instabuild) {
                    player.displayClientMessage(Component.translatable("enderutilities.chat.message.creativeonly"), true);
                    return InteractionResult.FAIL;
                }
                placeCube(context, ItemStack.EMPTY, true, false);
                if (affectEntities(stack)) {
                    removeAreaEntities(context);
                }
            }
            default -> schedulePlaces(context, placeStack, List.of(clicked.relative(face)), false, null);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 600;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    public static ItemStack placeStack(Player player, ItemStack wand) {
        return placeStack(player, wand, player.level(), player.blockPosition());
    }

    public static ItemStack placeStack(Player player, ItemStack wand, Level level, BlockPos targeted) {
        int sel = selection(wand);
        if (sel == WandModeOptions.BLOCK_TYPE_TARGETED) {
            ItemStack picked = new ItemStack(level.getBlockState(targeted).getBlock());
            if (!picked.isEmpty() && picked.getItem() instanceof BlockItem) {
                return findMatchingBlock(player, level.getBlockState(targeted));
            }
        }
        if (sel >= 0) {
            ItemStack bound = options(wand).boundBlock(sel);
            if (!bound.isEmpty() && bound.getItem() instanceof BlockItem blockItem) {
                ItemStack found = findMatchingBlock(player, blockItem.getBlock().defaultBlockState());
                return found.isEmpty() ? (player.getAbilities().instabuild ? bound.copy() : ItemStack.EMPTY) : found;
            }
            return ItemStack.EMPTY;
        }
        if (player.getInventory().getSelected() == wand) {
            return adjacentBlock(player);
        }
        return ItemStack.EMPTY;
    }

    private static void placeColumn(UseOnContext context, ItemStack placeStack, BlockPos start, Direction face) {
        ItemStack wand = context.getItemInHand();
        Mode mode = Mode.fromStack(wand);
        boolean continueThrough = option(wand, mode, WandModeOptions.CONTINUE_THROUGH);
        int length = options(wand).rPosH(mode.ordinal());
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i <= length; i++) {
            BlockPos pos = start.relative(face, i);
            boolean air = context.getLevel().getBlockState(pos).canBeReplaced();
            if (!air && !replaceExisting(wand) && !continueThrough) {
                break;
            }
            if (air || replaceExisting(wand)) {
                positions.add(pos);
            }
        }
        schedulePlaces(context, placeStack, positions, replaceExisting(wand), null);
    }

    private static BlockState boundPlacementState(ItemStack wand, Level level, ItemStack placeStack) {
        int sel = selection(wand);
        if (sel < 0 || level == null) {
            return null;
        }
        return options(wand).boundState(sel, level.registryAccess()).orElse(null);
    }

    private static void changeAreaDimensions(Player player, ItemStack stack, int amount) {
        Mode mode = Mode.fromStack(stack);
        if (stack.get(ModDataComponents.RULER_START) == null || mode == Mode.PASTE || mode == Mode.MOVE_DST) {
            return;
        }
        WandModeOptions opts = options(stack);
        int m = mode.ordinal();
        if (mode == Mode.STACK) {
            Direction look = Math.abs(player.getXRot()) > 40.0F
                    ? (player.getXRot() < 0.0F ? Direction.UP : Direction.DOWN)
                    : player.getDirection();
            setOptions(stack, opts.adjustStackArea(look, amount));
            return;
        }
        if (mode == Mode.COLUMN || mode == Mode.CIRCLE) {
            setOptions(stack, opts.adjustColumn(m, amount));
            return;
        }
        if (Math.abs(player.getXRot()) > 20.0F) {
            boolean up = player.getXRot() < 0.0F;
            setOptions(stack, opts.withArea(m, opts.rPosH(m), opts.rNegH(m),
                    up ? opts.rPosV(m) + amount : opts.rPosV(m),
                    up ? opts.rNegV(m) : opts.rNegV(m) + amount));
            return;
        }
        setOptions(stack, opts.withArea(m, opts.rPosH(m) + amount, opts.rNegH(m), opts.rPosV(m), opts.rNegV(m)));
    }

    public static List<BlockPos> ghostPositions(ItemStack wand, Level level, BlockPos origin, Direction face) {
        if (!option(wand, Mode.fromStack(wand), WandModeOptions.RENDER_GHOST) || origin == null || face == null) {
            return List.of();
        }
        return areaPositions(wand, level, origin, face);
    }

    public static List<BlockPos> areaPositions(ItemStack wand, Level level, BlockPos origin, Direction face) {
        Mode mode = Mode.fromStack(wand);
        if (origin == null || face == null) {
            return List.of();
        }
        WandModeOptions opts = options(wand);
        int m = mode.ordinal();
        List<BlockPos> positions = new ArrayList<>();
        if (mode == Mode.COLUMN) {
            for (int i = 0; i <= opts.rPosH(m); i++) {
                positions.add(origin.relative(face, i));
            }
        } else if (mode == Mode.LINE) {
            Direction right = face.getAxis().isVertical() ? Direction.EAST : face.getClockWise();
            if (option(wand, Mode.LINE, WandModeOptions.AREA_FLIPPED)) {
                right = right.getOpposite();
            }
            for (int i = -opts.rNegH(m); i <= opts.rPosH(m); i++) {
                positions.add(origin.relative(right, i));
            }
        } else if (mode == Mode.PLANE || mode == Mode.EXTEND_AREA || mode == Mode.EXTEND_CONTINUOUS) {
            return floodPlane(level, wand, origin, face);
        } else if (mode == Mode.CIRCLE) {
            return circlePositions(wand, level, origin, face);
        }
        return positions;
    }

    public static BlockState ghostBlockState(ItemStack wand, Level level, BlockPos origin) {
        BlockState bound = boundPlacementState(wand, level, ItemStack.EMPTY);
        if (bound != null && !bound.isAir()) {
            return bound;
        }
        if (origin != null && level != null) {
            BlockState behind = level.getBlockState(origin);
            if (!behind.isAir()) {
                return behind;
            }
        }
        return null;
    }

    private static List<BlockPos> floodPlane(Level level, ItemStack wand, BlockPos origin, Direction face) {
        Mode mode = Mode.fromStack(wand);
        WandModeOptions opts = options(wand);
        int m = mode.ordinal();
        Direction right = face.getAxis().isVertical() ? Direction.EAST : face.getClockWise();
        Direction up = face.getAxis().isVertical() ? Direction.SOUTH : Direction.UP;
        if (option(wand, mode, WandModeOptions.AREA_FLIPPED)) {
            right = right.getOpposite();
        }
        BlockPos a = origin.relative(right, -opts.rNegH(m)).relative(up, -opts.rNegV(m));
        BlockPos b = origin.relative(right, opts.rPosH(m)).relative(up, opts.rPosV(m));
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        boolean diagonals = mode == Mode.EXTEND_CONTINUOUS && option(wand, mode, WandModeOptions.ALLOW_DIAGONALS);
        boolean continueThrough = mode != Mode.EXTEND_CONTINUOUS && option(wand, mode, WandModeOptions.CONTINUE_THROUGH);
        int sel = selection(wand);
        BlockState targeted = level != null ? level.getBlockState(origin.relative(face.getOpposite())) : null;
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> positions = new ArrayList<>();
        List<BlockPos> branches = new ArrayList<>();
        BlockPos pos = origin;
        int branchIndex = 0;
        int counter = 0;
        while (counter <= 16641) {
            BlockPos next = checkFlood(level, wand, pos, face, min, max, visited, branches, positions,
                    continueThrough, diagonals, targeted, sel, mode);
            counter++;
            if (next == null) {
                if (branchIndex < branches.size()) {
                    pos = branches.get(branchIndex++);
                } else {
                    break;
                }
            } else {
                pos = next;
            }
        }
        return positions;
    }

    private static BlockPos checkFlood(
            Level level,
            ItemStack wand,
            BlockPos posIn,
            Direction face,
            BlockPos min,
            BlockPos max,
            Set<BlockPos> visited,
            List<BlockPos> branches,
            List<BlockPos> positions,
            boolean continueThrough,
            boolean diagonals,
            BlockState targeted,
            int sel,
            Mode mode
    ) {
        if (visited.contains(posIn) || !inside(posIn, min, max)) {
            return null;
        }
        boolean replaceable = level == null || level.getBlockState(posIn).canBeReplaced();
        if (replaceable) {
            if (mode == Mode.EXTEND_AREA || mode == Mode.EXTEND_CONTINUOUS) {
                BlockPos back = posIn.relative(face.getOpposite());
                BlockState behind = level == null ? targeted : level.getBlockState(back);
                boolean solid = behind != null && !behind.isAir() && behind.getFluidState().isEmpty();
                boolean match = sel == WandModeOptions.BLOCK_TYPE_ADJACENT
                        || sel >= 0
                        || (targeted != null && behind != null && behind.getBlock() == targeted.getBlock());
                if (solid && match) {
                    positions.add(posIn);
                } else if (mode == Mode.EXTEND_CONTINUOUS) {
                    return null;
                }
            } else {
                positions.add(posIn);
            }
        } else if (!continueThrough) {
            return null;
        }
        visited.add(posIn);
        BlockPos continueTo = null;
        int sides = 0;
        for (BlockPos adj : planeNeighbors(posIn, face, diagonals)) {
            if (visited.contains(adj) || !inside(adj, min, max)) {
                continue;
            }
            boolean open = level == null || level.getBlockState(adj).canBeReplaced() || continueThrough;
            if (!open) {
                continue;
            }
            if (sides == 0) {
                continueTo = adj;
            } else if (!branches.contains(adj)) {
                branches.add(adj);
            }
            sides++;
        }
        return continueTo;
    }

    private static boolean inside(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    private static List<BlockPos> planeNeighbors(BlockPos pos, Direction face, boolean diagonals) {
        List<BlockPos> next = new ArrayList<>();
        Direction.Axis skip = face.getAxis();
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == skip) {
                continue;
            }
            next.add(pos.relative(dir));
        }
        if (diagonals) {
            Direction[] planar = new Direction[4];
            int i = 0;
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() != skip) {
                    planar[i++] = dir;
                }
            }
            for (int a = 0; a < planar.length; a++) {
                for (int b = a + 1; b < planar.length; b++) {
                    if (planar[a].getAxis() != planar[b].getAxis()) {
                        next.add(pos.relative(planar[a]).relative(planar[b]));
                    }
                }
            }
        }
        return next;
    }

    private static void placeLine(UseOnContext context, ItemStack placeStack) {
        ItemStack wand = context.getItemInHand();
        BlockPos start = wand.get(ModDataComponents.RULER_START);
        BlockPos end = context.getClickedPos();
        if (start == null) {
            Direction face = context.getClickedFace();
            Direction right = face.getAxis().isVertical() ? Direction.EAST : face.getClockWise();
            if (option(wand, Mode.LINE, WandModeOptions.AREA_FLIPPED)) {
                right = right.getOpposite();
            }
            WandModeOptions opts = options(wand);
            List<BlockPos> positions = new ArrayList<>();
            BlockPos origin = end.relative(face);
            for (int i = -opts.rNegH(Mode.LINE.ordinal()); i <= opts.rPosH(Mode.LINE.ordinal()); i++) {
                positions.add(origin.relative(right, i));
            }
            schedulePlaces(context, placeStack, positions, replaceExisting(wand), null);
            return;
        }
        Direction axis = primaryAxis(start, end);
        int length = switch (axis.getAxis()) {
            case X -> Math.abs(end.getX() - start.getX());
            case Y -> Math.abs(end.getY() - start.getY());
            case Z -> Math.abs(end.getZ() - start.getZ());
        };
        List<BlockPos> positions = new ArrayList<>();
        boolean continueThrough = option(context.getItemInHand(), Mode.fromStack(context.getItemInHand()), WandModeOptions.CONTINUE_THROUGH);
        for (int i = 0; i <= length && i < 256; i++) {
            BlockPos pos = start.relative(axis, i);
            boolean air = context.getLevel().getBlockState(pos).canBeReplaced();
            if (!air && !replaceExisting(context.getItemInHand()) && !continueThrough) {
                if (i > 0) {
                    break;
                }
            }
            if (air || replaceExisting(context.getItemInHand())) {
                positions.add(pos);
            }
        }
        schedulePlaces(context, placeStack, positions, replaceExisting(context.getItemInHand()), null);
    }

    private static Direction primaryAxis(BlockPos start, BlockPos end) {
        int dx = Math.abs(end.getX() - start.getX());
        int dy = Math.abs(end.getY() - start.getY());
        int dz = Math.abs(end.getZ() - start.getZ());
        if (dx >= dy && dx >= dz) {
            return end.getX() >= start.getX() ? Direction.EAST : Direction.WEST;
        }
        if (dy >= dz) {
            return end.getY() >= start.getY() ? Direction.UP : Direction.DOWN;
        }
        return end.getZ() >= start.getZ() ? Direction.SOUTH : Direction.NORTH;
    }

    private static void placePlane(UseOnContext context, ItemStack placeStack, BlockPos clicked, Direction face) {
        BlockPos start = context.getItemInHand().getOrDefault(ModDataComponents.RULER_START, clicked);
        if (start.equals(clicked)) {
            List<BlockPos> area = areaPositions(context.getItemInHand(), context.getLevel(), clicked.relative(face), face);
            if (area.size() > 1) {
                schedulePlaces(context, placeStack, area, replaceExisting(context.getItemInHand()), null);
                return;
            }
        }
        int minX = Math.min(start.getX(), clicked.getX());
        int minY = Math.min(start.getY(), clicked.getY());
        int minZ = Math.min(start.getZ(), clicked.getZ());
        int maxX = Math.max(start.getX(), clicked.getX());
        int maxY = Math.max(start.getY(), clicked.getY());
        int maxZ = Math.max(start.getZ(), clicked.getZ());
        Direction.Axis axis = face.getAxis();
        List<BlockPos> positions = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (axis == Direction.Axis.X && x != clicked.getX()) {
                        continue;
                    }
                    if (axis == Direction.Axis.Y && y != clicked.getY()) {
                        continue;
                    }
                    if (axis == Direction.Axis.Z && z != clicked.getZ()) {
                        continue;
                    }
                    positions.add(new BlockPos(x, y, z).relative(face));
                    if (positions.size() >= 4096) {
                        schedulePlaces(context, placeStack, positions, replaceExisting(context.getItemInHand()), null);
                        return;
                    }
                }
            }
        }
        schedulePlaces(context, placeStack, positions, replaceExisting(context.getItemInHand()), null);
    }

    private static void placeCube(UseOnContext context, ItemStack placeStack, boolean delete, boolean wallsOnly) {
        BlockPos start = context.getItemInHand().get(ModDataComponents.RULER_START);
        BlockPos end = context.getClickedPos();
        if (start == null) {
            return;
        }
        int minX = Math.min(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxX = Math.max(start.getX(), end.getX());
        int maxY = Math.max(start.getY(), end.getY());
        int maxZ = Math.max(start.getZ(), end.getZ());
        List<BlockPos> positions = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    if (wallsOnly && !edge) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    if (delete) {
                        context.getLevel().destroyBlock(pos, false);
                    } else {
                        positions.add(pos);
                    }
                    if (positions.size() >= 4096) {
                        schedulePlaces(context, placeStack, positions, replaceExisting(context.getItemInHand()), null);
                        return;
                    }
                }
            }
        }
        if (!delete) {
            schedulePlaces(context, placeStack, positions, replaceExisting(context.getItemInHand()), null);
        }
    }

    private static void replaceArea(UseOnContext context, ItemStack placeStack, BlockState match, boolean threeD) {
        ItemStack wand = context.getItemInHand();
        if (Mode.fromStack(wand) == Mode.REPLACE && !option(wand, Mode.REPLACE, WandModeOptions.REPLACE_MODE_IS_AREA)) {
            schedulePlaces(context, placeStack, List.of(context.getClickedPos()), true, match);
            return;
        }
        BlockPos start = wand.getOrDefault(ModDataComponents.RULER_START, context.getClickedPos());
        BlockPos end = context.getClickedPos();
        int minX = Math.min(start.getX(), end.getX());
        int minY = threeD ? Math.min(start.getY(), end.getY()) : end.getY();
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxX = Math.max(start.getX(), end.getX());
        int maxY = threeD ? Math.max(start.getY(), end.getY()) : end.getY();
        int maxZ = Math.max(start.getZ(), end.getZ());
        List<BlockPos> positions = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (context.getLevel().getBlockState(pos).is(match.getBlock())) {
                        positions.add(pos);
                    }
                    if (positions.size() >= 4096) {
                        schedulePlaces(context, placeStack, positions, true, match);
                        return;
                    }
                }
            }
        }
        schedulePlaces(context, placeStack, positions, true, match);
    }

    private static void placeCircle(UseOnContext context, ItemStack placeStack, BlockPos origin, Direction face) {
        ItemStack wand = context.getItemInHand();
        List<BlockPos> positions = circlePositions(wand, context.getLevel(), origin, face);
        schedulePlaces(context, placeStack, positions, replaceExisting(wand), null);
    }

    /**
     * Filled Euclidean disk (or 4-connected outline when AREA_FLIPPED) in the plane of the
     * clicked face. Radius is the Column-style rPosH area value.
     */
    public static List<BlockPos> circlePositions(ItemStack wand, Level level, BlockPos origin, Direction face) {
        WandModeOptions opts = options(wand);
        int radius = opts.rPosH(Mode.CIRCLE.ordinal());
        boolean outline = option(wand, Mode.CIRCLE, WandModeOptions.AREA_FLIPPED);
        boolean continueThrough = option(wand, Mode.CIRCLE, WandModeOptions.CONTINUE_THROUGH);
        boolean replace = replaceExisting(wand);
        Direction right = face.getAxis().isVertical() ? Direction.EAST : face.getClockWise();
        Direction up = face.getAxis().isVertical() ? Direction.SOUTH : Direction.UP;
        List<BlockPos> positions = new ArrayList<>();
        int radiusSq = radius * radius;
        for (int h = -radius; h <= radius; h++) {
            for (int v = -radius; v <= radius; v++) {
                int distSq = h * h + v * v;
                if (distSq > radiusSq) {
                    continue;
                }
                if (outline && radius > 0) {
                    boolean onRing = (h + 1) * (h + 1) + v * v > radiusSq
                            || (h - 1) * (h - 1) + v * v > radiusSq
                            || h * h + (v + 1) * (v + 1) > radiusSq
                            || h * h + (v - 1) * (v - 1) > radiusSq;
                    if (!onRing) {
                        continue;
                    }
                }
                BlockPos pos = origin.relative(right, h).relative(up, v);
                if (includeCirclePos(level, pos, continueThrough, replace)) {
                    positions.add(pos);
                }
                if (positions.size() >= 16641) {
                    return positions;
                }
            }
        }
        return positions;
    }

    private static boolean includeCirclePos(Level level, BlockPos pos, boolean continueThrough, boolean replace) {
        if (level == null) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (state.canBeReplaced() || replace) {
            return true;
        }
        return continueThrough;
    }

    private static void schedulePlaces(
            UseOnContext context,
            ItemStack placeStack,
            List<BlockPos> positions,
            boolean replace,
            BlockState match
    ) {
        Player player = context.getPlayer();
        if (player == null || positions.isEmpty()) {
            return;
        }
        ItemStack wand = context.getItemInHand();
        Mode mode = Mode.fromStack(wand);
        boolean move = option(wand, mode, WandModeOptions.MOVE_POSITION);
        Direction face = context.getClickedFace();
        if (positions.size() <= INSTANT_PLACE_LIMIT) {
            for (BlockPos pos : positions) {
                tryPlace(context.getLevel(), player, wand, placeStack, pos, face, replace, match);
            }
            if (move) {
                advanceStart(wand, face);
            }
            return;
        }
        PlayerTaskScheduler.getInstance().addTask(player, new TaskBuildersWand(positions, replace, match, move, face));
    }

    public static boolean tryPlace(
            Level level,
            Player player,
            ItemStack wand,
            ItemStack placeStack,
            BlockPos pos,
            Direction face,
            boolean replace,
            BlockState match
    ) {
        if (player == null) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        if (replace) {
            if (match != null && !existing.is(match.getBlock())) {
                return false;
            }
        } else if (!existing.canBeReplaced() && !replaceExisting(wand)) {
            return false;
        }
        if (!(placeStack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        if (!hasEnoughCharge(wand, player)) {
            return false;
        }
        BlockState state = boundPlacementState(wand, level, placeStack);
        if (state == null) {
            BlockPlaceContext place = new BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, placeStack,
                    new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), face == null ? Direction.UP : face, pos, false));
            state = blockItem.getBlock().getStateForPlacement(place);
        }
        if (state == null || !level.setBlock(pos, state, 3)) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            placeStack.shrink(1);
            ModularHelper.useEnderCharge(wand, ENDER_CHARGE_COST, false);
        }
        return true;
    }

    private static ItemStack adjacentBlock(Player player) {
        int slot = player.getInventory().selected;
        if (slot > 0) {
            ItemStack left = player.getInventory().getItem(slot - 1);
            if (left.getItem() instanceof BlockItem) {
                return left;
            }
        }
        if (slot < 8) {
            ItemStack right = player.getInventory().getItem(slot + 1);
            if (right.getItem() instanceof BlockItem) {
                return right;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void copyTemplate(UseOnContext context, ItemStack wand, boolean move) {
        BlockPos start = wand.get(ModDataComponents.RULER_START);
        BlockPos end = context.getClickedPos();
        Player copier = context.getPlayer();
        if (start == null || copier == null) {
            return;
        }
        if (!areaWithinLimit(copier, start, end)) {
            copier.displayClientMessage(Component.translatable(
                    "enderutilities.chat.message.areatoolarge", maxAreaDimension(copier)), true);
            return;
        }
        if (tooFar(copier, start) || tooFar(copier, end)) {
            copier.displayClientMessage(Component.translatable("enderutilities.chat.message.areatoofar"), true);
            return;
        }
        int minX = Math.min(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxX = Math.max(start.getX(), end.getX());
        int maxY = Math.max(start.getY(), end.getY());
        int maxZ = Math.max(start.getZ(), end.getZ());
        List<WandTemplate.Entry> entries = new ArrayList<>();
        for (int x = minX; x <= maxX && entries.size() < 4096; x++) {
            for (int y = minY; y <= maxY && entries.size() < 4096; y++) {
                for (int z = minZ; z <= maxZ && entries.size() < 4096; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = context.getLevel().getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    Optional<net.minecraft.nbt.CompoundTag> nbt = Optional.empty();
                    var be = context.getLevel().getBlockEntity(pos);
                    if (be != null) {
                        nbt = Optional.of(be.saveWithoutMetadata(context.getLevel().registryAccess()));
                    }
                    entries.add(new WandTemplate.Entry(new BlockPos(x - minX, y - minY, z - minZ), state, nbt));
                }
            }
        }
        List<WandTemplate.EntityEntry> entities = affectEntities(wand)
                ? captureEntities(context.getLevel(), minX, minY, minZ, maxX, maxY, maxZ)
                : List.of();
        String existing = wand.getOrDefault(ModDataComponents.WAND_TEMPLATE, WandTemplate.EMPTY).name();
        Direction facing = copier.getDirection();
        var template = new WandTemplate(entries, existing, entities, facing);
        wand.set(ModDataComponents.WAND_TEMPLATE, template);
        if (context.getLevel() instanceof net.minecraft.server.level.ServerLevel server) {
            WandTemplateStore.write(server.getServer(), copier, template, templateSlot(wand));
        }
        if (move) {
            wand.set(ModDataComponents.WAND_MOVE_ORIGIN, new BlockPos(minX, minY, minZ));
            wand.set(ModDataComponents.RULER_END, new BlockPos(maxX, maxY, maxZ));
        }
        copier.displayClientMessage(Component.translatable(
                "enderutilities.chat.message.areasavedtotemplate", templateSlot(wand) + 1), true);
    }

    private static void pasteTemplate(UseOnContext context, ItemStack wand, boolean move) {
        var template = wand.getOrDefault(ModDataComponents.WAND_TEMPLATE, WandTemplate.EMPTY);
        Player player = context.getPlayer();
        if (player != null && context.getLevel() instanceof net.minecraft.server.level.ServerLevel server) {
            template = WandTemplateStore.read(server.getServer(), player, template.name(), templateSlot(wand)).orElse(template);
        }
        if (template.isEmpty() || player == null) {
            return;
        }
        BlockPos dest = context.getClickedPos().relative(context.getClickedFace());
        if (tooFar(player, dest)) {
            player.displayClientMessage(Component.translatable("enderutilities.chat.message.areatoofar"), true);
            return;
        }
        Rotation rotation = rotationBetween(template.facing(), player.getDirection());
        ReplaceMode replace = player.getAbilities().instabuild
                ? (replaceExisting(wand) ? ReplaceMode.EVERYTHING : ReplaceMode.NOTHING)
                : (replaceExisting(wand) ? ReplaceMode.WITH_NON_AIR : ReplaceMode.NOTHING);
        boolean entities = affectEntities(wand);
        boolean mirror = mirrored(wand);
        if (!player.getAbilities().instabuild) {
            PlayerTaskScheduler.getInstance().addTask(player,
                    new TaskTemplatePlaceBlocks(template, dest, rotation, replace, entities, mirror));
        } else {
            for (var entry : template.blocks()) {
                BlockPos pos = dest.offset(transformOffset(entry.offset(), rotation, mirror));
                var state = entry.state();
                if (mirror) {
                    state = state.mirror(Mirror.FRONT_BACK);
                }
                placeTemplateBlock(context.getLevel(), player, wand, pos, state.rotate(rotation), entry.nbt(), replace);
            }
            if (entities) {
                placeTemplateEntities(context.getLevel(), dest, template, rotation, mirror);
            }
        }
        if (move) {
            BlockPos origin = wand.get(ModDataComponents.WAND_MOVE_ORIGIN);
            BlockPos end = wand.get(ModDataComponents.RULER_END);
            if (origin != null && end != null) {
                for (int x = origin.getX(); x <= end.getX(); x++) {
                    for (int y = origin.getY(); y <= end.getY(); y++) {
                        for (int z = origin.getZ(); z <= end.getZ(); z++) {
                            context.getLevel().destroyBlock(new BlockPos(x, y, z), false);
                        }
                    }
                }
            }
            wand.remove(ModDataComponents.WAND_MOVE_ORIGIN);
        }
    }

    private static void stackTemplate(UseOnContext context, ItemStack wand, ItemStack placeStack) {
        copyTemplate(context, wand, false);
        var template = wand.getOrDefault(ModDataComponents.WAND_TEMPLATE, WandTemplate.EMPTY);
        BlockPos start = wand.get(ModDataComponents.RULER_START);
        BlockPos end = context.getClickedPos();
        if (template.isEmpty() || start == null) {
            return;
        }
        int sizeX = Math.abs(end.getX() - start.getX()) + 1;
        int sizeY = Math.abs(end.getY() - start.getY()) + 1;
        int sizeZ = Math.abs(end.getZ() - start.getZ()) + 1;
        BlockPos origin = new BlockPos(Math.min(start.getX(), end.getX()), Math.min(start.getY(), end.getY()),
                Math.min(start.getZ(), end.getZ()));
        WandModeOptions.Area3D area = options(wand).stackArea();
        ReplaceMode replace = replaceExisting(wand) ? ReplaceMode.WITH_NON_AIR : ReplaceMode.NOTHING;
        Player player = context.getPlayer();
        boolean creative = player != null && player.getAbilities().instabuild;
        List<BlockPos> positions = new ArrayList<>();
        for (int y = -area.yn(); y <= area.yp(); y++) {
            for (int x = -area.xn(); x <= area.xp(); x++) {
                for (int z = -area.zn(); z <= area.zp(); z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockPos dest = origin.offset(x * sizeX, y * sizeY, z * sizeZ);
                    for (var entry : template.blocks()) {
                        BlockPos pos = dest.offset(entry.offset());
                        if (creative) {
                            if (canReplaceAt(context.getLevel(), pos, entry.state(), replace)) {
                                context.getLevel().setBlock(pos, entry.state(), 3);
                                applyTemplateNbt(context.getLevel(), pos, entry.nbt());
                            }
                        } else {
                            positions.add(pos);
                        }
                    }
                }
            }
        }
        if (!creative && player != null) {
            schedulePlaces(context, placeStack.isEmpty() ? placeStack(player, wand) : placeStack, positions, replaceExisting(wand), null);
        }
    }

    private static List<WandTemplate.EntityEntry> captureEntities(
            Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ
    ) {
        List<WandTemplate.EntityEntry> entities = new ArrayList<>();
        var box = new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        BlockPos origin = new BlockPos(minX, minY, minZ);
        for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box,
                found -> !(found instanceof Player))) {
            var nbt = new net.minecraft.nbt.CompoundTag();
            if (!entity.save(nbt)) {
                continue;
            }
            entities.add(new WandTemplate.EntityEntry(
                    entity.position().subtract(minX, minY, minZ),
                    entity.blockPosition().subtract(origin),
                    nbt));
        }
        return entities;
    }

    public static Rotation rotationBetween(Direction original, Direction rotated) {
        if (original.getAxis() == Direction.Axis.Y || rotated.getAxis() == Direction.Axis.Y || original == rotated) {
            return Rotation.NONE;
        }
        if (rotated == original.getOpposite()) {
            return Rotation.CLOCKWISE_180;
        }
        return rotated == original.getClockWise() ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
    }

    public static BlockPos rotateOffset(BlockPos offset, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case CLOCKWISE_180 -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> offset;
        };
    }

    public static BlockPos transformOffset(BlockPos offset, Rotation rotation, boolean mirror) {
        BlockPos pos = mirror ? new BlockPos(-offset.getX(), offset.getY(), offset.getZ()) : offset;
        return rotateOffset(pos, rotation);
    }

    public static boolean canReplaceAt(Level level, BlockPos pos, BlockState stateNew, ReplaceMode replace) {
        boolean air = level.getBlockState(pos).canBeReplaced();
        return replace == ReplaceMode.EVERYTHING
                || (replace == ReplaceMode.WITH_NON_AIR && !stateNew.isAir())
                || air;
    }

    public static boolean placeTemplateBlock(
            Level level,
            Player player,
            ItemStack wand,
            BlockPos pos,
            BlockState state,
            Optional<net.minecraft.nbt.CompoundTag> nbt,
            ReplaceMode replace
    ) {
        if (!canReplaceAt(level, pos, state, replace)) {
            return false;
        }
        if (player != null && player.getAbilities().instabuild) {
            if (!level.setBlock(pos, state, 3)) {
                return false;
            }
            applyTemplateNbt(level, pos, nbt);
            return true;
        }
        if (wand != null && player != null && !hasEnoughCharge(wand, player)) {
            return false;
        }
        ItemStack use = findMatchingBlock(player, state);
        if (use.isEmpty() || !(use.getItem() instanceof BlockItem)) {
            return false;
        }
        if (!level.setBlock(pos, state, 3)) {
            return false;
        }
        use.shrink(1);
        if (wand != null && player != null) {
            ModularHelper.useEnderCharge(wand, ENDER_CHARGE_COST, false);
        }
        applyTemplateNbt(level, pos, nbt);
        return true;
    }

    public static void placeTemplateEntities(
            Level level,
            BlockPos dest,
            WandTemplate template,
            Rotation rotation,
            boolean mirror
    ) {
        for (var entry : template.entities()) {
            BlockPos offset = transformOffset(entry.blockPos(), rotation, mirror);
            net.minecraft.world.entity.EntityType.create(entry.nbt(), level).ifPresent(entity -> {
                entity.moveTo(
                        dest.getX() + offset.getX() + 0.5,
                        dest.getY() + offset.getY(),
                        dest.getZ() + offset.getZ() + 0.5,
                        entity.getYRot(),
                        entity.getXRot());
                level.addFreshEntity(entity);
            });
        }
    }

    private static void removeAreaEntities(UseOnContext context) {
        BlockPos start = context.getItemInHand().get(ModDataComponents.RULER_START);
        BlockPos end = context.getClickedPos();
        if (start == null) {
            return;
        }
        var box = new net.minecraft.world.phys.AABB(
                Math.min(start.getX(), end.getX()), Math.min(start.getY(), end.getY()), Math.min(start.getZ(), end.getZ()),
                Math.max(start.getX(), end.getX()) + 1, Math.max(start.getY(), end.getY()) + 1, Math.max(start.getZ(), end.getZ()) + 1);
        for (var entity : context.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box,
                found -> !(found instanceof Player))) {
            entity.discard();
        }
    }

    private static void applyTemplateNbt(Level level, BlockPos pos, Optional<net.minecraft.nbt.CompoundTag> nbt) {
        nbt.ifPresent(tag -> {
            var be = level.getBlockEntity(pos);
            if (be != null) {
                be.loadWithComponents(tag.copy(), level.registryAccess());
            }
        });
    }

    private static ItemStack findMatchingBlock(Player player, BlockState state) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == state.getBlock()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (key == HotKeys.KEYCODE_CUSTOM_1) {
            placeHelperBlock(player);
            return true;
        }
        Mode mode = Mode.fromStack(stack);
        if (EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT_ALT)) {
            if (mode == Mode.REPLACE_3D) {
                int next = secondarySelection(stack) + (EnumKey.keypressActionIsReversed(key) ? -1 : 1);
                next = Math.floorMod(next, MAX_TEMPLATE_SLOTS);
                setOptions(stack, options(stack).withSecondarySelection(mode.ordinal(), next));
                return true;
            }
            if (mode == Mode.STACK) {
                changeAreaDimensions(player, stack, EnumKey.keypressActionIsReversed(key) ? 1 : -1);
                return true;
            }
        }
        if (EnumKey.SCROLL.matches(key, HotKeys.MOD_ALT)) {
            int min = mode.isAreaMode() || mode == Mode.REPLACE ? 0 : WandModeOptions.BLOCK_TYPE_ADJACENT;
            int next = selection(stack) + (EnumKey.keypressActionIsReversed(key) ? -1 : 1);
            int range = MAX_TEMPLATE_SLOTS - min;
            next = min + Math.floorMod(next - min, range);
            setOptions(stack, options(stack).withSelection(mode.ordinal(), next));
            return true;
        }
        if (EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT)) {
            if (mode != Mode.PASTE && mode != Mode.MOVE_DST) {
                if (mode.hasTwoPlacableCorners()) {
                    moveEndPosition(stack, player, EnumKey.keypressActionIsReversed(key));
                } else {
                    changeAreaDimensions(player, stack, EnumKey.keypressActionIsReversed(key) ? 1 : -1);
                }
                return true;
            }
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT)) {
            if (mode == Mode.REPLACE_3D) {
                toggleOption(stack, mode, WandModeOptions.BIND_MODE);
                return true;
            }
            if (mode.isAreaMode()) {
                toggleMirror(stack, player);
                return true;
            }
            toggleOption(stack, mode, WandModeOptions.MOVE_POSITION);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL)) {
            Mode.cycle(stack, EnumKey.keypressActionIsReversed(key) || EnumKey.keypressContainsShift(key), player);
            return true;
        }
        if (EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT_CTRL_ALT)) {
            return ModularHelper.changeSelectedModule(stack, ModuleType.LINKCRYSTAL, EnumKey.keypressActionIsReversed(key));
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_CTRL_ALT)) {
            if (mode == Mode.COPY) {
                toggleOption(stack, mode, WandModeOptions.CHISELS_AND_BITS_CROSSWORLD);
                return true;
            }
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL_ALT)) {
            if (mode == Mode.PASTE || mode == Mode.MOVE_DST || mode == Mode.STACK) {
                toggleOption(stack, mode, WandModeOptions.REPLACE_EXISTING);
                return true;
            }
            if (mode == Mode.DELETE) {
                toggleOption(stack, mode, WandModeOptions.AFFECT_ENTITIES);
                return true;
            }
            if (mode == Mode.EXTEND_CONTINUOUS || mode == Mode.REPLACE) {
                toggleOption(stack, mode, WandModeOptions.ALLOW_DIAGONALS);
                return true;
            }
            if (mode == Mode.COLUMN || mode == Mode.LINE || mode == Mode.PLANE || mode == Mode.EXTEND_AREA
                    || mode == Mode.CIRCLE) {
                toggleOption(stack, mode, WandModeOptions.CONTINUE_THROUGH);
                return true;
            }
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_ALT)) {
            if (mode == Mode.PASTE || mode == Mode.STACK) {
                toggleOption(stack, mode, WandModeOptions.AFFECT_ENTITIES);
                return true;
            }
            if (!mode.isAreaMode()) {
                toggleOption(stack, mode, WandModeOptions.RENDER_GHOST);
                return true;
            }
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE)) {
            if (mode == Mode.REPLACE) {
                toggleOption(stack, mode, WandModeOptions.REPLACE_MODE_IS_AREA);
                return true;
            }
            if (mode.isAreaMode() && mode != Mode.COPY) {
                Direction look = Direction.orderedByNearest(player)[0];
                setOptions(stack, options(stack).withFacing(mode.ordinal(), look));
                return true;
            }
            if (!mode.isAreaMode()) {
                toggleOption(stack, mode, WandModeOptions.AREA_FLIPPED);
                Direction look = Direction.orderedByNearest(player)[0];
                setOptions(stack, options(stack).withFacing(mode.ordinal(), look));
                return true;
            }
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_ALT)
                && (mode == Mode.COLUMN || mode == Mode.LINE || mode == Mode.PLANE || mode == Mode.EXTEND_AREA
                        || mode == Mode.CIRCLE)) {
            toggleOption(stack, mode, WandModeOptions.REPLACE_EXISTING);
            return true;
        }
        return super.doKeyBindingAction(player, stack, key);
    }

    private static void toggleMirror(ItemStack stack, Player player) {
        Mode mode = Mode.fromStack(stack);
        Mirror next = player.getDirection().getAxis() == Direction.Axis.Z ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
        if (mirrored(stack) && mirror(stack) == next) {
            next = Mirror.NONE;
        }
        int bits = options(stack).optionBits(mode.ordinal());
        if (next == Mirror.NONE) {
            bits &= ~WandModeOptions.MIRRORED;
        } else {
            bits |= WandModeOptions.MIRRORED;
        }
        setOptions(stack, options(stack).withOptionBits(mode.ordinal(), bits).withMirror(mode.ordinal(), next.ordinal()));
    }

    private static void moveEndPosition(ItemStack stack, Player player, boolean reverse) {
        Direction direction = Direction.orderedByNearest(player)[0];
        BlockPos start = stack.get(ModDataComponents.RULER_START);
        BlockPos end = stack.get(ModDataComponents.RULER_END);
        if (end == null) {
            end = start;
        }
        if (start == null || end == null) {
            return;
        }
        int v = reverse ? 1 : -1;
        stack.set(ModDataComponents.RULER_END, end.relative(direction, v));
    }

    @Override
    public Component getName(ItemStack stack) {
        String base = Component.translatable(getDescriptionId()).getString();
        if (base.length() >= 14) {
            base = EUStringUtils.getInitialsWithDots(base);
        }
        Mode mode = Mode.fromStack(stack);
        return Component.literal(base)
                .append(" - ")
                .append(Component.translatable(mode.tooltip()).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.WHITE);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean verbose = ClientAccess.isShiftDown();
        if (ModularHelper.data(stack).modules().isEmpty()) {
            EnderItemTooltips.addTranslatedTips(getDescriptionId() + ".tooltips", tooltip, verbose);
        }
        List<Component> lines = new ArrayList<>();
        addConfiguredTooltipLines(stack, lines, true);
        if (!verbose && lines.size() > 2) {
            lines.clear();
            addConfiguredTooltipLines(stack, lines, false);
            if (!lines.isEmpty()) {
                tooltip.add(lines.getFirst());
            }
            tooltip.add(Component.translatable("enderutilities.tooltip.item.holdshift"));
        } else {
            tooltip.addAll(lines);
        }
        EnderItemTooltips.addModularTooltips(this, stack, tooltip);
    }

    private void addConfiguredTooltipLines(ItemStack stack, List<Component> list, boolean verbose) {
        Mode mode = Mode.fromStack(stack);
        ChatFormatting pre = ChatFormatting.DARK_GREEN;
        ChatFormatting gray = ChatFormatting.GRAY;
        list.add(Component.translatable("enderutilities.tooltip.item.mode").withStyle(gray)
                .append(": ")
                .append(Component.translatable(mode.tooltip()).withStyle(pre)));
        int sel = selection(stack);
        if (mode.isAreaMode() && mode != Mode.REPLACE_3D) {
            String key = mode == Mode.COPY || mode == Mode.PASTE
                    ? "enderutilities.tooltip.item.selectedtemplate"
                    : "enderutilities.tooltip.item.area";
            list.add(Component.translatable(key).withStyle(gray)
                    .append(": ")
                    .append(Component.literal(String.valueOf(sel + 1)).withStyle(ChatFormatting.GREEN)));
        } else if (sel >= 0) {
            ItemStack bound = options(stack).boundBlock(sel);
            if (!bound.isEmpty()) {
                list.add(Component.translatable("enderutilities.tooltip.item.build.target").withStyle(gray)
                        .append(": ")
                        .append(bound.getHoverName().copy().withStyle(pre)));
            }
            if (mode == Mode.REPLACE_3D) {
                ItemStack replacement = options(stack).boundBlock(secondarySelection(stack));
                if (!replacement.isEmpty()) {
                    list.add(Component.translatable("enderutilities.tooltip.item.build.replacement").withStyle(gray)
                            .append(": ")
                            .append(replacement.getHoverName().copy().withStyle(pre)));
                }
            }
        } else {
            String typeKey = sel == WandModeOptions.BLOCK_TYPE_TARGETED
                    ? "enderutilities.tooltip.item.blocktype.targeted"
                    : "enderutilities.tooltip.item.blocktype.adjacent";
            list.add(Component.translatable("enderutilities.tooltip.item.selectedblock").withStyle(gray)
                    .append(": ")
                    .append(Component.translatable(typeKey).withStyle(pre)));
        }
        if (mode.isAreaMode()) {
            Direction facing = areaFacing(stack);
            Component facingText = facing != null
                    ? Component.literal(facing.getSerializedName()).withStyle(ChatFormatting.GREEN)
                    : Component.literal("N/A").withStyle(ChatFormatting.RED);
            list.add(Component.translatable("enderutilities.tooltip.item.rotation").withStyle(gray)
                    .append(": ")
                    .append(facingText));
            if (mirrored(stack)) {
                String axis = mirror(stack) == Mirror.FRONT_BACK ? "x" : "z";
                list.add(Component.translatable("enderutilities.tooltip.item.mirror").withStyle(gray)
                        .append(": ")
                        .append(Component.literal(axis).withStyle(ChatFormatting.GREEN)));
            } else {
                list.add(yesNo("enderutilities.tooltip.item.mirror", false));
            }
        } else {
            list.add(yesNo("enderutilities.tooltip.item.area.flipped", option(stack, mode, WandModeOptions.AREA_FLIPPED)));
            if (option(stack, mode, WandModeOptions.AREA_FLIPPED)) {
                list.add(Component.translatable("enderutilities.tooltip.item.flipaxis").withStyle(gray)
                        .append(": ")
                        .append(Component.literal(String.valueOf(flipAxis(stack))).withStyle(ChatFormatting.BLUE)));
            }
            list.add(yesNo("enderutilities.tooltip.item.move", option(stack, mode, WandModeOptions.MOVE_POSITION)));
        }
        if (mode == Mode.EXTEND_CONTINUOUS) {
            list.add(yesNo("enderutilities.tooltip.item.builderswand.allowdiagonals",
                    option(stack, mode, WandModeOptions.ALLOW_DIAGONALS)));
        }
        if (!mode.isAreaMode()) {
            list.add(yesNo("enderutilities.tooltip.item.builderswand.renderghostblocks",
                    option(stack, mode, WandModeOptions.RENDER_GHOST)));
        }
        if (!verbose) {
            return;
        }
    }

    private static Component yesNo(String label, boolean value) {
        return Component.translatable(label).withStyle(ChatFormatting.GRAY)
                .append(": ")
                .append(Component.translatable(value ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 4;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType type) {
        return switch (type) {
            case ENDERCAPACITOR -> 1;
            case LINKCRYSTAL -> 3;
            default -> 0;
        };
    }

    @Override
    public int getMaxModules(ItemStack container, ItemStack moduleStack) {
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IModule module)) {
            return 0;
        }
        if (module.getModuleType(moduleStack) == ModuleType.LINKCRYSTAL
                && module.getModuleTier(moduleStack) != ItemLinkCrystal.TYPE_BLOCK) {
            return 0;
        }
        return getMaxModules(container, module.getModuleType(moduleStack));
    }
}
