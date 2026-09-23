package dev.celli.enderutilities.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerOffhandInvWrapper;

public final class InventoryHelper {
    private InventoryHelper() {
    }

    /**
     * Inventory at {@code pos}. Vanilla/trapped double chests are one 54-slot
     * {@link InvWrapper} via {@link ChestBlock#getContainer}, not the 27-slot
     * NeoForge capability on a single half. Barrels and other blocks stay as-is.
     */
    public static IItemHandler inventoryAt(Level level, BlockPos pos, Direction side) {
        Container chest = vanillaChestContainer(level, pos);
        if (chest != null) {
            return new InvWrapper(chest);
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    /**
     * Combined vanilla chest inventory (27 or 54 slots), or {@code null} if this
     * is not a {@link ChestBlock}. Cats / a solid block above do not hide the other half.
     */
    @Nullable
    public static Container vanillaChestContainer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) {
            return ChestBlock.getContainer(chest, state, level, pos, true);
        }
        return null;
    }

    public static ItemStack insertIntoExisting(IItemHandler handler, ItemStack stack) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        for (int i = 0; i < handler.getSlots() && !stack.isEmpty(); i++) {
            ItemStack slot = handler.getStackInSlot(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                stack = handler.insertItem(i, stack, false);
            }
        }
        return stack;
    }

    public static ItemStack insert(IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        return ItemHandlerHelper.insertItemStacked(handler, stack, simulate);
    }

    public static boolean hasMatchingStack(IItemHandler handler, ItemStack stack) {
        if (handler == null || stack.isEmpty()) {
            return false;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(i), stack)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack insertMatchingOrAny(IItemHandler handler, ItemStack stack, boolean matchingOnly, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        if (matchingOnly && !hasMatchingStack(handler, stack)) {
            return stack;
        }
        return insert(handler, stack, simulate);
    }

    public static IItemHandler playerHandler(Player player) {
        return player.getCapability(Capabilities.ItemHandler.ENTITY);
    }

    public static void moveAll(IItemHandler from, IItemHandler to, int fromStart, int fromEnd) {
        if (from == null || to == null) {
            return;
        }
        int end = Math.min(fromEnd, from.getSlots());
        for (int i = fromStart; i < end; i++) {
            ItemStack extracted = from.extractItem(i, 64, false);
            ItemStack leftover = insert(to, extracted, false);
            if (!leftover.isEmpty()) {
                from.insertItem(i, leftover, false);
            }
        }
    }

    public static void moveMatching(IItemHandler from, IItemHandler to, int fromStart, int fromEnd) {
        if (from == null || to == null) {
            return;
        }
        int end = Math.min(fromEnd, from.getSlots());
        for (int i = fromStart; i < end; i++) {
            ItemStack current = from.getStackInSlot(i);
            if (current.isEmpty() || !hasMatchingStack(to, current)) {
                continue;
            }
            ItemStack extracted = from.extractItem(i, current.getCount(), false);
            ItemStack leftover = insert(to, extracted, false);
            if (!leftover.isEmpty()) {
                from.insertItem(i, leftover, false);
            }
        }
    }

    public static void fillMatching(IItemHandler from, IItemHandler to) {
        if (from == null || to == null) {
            return;
        }
        for (int i = 0; i < to.getSlots(); i++) {
            ItemStack current = to.getStackInSlot(i);
            if (current.isEmpty() || current.getCount() >= current.getMaxStackSize()) {
                continue;
            }
            int need = current.getMaxStackSize() - current.getCount();
            for (int src = 0; src < from.getSlots() && need > 0; src++) {
                ItemStack available = from.getStackInSlot(src);
                if (!ItemStack.isSameItemSameComponents(available, current)) {
                    continue;
                }
                ItemStack taken = from.extractItem(src, need, false);
                ItemStack leftover = to.insertItem(i, taken, false);
                need -= taken.getCount() - leftover.getCount();
                if (!leftover.isEmpty()) {
                    from.insertItem(src, leftover, false);
                }
            }
        }
    }

    public static void leaveOneFullStack(IItemHandler player, IItemHandler bag) {
        if (player == null || bag == null) {
            return;
        }
        java.util.Set<String> kept = new java.util.HashSet<>();
        for (int i = 0; i < player.getSlots(); i++) {
            ItemStack current = player.getStackInSlot(i);
            if (current.isEmpty()) {
                continue;
            }
            String key = current.getItemHolder().getRegisteredName() + current.getComponents().toString();
            if (kept.add(key)) {
                int need = current.getMaxStackSize() - current.getCount();
                if (need > 0) {
                    for (int src = 0; src < bag.getSlots() && need > 0; src++) {
                        if (!ItemStack.isSameItemSameComponents(bag.getStackInSlot(src), current)) {
                            continue;
                        }
                        ItemStack taken = bag.extractItem(src, need, false);
                        ItemStack leftover = player.insertItem(i, taken, false);
                        need -= taken.getCount() - leftover.getCount();
                        if (!leftover.isEmpty()) {
                            bag.insertItem(src, leftover, false);
                        }
                    }
                }
            } else {
                ItemStack extracted = player.extractItem(i, current.getCount(), false);
                ItemStack leftover = insert(bag, extracted, false);
                if (!leftover.isEmpty()) {
                    player.insertItem(i, leftover, false);
                }
            }
        }
    }

    public static IItemHandlerModifiable playerMainAndOffhand(Player player) {
        return new CombinedInvWrapper(
                new PlayerMainInvWrapper(player.getInventory()),
                new PlayerOffhandInvWrapper(player.getInventory()));
    }

    public static void sortRange(IItemHandlerModifiable inv, int from, int to) {
        if (inv == null) {
            return;
        }
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        int end = Math.min(to, inv.getSlots());
        for (int i = from; i < end; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        stacks.sort((a, b) -> {
            int name = a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString());
            return name != 0 ? name : Integer.compare(b.getCount(), a.getCount());
        });
        int slot = from;
        for (ItemStack stack : stacks) {
            if (slot >= end) {
                break;
            }
            inv.setStackInSlot(slot++, stack);
        }
    }
}
