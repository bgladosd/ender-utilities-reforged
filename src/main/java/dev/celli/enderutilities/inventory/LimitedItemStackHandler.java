package dev.celli.enderutilities.inventory;

import java.util.function.BiPredicate;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class LimitedItemStackHandler extends ItemStackHandler {
    private int stackLimit;
    private int visibleSlots;
    private BiPredicate<Integer, ItemStack> validator = (slot, stack) -> true;
    private Runnable onChanged = () -> {
    };

    public LimitedItemStackHandler(int size, int stackLimit) {
        super(size);
        this.stackLimit = stackLimit;
        this.visibleSlots = size;
    }

    public LimitedItemStackHandler onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public LimitedItemStackHandler validator(BiPredicate<Integer, ItemStack> validator) {
        this.validator = validator;
        return this;
    }

    public void setStackLimit(int stackLimit) {
        this.stackLimit = Math.max(1, stackLimit);
    }

    public int getStackLimit() {
        return stackLimit;
    }

    public void setVisibleSlots(int visibleSlots) {
        this.visibleSlots = Math.max(1, Math.min(visibleSlots, stacks.size()));
    }

    public int getVisibleSlots() {
        return visibleSlots;
    }

    public void ensureSize(int size) {
        if (stacks.size() != size) {
            NonNullList<ItemStack> next = NonNullList.withSize(size, ItemStack.EMPTY);
            int copy = Math.min(stacks.size(), size);
            for (int i = 0; i < copy; i++) {
                next.set(i, stacks.get(i));
            }
            this.stacks = next;
        }
        this.visibleSlots = size;
    }

    @Override
    public int getSlots() {
        return visibleSlots;
    }

    @Override
    public int getSlotLimit(int slot) {
        return stackLimit;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return stackLimit;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < getSlots() && validator.test(slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChanged.run();
    }
}
