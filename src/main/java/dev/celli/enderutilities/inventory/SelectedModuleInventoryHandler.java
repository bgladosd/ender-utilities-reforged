package dev.celli.enderutilities.inventory;

import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class SelectedModuleInventoryHandler implements IItemHandlerModifiable {
    private final Supplier<ItemStack> container;
    private final Consumer<ItemStack> writer;
    private final ModuleType type;
    private final int slots;
    private final int maxStack;

    public SelectedModuleInventoryHandler(
            Supplier<ItemStack> container,
            Consumer<ItemStack> writer,
            ModuleType type,
            int slots,
            int maxStack
    ) {
        this.container = container;
        this.writer = writer;
        this.type = type;
        this.slots = slots;
        this.maxStack = maxStack;
    }

    private ItemStackBackedHandler backing() {
        return new ItemStackBackedHandler(
                () -> ModularHelper.getSelectedModuleStack(container.get(), type),
                card -> {
                    ItemStack host = container.get();
                    ModularHelper.setSelectedModuleStack(host, type, card);
                    writer.accept(host);
                },
                slots,
                this::cardLimit,
                true
        );
    }

    private int cardLimit() {
        ItemStack card = ModularHelper.getSelectedModuleStack(container.get(), type);
        return card.isEmpty()
                ? maxStack
                : dev.celli.enderutilities.item.ItemMemoryCard.getInventoryStackLimitFromContainerStack(card);
    }

    public boolean hasCard() {
        return !ModularHelper.getSelectedModuleStack(container.get(), type).isEmpty();
    }

    @Override
    public int getSlots() {
        return slots;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return hasCard() ? backing().getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return hasCard() ? backing().insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return hasCard() ? backing().extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return cardLimit();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return hasCard();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (hasCard()) {
            backing().setStackInSlot(slot, stack);
        }
    }
}
