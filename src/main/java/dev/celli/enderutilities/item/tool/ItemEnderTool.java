package dev.celli.enderutilities.item.tool;

import java.util.List;

import dev.celli.enderutilities.item.ItemLinkCrystal;
import dev.celli.enderutilities.item.ItemLocationBoundModular;
import dev.celli.enderutilities.item.module.IAnvilRepairable;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.ChunkLoading;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemEnderTool extends ItemLocationBoundModular implements IAnvilRepairable {
    public enum Kind {
        PICKAXE(BlockTags.MINEABLE_WITH_PICKAXE),
        AXE(BlockTags.MINEABLE_WITH_AXE),
        SHOVEL(BlockTags.MINEABLE_WITH_SHOVEL),
        HOE(BlockTags.MINEABLE_WITH_HOE);

        public final TagKey<Block> mineable;

        Kind(TagKey<Block> mineable) {
            this.mineable = mineable;
        }
    }

    public static final int ENDER_CHARGE_COST = 50;
    public static final int MODE_NORMAL = 0;
    public static final int MODE_FAST = 1;
    public static final int DROPS_NORMAL = 0;
    public static final int DROPS_PLAYER = 1;
    public static final int DROPS_REMOTE = 2;

    private final Kind kind;

    public ItemEnderTool(Properties properties, Kind kind) {
        super(properties.stacksTo(1).durability(1561));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isFast(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_MODE, MODE_NORMAL) == MODE_FAST;
    }

    public int dropsMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TOOL_DROPS, DROPS_NORMAL);
    }

    public boolean isCreativeLikeBreakingEnabled(ItemStack stack) {
        return kind != Kind.HOE
                && ModularHelper.getInstalledModuleCount(stack, ModuleType.CREATIVE_BREAKING) > 0
                && stack.getOrDefault(ModDataComponents.CREATIVE_BREAKING, false);
    }

    public void cycleCreativeBreakingMode(ItemStack stack) {
        if (kind != Kind.HOE && ModularHelper.getInstalledModuleCount(stack, ModuleType.CREATIVE_BREAKING) > 0) {
            stack.set(ModDataComponents.CREATIVE_BREAKING, !stack.getOrDefault(ModDataComponents.CREATIVE_BREAKING, false));
        } else {
            stack.set(ModDataComponents.CREATIVE_BREAKING, false);
        }
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return 1.0F;
        }
        if (isCreativeLikeBreakingEnabled(stack) && isCorrectToolForDrops(stack, state)) {
            return 1600.0F;
        }
        float speed = state.is(kind.mineable) ? 8.0F : 1.0F;
        return isFast(stack) ? speed * 4.0F : speed;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(kind.mineable);
    }

    public void handleHarvestDrops(ItemStack tool, net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (event.getLevel().isClientSide() || tool.getDamageValue() >= tool.getMaxDamage() - 1) {
            return;
        }
        int mode = dropsMode(tool);
        if (mode == DROPS_NORMAL || !(event.getBreaker() instanceof Player player)) {
            return;
        }
        int core = ModularHelper.getMaxModuleTier(tool, ModuleType.ENDERCORE);
        if (mode == DROPS_PLAYER && core >= 0
                && dev.celli.enderutilities.event.ItemPickupEvents.handlePlayerItemPickup(player, event.getDrops())) {
            return;
        }
        var inv = linkedInventory(tool, player);
        boolean transported = ToolDropHandler.transportDrops(event.getDrops(), inv, tool, player, mode == DROPS_REMOTE);
        if (transported && mode == DROPS_REMOTE) {
            ModularHelper.useEnderCharge(tool, ENDER_CHARGE_COST, false);
        }
        if (!event.getDrops().isEmpty() && core >= 0) {
            dev.celli.enderutilities.event.ItemPickupEvents.handlePlayerItemPickup(player, event.getDrops());
        }
    }

    net.neoforged.neoforge.items.IItemHandler linkedInventory(ItemStack tool, Player player) {
        int mode = dropsMode(tool);
        int core = ModularHelper.getMaxModuleTier(tool, ModuleType.ENDERCORE);
        if (mode == DROPS_PLAYER && core >= 0) {
            return dev.celli.enderutilities.util.InventoryHelper.playerHandler(player);
        }
        if (mode == DROPS_REMOTE && core >= 1 && ModularHelper.useEnderCharge(tool, ENDER_CHARGE_COST, true)) {
            var target = ModularHelper.getSelectedTarget(tool);
            if (target == null) {
                return null;
            }
            if (!target.dimension().equals(player.level().dimension()) && core != 2) {
                return null;
            }
            return ModularHelper.getBoundInventory(tool, player, ChunkLoading.TIMEOUT_TOOL_INVENTORY);
        }
        return null;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (!level.isClientSide && miningEntity instanceof Player player && isCreativeLikeBreakingEnabled(stack)) {
            player.getCooldowns().addCooldown(this, 6);
        }
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            int damage = isFast(stack) && !isCreativeLikeBreakingEnabled(stack) ? 2 : 1;
            if (stack.getDamageValue() + damage >= stack.getMaxDamage()) {
                stack.setDamageValue(stack.getMaxDamage() - 1);
            } else {
                stack.hurtAndBreak(damage, miningEntity, EquipmentSlot.MAINHAND);
            }
        }
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        if (player.isShiftKeyDown()) {
            BlockEntity te = level.getBlockEntity(pos);
            boolean inventory = te instanceof EnderChestBlockEntity
                    || level.getCapability(Capabilities.ItemHandler.BLOCK, pos, context.getClickedFace()) != null;
            if (inventory && ModularHelper.getSelectedModuleTier(stack, ModuleType.LINKCRYSTAL) == ItemLinkCrystal.TYPE_BLOCK) {
                return super.useOn(context);
            }
        }
        if (kind != Kind.HOE) {
            return placeBlock(player, context);
        }
        if (!level.isClientSide) {
            if (isFast(stack)) {
                if (!useHoeArea(stack, player, context, 1, 1)) {
                    useHoeToPlantArea(stack, player, context, 1, 1);
                }
            } else if (!useHoe(stack, player, context, pos)) {
                useHoeToPlant(stack, player, context, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Orig {@code ItemEnderTool#placeBlock}: place the {@link BlockItem} from the hotbar slot
     * to the right of the tool (or slot 0 if the tool is last; if the tool is in slot 0 and
     * slot 1 is not a block, fall back to the last hotbar slot). Off-hand blocks are left to
     * vanilla. No sneak or charge check — sneak-bind is handled above.
     */
    private InteractionResult placeBlock(Player player, UseOnContext context) {
        if (!player.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        int origSlot = player.getInventory().selected;
        int slot = origSlot >= 8 ? 0 : origSlot + 1;
        ItemStack target = player.getInventory().getItem(slot);
        if (origSlot == 0 && (target.isEmpty() || !(target.getItem() instanceof BlockItem))) {
            slot = 8;
            target = player.getInventory().getItem(slot);
        }
        if (target.isEmpty() || !(target.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        if (!canPlaceBlockAt(level, pos, side, blockItem.getBlock())) {
            return InteractionResult.PASS;
        }
        InteractionHand hand = context.getHand();
        ItemStack tool = player.getItemInHand(hand);
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.setItemInHand(hand, target);
        int sizeOrig = target.getCount();
        InteractionResult result = InteractionResult.PASS;
        try {
            result = target.useOn(new UseOnContext(player, hand, new BlockHitResult(
                    context.getClickLocation(), side, pos, context.isInside())));
        } catch (Exception ignored) {
        }
        player.setItemInHand(hand, tool);
        if (player.getAbilities().instabuild) {
            target.setCount(sizeOrig);
        }
        player.getInventory().setItem(slot, target.isEmpty() ? ItemStack.EMPTY : target);
        return result;
    }

    /** Orig {@code BlockUtils.checkCanPlaceBlockAt}. */
    private static boolean canPlaceBlockAt(Level level, BlockPos pos, Direction side, Block block) {
        BlockState existing = level.getBlockState(pos);
        if (existing.is(net.minecraft.world.level.block.Blocks.SNOW) && existing.canBeReplaced()) {
            side = Direction.UP;
        } else if (!existing.canBeReplaced()) {
            pos = pos.relative(side);
        }
        return block.defaultBlockState().canSurvive(level, pos) && level.getBlockState(pos).canBeReplaced();
    }

    private boolean useHoeArea(ItemStack stack, Player player, UseOnContext context, int rWidth, int rHeight) {
        boolean northSouth = (Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5F) & 1) == 0;
        if (!northSouth) {
            int tmp = rWidth;
            rWidth = rHeight;
            rHeight = tmp;
        }
        BlockPos pos = context.getClickedPos();
        boolean tilled = false;
        for (int x = pos.getX() - rWidth; x <= pos.getX() + rWidth; x++) {
            for (int z = pos.getZ() - rHeight; z <= pos.getZ() + rHeight; z++) {
                tilled |= useHoe(stack, player, context, new BlockPos(x, pos.getY(), z));
            }
        }
        return tilled;
    }

    private boolean useHoe(ItemStack stack, Player player, UseOnContext context, BlockPos pos) {
        Level level = context.getLevel();
        Direction side = context.getClickedFace();
        if (!player.mayUseItemAt(pos, side, stack) || side == Direction.DOWN || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        UseOnContext hoeContext = new UseOnContext(player, context.getHand(),
                new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false));
        BlockState tilled = level.getBlockState(pos).getToolModifiedState(hoeContext, ItemAbilities.HOE_TILL, false);
        if (tilled == null) {
            return false;
        }
        level.setBlock(pos, tilled, Block.UPDATE_ALL_IMMEDIATE);
        if (stack.getDamageValue() < stack.getMaxDamage() - 1) {
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private boolean useHoeToPlantArea(ItemStack stack, Player player, UseOnContext context, int rWidth, int rHeight) {
        boolean northSouth = (Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5F) & 1) == 0;
        if (!northSouth) {
            int tmp = rWidth;
            rWidth = rHeight;
            rHeight = tmp;
        }
        BlockPos pos = context.getClickedPos();
        boolean planted = false;
        for (int x = pos.getX() - rWidth; x <= pos.getX() + rWidth; x++) {
            for (int z = pos.getZ() - rHeight; z <= pos.getZ() + rHeight; z++) {
                planted |= useHoeToPlant(stack, player, context, new BlockPos(x, pos.getY(), z));
            }
        }
        return planted;
    }

    private boolean useHoeToPlant(ItemStack toolStack, Player player, UseOnContext context, BlockPos pos) {
        if (!ModularHelper.useEnderCharge(toolStack, ENDER_CHARGE_COST, true)) {
            return false;
        }
        IItemHandler inv = linkedInventory(toolStack, player);
        if (inv == null) {
            return false;
        }
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            if (plantItemFromInventorySlot(player, context, inv, slot, pos)) {
                if (dropsMode(toolStack) == DROPS_REMOTE) {
                    ModularHelper.useEnderCharge(toolStack, ENDER_CHARGE_COST, false);
                }
                return true;
            }
        }
        return false;
    }

    private boolean plantItemFromInventorySlot(Player player, UseOnContext context, IItemHandler inv, int slot, BlockPos pos) {
        ItemStack plantStack = inv.getStackInSlot(slot);
        if (plantStack.isEmpty() || !(plantStack.getItem() instanceof BlockItem blockItem)
                || !isPlantItem(blockItem.getBlock())) {
            return false;
        }
        ItemStack extracted = inv.extractItem(slot, 1, false);
        if (extracted.isEmpty()) {
            return false;
        }
        InteractionHand hand = context.getHand();
        ItemStack held = player.getItemInHand(hand);
        player.setItemInHand(hand, extracted);
        UseOnContext plantContext = new UseOnContext(player, hand,
                new BlockHitResult(Vec3.atCenterOf(pos), context.getClickedFace(), pos, false));
        InteractionResult result = extracted.useOn(plantContext);
        extracted = player.getItemInHand(hand);
        player.setItemInHand(hand, held);
        boolean planted = result.consumesAction();
        if (!extracted.isEmpty()) {
            ItemStack leftover = dev.celli.enderutilities.util.InventoryHelper.insert(inv, extracted, false);
            if (!leftover.isEmpty() && !player.addItem(leftover)) {
                player.drop(leftover, false);
            }
        }
        return planted;
    }

    private static boolean isPlantItem(Block block) {
        BlockState def = block.defaultBlockState();
        return def.is(BlockTags.CROPS)
                || def.is(BlockTags.SAPLINGS)
                || !def.blocksMotion();
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE)) {
            stack.set(ModDataComponents.ITEM_MODE, isFast(stack) ? MODE_NORMAL : MODE_FAST);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT)) {
            stack.set(ModDataComponents.TOOL_DROPS, (dropsMode(stack) + 1) % 3);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_CTRL) && kind != Kind.HOE) {
            cycleCreativeBreakingMode(stack);
            return true;
        }
        return super.doKeyBindingAction(player, stack, key);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int mode = dropsMode(stack);
        String dropsKey = mode == DROPS_PLAYER
                ? "enderutilities.tooltip.item.endertool.playerinv"
                : mode == DROPS_REMOTE
                        ? "enderutilities.tooltip.item.endertool.remote"
                        : "enderutilities.tooltip.item.normal";
        tooltip.add(Component.translatable("enderutilities.tooltip.item.endertool.dropsmode")
                .append(": ")
                .append(Component.translatable(dropsKey).withStyle(ChatFormatting.DARK_GREEN)));
        if (kind == Kind.HOE) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.mode")
                    .append(": ")
                    .append(Component.translatable(isFast(stack)
                                    ? "enderutilities.tooltip.item.3x3"
                                    : "enderutilities.tooltip.item.1x1")
                            .withStyle(ChatFormatting.DARK_GREEN)));
        } else {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.endertool.digmode")
                    .append(": ")
                    .append(Component.translatable(isFast(stack)
                                    ? "enderutilities.tooltip.item.fast"
                                    : "enderutilities.tooltip.item.normal")
                            .withStyle(ChatFormatting.DARK_GREEN)));
        }
        addEnderCoreTooltip(stack, tooltip);
        if (kind != Kind.HOE) {
            boolean installed = ModularHelper.getInstalledModuleCount(stack, ModuleType.CREATIVE_BREAKING) > 0;
            tooltip.add(Component.translatable("enderutilities.tooltip.item.creative_breaking_installed",
                    yesNo(installed)));
            tooltip.add(Component.translatable("enderutilities.tooltip.item.creative_breaking_enabled",
                    yesNo(isCreativeLikeBreakingEnabled(stack))));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** Orig {@code ItemEnderTool#addTooltipLines} Ender Core line. */
    static void addEnderCoreTooltip(ItemStack stack, List<Component> tooltip) {
        int coreTier = ModularHelper.getMaxModuleTier(stack, ModuleType.ENDERCORE);
        Component core;
        if (coreTier >= 0 && coreTier <= 2) {
            String typeKey = coreTier == 0
                    ? "enderutilities.tooltip.item.basic"
                    : coreTier == 1
                            ? "enderutilities.tooltip.item.enhanced"
                            : "enderutilities.tooltip.item.advanced";
            core = Component.translatable(typeKey).withStyle(ChatFormatting.DARK_GREEN)
                    .append(Component.literal(" (").withStyle(ChatFormatting.RESET, ChatFormatting.GRAY))
                    .append(Component.translatable("enderutilities.tooltip.item.tier").withStyle(ChatFormatting.BLUE))
                    .append(Component.literal(" " + (coreTier + 1)).withStyle(ChatFormatting.BLUE))
                    .append(Component.literal(")").withStyle(ChatFormatting.RESET, ChatFormatting.GRAY));
        } else {
            core = Component.translatable("enderutilities.tooltip.item.none").withStyle(ChatFormatting.RED);
        }
        tooltip.add(Component.translatable("enderutilities.tooltip.item.endercore")
                .append(": ")
                .append(core));
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return kind == Kind.HOE ? 5 : 6;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType type) {
        return switch (type) {
            case ENDERCAPACITOR -> 1;
            case LINKCRYSTAL -> 3;
            case ENDERCORE -> 1;
            case CREATIVE_BREAKING -> kind == Kind.HOE ? 0 : 1;
            default -> 0;
        };
    }

    @Override
    public boolean repairItem(ItemStack stack, int amount) {
        if (amount == -1) {
            amount = stack.getMaxDamage();
        }
        int damage = Math.max(stack.getDamageValue() - amount, 0);
        boolean repaired = damage != stack.getDamageValue();
        stack.setDamageValue(damage);
        return repaired;
    }

    @Override
    public boolean isRepairItem(ItemStack stackTool, ItemStack stackMaterial) {
        return stackMaterial.is(dev.celli.enderutilities.registry.ModItems.ENDER_ALLOY_ADVANCED.get());
    }
}
