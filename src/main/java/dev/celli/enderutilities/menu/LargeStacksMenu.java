package dev.celli.enderutilities.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Orig {@code ContainerCustomSlotClick} / {@code ContainerLargeStacks}:
 * cursor stacks can exceed 64; creative middle-click multiplies the slot by 10.
 */
public abstract class LargeStacksMenu extends EnderMenu {
    protected final int machineSlots;

    protected LargeStacksMenu(MenuType<?> type, int containerId, int machineSlots) {
        super(type, containerId);
        this.machineSlots = machineSlots;
    }

    protected IItemHandler machineHandler() {
        Slot slot = machineSlots > 0 ? getSlot(0) : null;
        if (slot instanceof LargeStackSlot large) {
            return large.getItemHandler();
        }
        return null;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < machineSlots && clickType == ClickType.CLONE && player.getAbilities().instabuild) {
            cycleStackSize(slotId);
            broadcastChanges();
            return;
        }
        if (clickType == ClickType.PICKUP && slotId >= 0 && slotId < slots.size()
                && (slotId < machineSlots || oversized(getCarried()))) {
            pickup(slotId, button == 1, player);
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private static boolean oversized(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() > stack.getMaxStackSize();
    }

    private void pickup(int slotId, boolean right, Player player) {
        Slot slot = getSlot(slotId);
        ItemStack cursor = getCarried();
        ItemStack inSlot = slot.getItem();
        int limit = slotLimit(slot, cursor.isEmpty() ? inSlot : cursor);
        if (cursor.isEmpty()) {
            if (inSlot.isEmpty() || !slot.mayPickup(player)) {
                return;
            }
            int amount = right ? (inSlot.getCount() + 1) / 2 : inSlot.getCount();
            setCarried(slot.safeTake(amount, Integer.MAX_VALUE, player));
            return;
        }
        if (inSlot.isEmpty() || ItemStack.isSameItemSameComponents(cursor, inSlot)) {
            if (!slot.mayPlace(cursor)) {
                if (!inSlot.isEmpty()) {
                    setCarried(slot.safeTake(inSlot.getCount(), Integer.MAX_VALUE, player));
                }
                return;
            }
            int space = Math.max(0, limit - inSlot.getCount());
            int move = right ? Math.min(1, Math.min(space, cursor.getCount())) : Math.min(space, cursor.getCount());
            if (move <= 0) {
                return;
            }
            if (inSlot.isEmpty()) {
                slot.set(cursor.copyWithCount(move));
            } else {
                inSlot.grow(move);
                slot.setChanged();
            }
            cursor.shrink(move);
            setCarried(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
            return;
        }
        if (slot.mayPickup(player) && slot.mayPlace(cursor) && cursor.getCount() <= limit) {
            slot.set(cursor);
            setCarried(inSlot);
        }
    }

    private void cycleStackSize(int slotId) {
        Slot slot = getSlot(slotId);
        ItemStack cursor = getCarried();
        ItemStack inSlot = slot.getItem();
        if (cursor.isEmpty()) {
            return;
        }
        IItemHandler handler = machineHandler();
        int max = handler != null ? handler.getSlotLimit(slot.getSlotIndex()) : slot.getMaxStackSize(cursor);
        if (ItemStack.isSameItemSameComponents(cursor, inSlot)) {
            if (cursor.getCount() == 1) {
                long next = (long) inSlot.getCount() * 10L;
                if (next > 0 && next <= max) {
                    slot.set(inSlot.copyWithCount((int) next));
                } else {
                    slot.set(ItemStack.EMPTY);
                }
            } else if (inSlot.getCount() < max) {
                slot.set(inSlot.copyWithCount(max));
            } else {
                slot.set(ItemStack.EMPTY);
            }
        } else {
            slot.set(cursor.copy());
        }
    }

    private static int slotLimit(Slot slot, ItemStack stack) {
        if (slot instanceof LargeStackSlot large) {
            return large.getMaxStackSize(stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        return stack.isEmpty() ? slot.getMaxStackSize() : Math.min(slot.getMaxStackSize(stack), stack.getMaxStackSize());
    }
}
