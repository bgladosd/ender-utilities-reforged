package dev.celli.enderutilities.inventory;

import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ItemStackBackedHandler implements IItemHandlerModifiable {
    private final Supplier<ItemStack> container;
    private final Consumer<ItemStack> writer;
    private final int slots;
    private final IntSupplier maxStack;
    private final boolean allowCustomStackSizes;

    public ItemStackBackedHandler(Supplier<ItemStack> container, Consumer<ItemStack> writer, int slots, int maxStack) {
        this(container, writer, slots, () -> maxStack, false);
    }

    public ItemStackBackedHandler(Supplier<ItemStack> container, Consumer<ItemStack> writer, int slots, IntSupplier maxStack) {
        this(container, writer, slots, maxStack, false);
    }

    public ItemStackBackedHandler(
            Supplier<ItemStack> container,
            Consumer<ItemStack> writer,
            int slots,
            IntSupplier maxStack,
            boolean allowCustomStackSizes
    ) {
        this.container = container;
        this.writer = writer;
        this.slots = slots;
        this.maxStack = maxStack;
        this.allowCustomStackSizes = allowCustomStackSizes;
    }

    private dev.celli.enderutilities.data.ItemInventoryData data() {
        return container.get().getOrDefault(
                dev.celli.enderutilities.registry.ModDataComponents.ITEM_INVENTORY,
                dev.celli.enderutilities.data.ItemInventoryData.EMPTY
        );
    }

    private void set(dev.celli.enderutilities.data.ItemInventoryData data) {
        ItemStack stack = container.get();
        stack.set(dev.celli.enderutilities.registry.ModDataComponents.ITEM_INVENTORY, data);
        writer.accept(stack);
    }

    @Override
    public int getSlots() {
        return slots;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return data().stackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= slots) {
            return stack;
        }
        ItemStack existing = data().stackInSlot(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
            return stack;
        }
        int cap = Math.max(1, maxStack.getAsInt());
        int limit = allowCustomStackSizes ? cap : Math.min(cap, stack.getMaxStackSize());
        int current = existing.getCount();
        int space = limit - current;
        if (space <= 0) {
            return stack;
        }
        int move = Math.min(space, stack.getCount());
        if (!simulate) {
            ItemStack stored = existing.isEmpty() ? stack.copyWithCount(move) : existing.copy();
            if (!existing.isEmpty()) {
                stored.grow(move);
            }
            set(data().withSlot(slot, stored));
        }
        return stack.copyWithCount(stack.getCount() - move);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack existing = data().stackInSlot(slot);
        if (existing.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(take);
        if (!simulate) {
            existing.shrink(take);
            set(data().withSlot(slot, existing));
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return maxStack.getAsInt();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        set(data().withSlot(slot, stack));
    }
}
