package dev.celli.enderutilities.menu;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Slot whose max count is the handler limit, not vanilla 64.
 * Lets ASU/MSU/barrel/JSU carry stacks the orig custom click container allowed.
 */
public class LargeStackSlot extends SlotItemHandler {
    public LargeStackSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public int getMaxStackSize() {
        return getItemHandler().getSlotLimit(getSlotIndex());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getItemHandler().getSlotLimit(getSlotIndex());
    }
}
