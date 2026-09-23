package dev.celli.enderutilities.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.ChunkLoading;
import dev.celli.enderutilities.util.EUStringUtils;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class ItemEnderBucket extends ItemLocationBoundModular implements IKeyBound {
    public static final double ENDER_CHARGE_COST = 0.2d;
    public static final int MODE_NORMAL = 0;
    public static final int MODE_FILL = 1;
    public static final int MODE_DRAIN = 2;
    public static final int MODE_BIND = 3;

    private static final String[] MODE_KEYS = {
            "enderutilities.tooltip.item.bucket.mode.normal",
            "enderutilities.tooltip.item.bucket.mode.fill",
            "enderutilities.tooltip.item.bucket.mode.drain",
            "enderutilities.tooltip.item.bucket.mode.bind"
    };

    public ItemEnderBucket(Properties properties) {
        super(properties.stacksTo(1));
    }

    public int mode(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.ITEM_MODE, MODE_NORMAL), MODE_NORMAL, MODE_BIND);
    }

    public static boolean isLinked(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BUCKET_LINKED, false);
    }

    /**
     * Client returns FAIL like orig {@code onItemRightClick} so 1.21 does not start the
     * first-person use-loop. Server still receives {@code ServerboundUseItemPacket} and
     * raytraces fluids itself.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || (isLinked(stack) && !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player))) {
            return InteractionResultHolder.fail(stack);
        }
        InteractionResult result = useBucketOnFluidBlock(level, player, stack, hand);
        return new InteractionResultHolder<>(result == InteractionResult.SUCCESS ? InteractionResult.CONSUME : result, stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (isLinked(stack) && !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            var tank = level.getCapability(Capabilities.FluidHandler.BLOCK, context.getClickedPos(), context.getClickedFace());
            if (tank != null && (mode(stack) != MODE_BIND || player.isShiftKeyDown())) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (useBucketOnFluidBlock(level, player, stack, context.getHand()) == InteractionResult.SUCCESS) {
            return InteractionResult.SUCCESS;
        }
        var tank = level.getCapability(Capabilities.FluidHandler.BLOCK, context.getClickedPos(), context.getClickedFace());
        if (tank != null) {
            if (mode(stack) == MODE_BIND) {
                return super.useOn(context);
            }
            return useBucketOnTank(level, context.getClickedPos(), context.getClickedFace(), player, stack);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (isLinked(stack) && !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)) {
            return InteractionResult.FAIL;
        }
        if (useBucketOnFluidBlock(level, player, stack, context.getHand()) == InteractionResult.SUCCESS) {
            return InteractionResult.SUCCESS;
        }
        return useBucketOnBlock(level, context.getClickedPos(), context.getClickedFace(), player, stack, context.getHand());
    }

    private boolean isTargetUsable(Level level, BlockPos pos, Direction side, Player player, ItemStack stack) {
        return level.mayInteract(player, pos) && player.mayUseItemAt(pos, side, stack);
    }

    private InteractionResult useBucketOnTank(Level level, BlockPos pos, Direction side, Player player, ItemStack stack) {
        if (!isTargetUsable(level, pos, side, player, stack)) {
            return InteractionResult.PASS;
        }
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        if (tank == null) {
            return InteractionResult.PASS;
        }
        FluidStack stored = getFluid(stack, player);
        int storedAmount = stored.getAmount();
        int bucketMode = mode(stack);
        if (bucketMode == MODE_FILL || (bucketMode == MODE_NORMAL && !player.isShiftKeyDown())) {
            FluidStack simulated = tank.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
            if (!simulated.isEmpty()) {
                int amount = Math.min(getCapacityAvailable(stack, simulated, player), FluidType.BUCKET_VOLUME);
                if (amount > 0 && (storedAmount == 0 || FluidStack.isSameFluidSameComponents(simulated, stored))) {
                    FluidStack toTake = tank.drain(amount, IFluidHandler.FluidAction.SIMULATE);
                    if (!toTake.isEmpty() && fill(stack, toTake, false, player) == toTake.getAmount()) {
                        FluidStack taken = tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
                        fill(stack, taken, true, player);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        } else if (storedAmount > 0) {
            FluidStack toGive = drain(stack, FluidType.BUCKET_VOLUME, false, player);
            if (!toGive.isEmpty() && tank.fill(toGive, IFluidHandler.FluidAction.SIMULATE) > 0) {
                int filled = tank.fill(toGive, IFluidHandler.FluidAction.EXECUTE);
                drain(stack, filled, true, player);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private InteractionResult useBucketOnBlock(
            Level level,
            BlockPos pos,
            Direction side,
            Player player,
            ItemStack stack,
            InteractionHand hand
    ) {
        BlockPos placePos = pos.relative(side);
        if (!isTargetUsable(level, placePos, side, player, stack)) {
            return InteractionResult.PASS;
        }
        if (level.getBlockState(placePos).getFluidState().is(net.minecraft.tags.FluidTags.WATER)
                || level.getBlockState(placePos).getFluidState().is(net.minecraft.tags.FluidTags.LAVA)
                || !level.getBlockState(placePos).getFluidState().isEmpty()) {
            return useBucketOnFluidBlock(level, placePos, side, player, stack, hand);
        }
        FluidStack stored = getFluid(stack, player);
        if (stored.getAmount() >= FluidType.BUCKET_VOLUME && mode(stack) != MODE_FILL) {
            FluidStack toPlace = drain(stack, FluidType.BUCKET_VOLUME, false, player);
            if (toPlace.getAmount() == FluidType.BUCKET_VOLUME
                    && tryPlaceFluid(level, placePos, player, stack, hand, toPlace)) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private InteractionResult useBucketOnFluidBlock(Level level, Player player, ItemStack stack, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        return useBucketOnFluidBlock(level, hit.getBlockPos(), hit.getDirection(), player, stack, hand);
    }

    private InteractionResult useBucketOnFluidBlock(
            Level level,
            BlockPos pos,
            Direction side,
            Player player,
            ItemStack stack,
            InteractionHand hand
    ) {
        BlockState state = level.getBlockState(pos);
        if (!isTargetUsable(level, pos, side, player, stack) || state.getFluidState().isEmpty()) {
            return InteractionResult.PASS;
        }

        FluidStack stored = getFluid(stack, player);
        int storedAmount = stored.getAmount();
        FluidStack target = fluidFromWorld(level, pos, side, player);
        boolean source = state.getFluidState().isSource();
        int bucketMode = mode(stack);

        if (bucketMode != MODE_DRAIN
                && (storedAmount == 0
                        || (getCapacityAvailable(stack, target, player) >= FluidType.BUCKET_VOLUME
                                && !target.isEmpty()
                                && FluidStack.isSameFluidSameComponents(stored, target)
                                && (!player.isShiftKeyDown() || bucketMode == MODE_FILL)))) {
            if (tryPickupFluid(level, pos, side, player, stack, target, source)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        if (storedAmount >= FluidType.BUCKET_VOLUME && bucketMode != MODE_FILL) {
            if (player.isShiftKeyDown()
                    || target.isEmpty()
                    || !FluidStack.isSameFluidSameComponents(stored, target)
                    || !source) {
                FluidStack toPlace = drain(stack, FluidType.BUCKET_VOLUME, false, player);
                if (toPlace.getAmount() == FluidType.BUCKET_VOLUME
                        && tryPlaceFluid(level, pos, player, stack, hand, toPlace)) {
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    private FluidStack fluidFromWorld(Level level, BlockPos pos, Direction side, @Nullable Player player) {
        BlockState state = level.getBlockState(pos);
        Fluid fluid = state.getFluidState().getType();
        if (fluid != Fluids.EMPTY) {
            return new FluidStack(fluid, FluidType.BUCKET_VOLUME);
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        if (handler != null) {
            return handler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
        }
        if (state.getBlock() instanceof BucketPickup && player != null) {
            return FluidStack.EMPTY;
        }
        return FluidStack.EMPTY;
    }

    private boolean tryPickupFluid(
            Level level,
            BlockPos pos,
            Direction side,
            Player player,
            ItemStack stack,
            FluidStack target,
            boolean source
    ) {
        if (target.isEmpty() || fill(stack, target, false, player) != target.getAmount()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BucketPickup pickup && source) {
            ItemStack filled = pickup.pickupBlock(player, level, pos, state);
            if (!filled.isEmpty()) {
                fill(stack, target, true, player);
                playFillSound(level, pos, target);
                return true;
            }
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        if (handler != null) {
            FluidStack simulated = handler.drain(target.getAmount(), IFluidHandler.FluidAction.SIMULATE);
            if (!simulated.isEmpty() && fill(stack, simulated, false, player) == simulated.getAmount()) {
                FluidStack taken = handler.drain(simulated.getAmount(), IFluidHandler.FluidAction.EXECUTE);
                if (!taken.isEmpty()) {
                    fill(stack, taken, true, player);
                    playFillSound(level, pos, taken);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryPlaceFluid(
            Level level,
            BlockPos pos,
            Player player,
            ItemStack stack,
            InteractionHand hand,
            FluidStack resource
    ) {
        FluidStack taken = drain(stack, resource.getAmount(), true, player);
        if (taken.getAmount() < FluidType.BUCKET_VOLUME) {
            if (!taken.isEmpty()) {
                fill(stack, taken, true, player);
            }
            return false;
        }
        var tank = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(FluidType.BUCKET_VOLUME);
        tank.setFluid(taken.copy());
        if (FluidUtil.tryPlaceFluid(player, level, hand, pos, tank, taken.copy())) {
            return true;
        }
        fill(stack, taken, true, player);
        return false;
    }

    private static void playFillSound(Level level, BlockPos pos, FluidStack fluid) {
        var sound = fluid.getFluidType().getSound(fluid, net.neoforged.neoforge.common.SoundActions.BUCKET_FILL);
        level.playSound(null, pos, sound != null ? sound : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public int getCapacityCached(ItemStack stack, @Nullable Player player) {
        if (isLinked(stack)) {
            ItemStack crystal = ModularHelper.getSelectedModuleStack(stack, ModuleType.LINKCRYSTAL);
            return crystal.getOrDefault(ModDataComponents.BUCKET_CACHED_CAPACITY, 0);
        }
        return getCapacity(stack, player);
    }

    public int getCapacityAvailable(ItemStack stack, FluidStack incoming, @Nullable Player player) {
        if (isLinked(stack)) {
            IFluidHandler handler = getLinkedTank(stack);
            if (handler == null) {
                return 0;
            }
            if (incoming.isEmpty()) {
                FluidStack existing = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                return Math.max(0, handler.getTankCapacity(0) - existing.getAmount());
            }
            return handler.fill(incoming.copyWithAmount(Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE);
        }
        FluidStack existing = getInternalFluid(stack);
        if (!existing.isEmpty() && !incoming.isEmpty() && !FluidStack.isSameFluidSameComponents(existing, incoming)) {
            return 0;
        }
        return getCapacity(stack, player) - existing.getAmount();
    }

    public int getCapacity(ItemStack stack, @Nullable Player player) {
        if (!isLinked(stack)) {
            return ModCommonConfig.ENDER_BUCKET_CAPACITY.get();
        }
        if (player != null && !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)) {
            return 0;
        }
        IFluidHandler handler = getLinkedTank(stack);
        if (handler == null) {
            return 0;
        }
        return handler.getTankCapacity(0);
    }

    public FluidStack getFluidCached(ItemStack stack) {
        if (isLinked(stack)) {
            return getInternalFluid(ModularHelper.getSelectedModuleStack(stack, ModuleType.LINKCRYSTAL));
        }
        return getInternalFluid(stack);
    }

    public FluidStack getFluid(ItemStack stack, @Nullable Player player) {
        if (isLinked(stack)) {
            if (player != null && !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)) {
                return FluidStack.EMPTY;
            }
            IFluidHandler handler = getLinkedTank(stack);
            if (handler != null) {
                FluidStack fluid = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                cacheFluid(stack, fluid);
                return fluid;
            }
            return FluidStack.EMPTY;
        }
        return getInternalFluid(stack);
    }

    public FluidStack drain(ItemStack stack, int maxDrain, boolean doDrain, @Nullable Player player) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        if (isLinked(stack)) {
            if (player != null && !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)) {
                return FluidStack.EMPTY;
            }
            IFluidHandler handler = getLinkedTank(stack);
            if (handler == null) {
                return FluidStack.EMPTY;
            }
            if (!ModularHelper.useEnderCharge(stack, (int) (ENDER_CHARGE_COST * maxDrain), !doDrain)) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = handler.drain(maxDrain, doDrain ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
            cacheFluid(stack, handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE));
            return drained;
        }
        FluidStack stored = getInternalFluid(stack);
        if (stored.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int taken = Math.min(stored.getAmount(), maxDrain);
        if (doDrain) {
            stored.shrink(taken);
            setInternalFluid(stack, stored);
        }
        return stored.copyWithAmount(taken);
    }

    public int fill(ItemStack stack, FluidStack incoming, boolean doFill, @Nullable Player player) {
        if (incoming.isEmpty()) {
            return 0;
        }
        if (isLinked(stack)) {
            if (player != null && !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)) {
                return 0;
            }
            IFluidHandler handler = getLinkedTank(stack);
            if (handler == null) {
                return 0;
            }
            if (!ModularHelper.useEnderCharge(stack, (int) (ENDER_CHARGE_COST * incoming.getAmount()), !doFill)) {
                return 0;
            }
            int filled = handler.fill(incoming, doFill ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
            cacheFluid(stack, handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE));
            return filled;
        }
        int available = getCapacityAvailable(stack, incoming, player);
        if (available <= 0) {
            return 0;
        }
        int filled = Math.min(available, incoming.getAmount());
        if (doFill && filled > 0) {
            FluidStack stored = getInternalFluid(stack);
            if (stored.isEmpty()) {
                setInternalFluid(stack, incoming.copyWithAmount(filled));
            } else {
                stored.grow(filled);
                setInternalFluid(stack, stored);
            }
        }
        return filled;
    }

    public IFluidHandler getLinkedTank(ItemStack stack) {
        TargetLocation target = ModularHelper.getSelectedTarget(stack);
        if (target == null) {
            return null;
        }
        ServerLevel dest = findLevel(target);
        if (dest == null) {
            return null;
        }
        ChunkLoading.getInstance().forceChunk(dest, target.pos(), ChunkLoading.TIMEOUT_BUCKET);
        return dest.getCapability(Capabilities.FluidHandler.BLOCK, target.pos(), target.face());
    }

    @Nullable
    private static ServerLevel findLevel(TargetLocation target) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getLevel(target.dimension()) : null;
    }

    private void cacheFluid(ItemStack stack, FluidStack fluid) {
        ItemStack crystal = ModularHelper.getSelectedModuleStack(stack, ModuleType.LINKCRYSTAL);
        if (crystal.isEmpty()) {
            return;
        }
        setInternalFluid(crystal, fluid);
        IFluidHandler handler = getLinkedTank(stack);
        if (handler != null) {
            crystal.set(ModDataComponents.BUCKET_CACHED_CAPACITY, handler.getTankCapacity(0));
        } else {
            crystal.remove(ModDataComponents.BUCKET_CACHED_CAPACITY);
        }
        ModularHelper.setSelectedModuleStack(stack, ModuleType.LINKCRYSTAL, crystal);
    }

    public static FluidStack getInternalFluid(ItemStack stack) {
        String id = stack.getOrDefault(ModDataComponents.BUCKET_FLUID, "");
        int amount = stack.getOrDefault(ModDataComponents.BUCKET_AMOUNT, 0);
        if (id.isEmpty() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(loc);
        if (fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, amount);
    }

    public static void setInternalFluid(ItemStack stack, FluidStack fluid) {
        if (fluid.isEmpty()) {
            stack.remove(ModDataComponents.BUCKET_FLUID);
            stack.remove(ModDataComponents.BUCKET_AMOUNT);
            return;
        }
        stack.set(ModDataComponents.BUCKET_FLUID, BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
        stack.set(ModDataComponents.BUCKET_AMOUNT, fluid.getAmount());
    }

    public static Fluid getFluid(ItemStack stack) {
        return getInternalFluid(stack).getFluid();
    }

    public static int getAmount(ItemStack stack) {
        return getInternalFluid(stack).getAmount();
    }

    private static void cycleBucketMode(ItemStack stack) {
        int next = stack.getOrDefault(ModDataComponents.ITEM_MODE, MODE_NORMAL) + 1;
        if (next > MODE_BIND || (next == MODE_BIND && !isLinked(stack))) {
            next = MODE_NORMAL;
        }
        stack.set(ModDataComponents.ITEM_MODE, next);
    }

    private static void cycleLinkMode(ItemStack stack) {
        stack.set(ModDataComponents.BUCKET_LINKED, !isLinked(stack));
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        // Orig: Toggle (no mods) cycles Normal / Fill / Drain / Bind.
        // Bind is skipped while the bucket is not linked to a tank.
        if (key == HotKeys.KEYBIND_ID_TOGGLE_MODE) {
            cycleBucketMode(stack);
            return true;
        }
        // Orig: Shift + Toggle flips the linked-tank flag.
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT)) {
            cycleLinkMode(stack);
            return true;
        }
        // Orig: Ctrl + (Shift +) Toggle or Ctrl + scroll changes the selected link crystal while linked.
        if ((EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL))
                && isLinked(stack)) {
            return super.doKeyBindingAction(player, stack, key);
        }
        return super.doKeyBindingAction(player, stack, key);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = isLinked(stack)
                ? super.getName(stack)
                : Component.translatable(getDescriptionId(stack));
        FluidStack fluid = getFluidCached(stack);
        if (fluid.isEmpty()) {
            return base;
        }
        return Component.empty()
                .append(base)
                .append(" - ")
                .append(fluid.getHoverName().copy().withStyle(ChatFormatting.GREEN));
    }

    @Override
    protected void addConfiguredTooltips(ItemStack stack, List<Component> tooltip) {
        FluidStack fluid = getFluidCached(stack);
        ChatFormatting blue = ChatFormatting.BLUE;
        ChatFormatting green = ChatFormatting.DARK_GREEN;
        boolean verbose = ClientAccess.isShiftDown();
        int amount = fluid.getAmount();
        int capacity = getCapacityCached(stack, null);
        Component fluidName = fluid.isEmpty()
                ? Component.translatable("enderutilities.tooltip.item.empty").withStyle(green)
                : fluid.getHoverName().copy().withStyle(green);
        String amountStr = EUStringUtils.formatNumberWithKSeparators(amount)
                + " mB / "
                + EUStringUtils.formatNumberWithKSeparators(capacity)
                + " mB";
        if (isLinked(stack)) {
            if (verbose) {
                tooltip.add(Component.translatable("enderutilities.tooltip.item.cached.fluid")
                        .append(": ").append(fluidName));
                tooltip.add(Component.translatable("enderutilities.tooltip.item.cached.amount")
                        .append(": ")
                        .append(Component.literal(EUStringUtils.formatNumberWithKSeparators(amount)).withStyle(blue))
                        .append(" mB / ")
                        .append(Component.literal(EUStringUtils.formatNumberWithKSeparators(capacity)).withStyle(blue))
                        .append(" mB"));
            } else {
                tooltip.add(Component.translatable("enderutilities.tooltip.item.cached.fluid.compact")
                        .append(": ").append(fluidName).append(" - ").append(amountStr));
            }
        } else if (verbose) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.fluid").append(": ").append(fluidName));
            tooltip.add(Component.translatable("enderutilities.tooltip.item.amount")
                    .append(": ")
                    .append(Component.literal(EUStringUtils.formatNumberWithKSeparators(amount)).withStyle(blue))
                    .append(" mB / ")
                    .append(Component.literal(EUStringUtils.formatNumberWithKSeparators(capacity)).withStyle(blue))
                    .append(" mB"));
        } else {
            tooltip.add(Component.empty().append(fluidName).append(" - ").append(amountStr));
        }
        tooltip.add(Component.translatable("enderutilities.tooltip.item.mode")
                .append(": ")
                .append(Component.translatable(MODE_KEYS[mode(stack)])));
        if (isLinked(stack)) {
            EnderItemTooltips.addModularTooltips(this, stack, tooltip);
        }
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
        ModuleType type = module.getModuleType(moduleStack);
        if (type != ModuleType.LINKCRYSTAL || module.getModuleTier(moduleStack) == ItemLinkCrystal.TYPE_BLOCK) {
            return getMaxModules(container, type);
        }
        return 0;
    }
}
