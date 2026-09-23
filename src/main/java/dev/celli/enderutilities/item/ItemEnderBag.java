package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EnderItemTooltips;
import dev.celli.enderutilities.util.InventoryHelper;
import dev.celli.enderutilities.util.RemoteAccessContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class ItemEnderBag extends ItemLocationBoundModular {
    public static final int ENDER_CHARGE_COST = 200;

    public ItemEnderBag(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        TargetLocation target = ModularHelper.getSelectedTarget(stack);
        if (target == null || target.blockId().isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        if (target.isEnderChest()) {
            if (!ModularHelper.useEnderCharge(stack, ENDER_CHARGE_COST, false)) {
                return InteractionResultHolder.fail(stack);
            }
            if (!level.isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> ChestMenu.threeRows(id, inv, p.getEnderChestInventory()),
                        Component.translatable("container.enderchest")
                ));
            }
            return ItemEnderUtilities.handledWithoutUseAnim(stack, level.isClientSide);
        }

        if (!level.isClientSide && level instanceof ServerLevel server) {
            var dest = server.getServer().getLevel(target.dimension());
            if (dest != null && ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)
                    && ModularHelper.useEnderCharge(stack, ENDER_CHARGE_COST, true)) {
                dev.celli.enderutilities.util.ChunkLoading.getInstance().forceChunk(
                        dest, target.pos(), dev.celli.enderutilities.util.ChunkLoading.TIMEOUT_BAG, player.getUUID());
                if (openBoundChest(dest, target.pos(), player, stack)) {
                    stack.set(ModDataComponents.CHUNK_LOADING_REQUIRED, true);
                    return ItemEnderUtilities.handledWithoutUseAnim(stack, false);
                }
                var be = dest.getBlockEntity(target.pos());
                if (be instanceof net.minecraft.world.MenuProvider provider) {
                    ModularHelper.useEnderCharge(stack, ENDER_CHARGE_COST, false);
                    stack.set(ModDataComponents.CHUNK_LOADING_REQUIRED, true);
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.openMenu(provider, buf -> buf.writeBlockPos(target.pos()));
                    } else {
                        player.openMenu(provider);
                    }
                    return ItemEnderUtilities.handledWithoutUseAnim(stack, false);
                }
                var state = dest.getBlockState(target.pos());
                var hit = new net.minecraft.world.phys.BlockHitResult(target.exact(), target.face(), target.pos(), false);
                var result = state.useWithoutItem(dest, player, hit);
                if (result.consumesAction()) {
                    ModularHelper.useEnderCharge(stack, ENDER_CHARGE_COST, false);
                    stack.set(ModDataComponents.CHUNK_LOADING_REQUIRED, true);
                    return ItemEnderUtilities.handledWithoutUseAnim(stack, false);
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * Vanilla/trapped double chests must open as one 54-slot GUI. The chest
     * block entity itself is only 27 slots, so {@link MenuProvider} on one half
     * is not enough. Bind still stores a single {@link BlockPos}.
     */
    private static boolean openBoundChest(ServerLevel dest, BlockPos pos, Player player, ItemStack stack) {
        BlockState state = dest.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return false;
        }
        Container container = InventoryHelper.vanillaChestContainer(dest, pos);
        if (container == null || !prepareChestOpen(dest, pos, state, player)) {
            return false;
        }
        ModularHelper.useEnderCharge(stack, ENDER_CHARGE_COST, false);
        Container remote = new RemoteAccessContainer(container);
        int slots = remote.getContainerSize();
        Component title = slots > 27
                ? Component.translatable("container.chestDouble")
                : Component.translatable("container.chest");
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> slots > 27
                        ? ChestMenu.sixRows(id, inv, remote)
                        : ChestMenu.threeRows(id, inv, remote),
                title
        ));
        return true;
    }

    private static boolean prepareChestOpen(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest) || !chest.canOpen(player)) {
            return false;
        }
        chest.unpackLootTable(player);
        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
            if (level.getBlockEntity(otherPos) instanceof ChestBlockEntity other) {
                if (!other.canOpen(player)) {
                    return false;
                }
                other.unpackLootTable(player);
            }
        }
        return true;
    }

    public static boolean isLocked(ItemStack stack) {
        ItemStack crystal = ModularHelper.getSelectedModuleStack(stack, ModuleType.LINKCRYSTAL);
        OwnerInfo owner = crystal.get(ModDataComponents.OWNER);
        return owner != null && !owner.isPublic();
    }

    public static boolean isEnderMode(ItemStack stack) {
        TargetLocation target = ModularHelper.getSelectedTarget(stack);
        return target != null && target.isEnderChest();
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 5;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType moduleType) {
        return switch (moduleType) {
            case ENDERCAPACITOR -> 1;
            case LINKCRYSTAL -> 4;
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

    @Override
    protected void addConfiguredTooltips(ItemStack stack, List<Component> tooltip) {
        TargetLocation target = ModularHelper.getSelectedTarget(stack);
        if (target != null && target.isEnderChest()) {
            EnderItemTooltips.addEnderChestBagLines(stack, tooltip);
            return;
        }
        super.addConfiguredTooltips(stack, tooltip);
    }
}
