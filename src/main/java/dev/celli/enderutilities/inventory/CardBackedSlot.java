package dev.celli.enderutilities.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Orig {@code ItemStackHandlerBasic} with {@code allowCustomStackSizes}: the card
 * limit is the slot limit, including items that vanilla-stack lower.
 */
public class CardBackedSlot extends SlotItemHandler {
    public CardBackedSlot(IItemHandler handler, int index, int x, int y) {
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
