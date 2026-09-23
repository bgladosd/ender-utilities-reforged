package dev.celli.enderutilities.inventory;

import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.celli.enderutilities.data.ModularItemData;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ModularModuleHandler implements IItemHandlerModifiable {
    private final Supplier<ItemStack> container;
    private final Consumer<ItemStack> writer;
    private final ModuleType type;
    private final int slotOffset;
    private final int slots;

    public ModularModuleHandler(Supplier<ItemStack> container, Consumer<ItemStack> writer, ModuleType type, int slots) {
        this(container, writer, type, 0, slots);
    }

    public ModularModuleHandler(
            Supplier<ItemStack> container,
            Consumer<ItemStack> writer,
            ModuleType type,
            int slotOffset,
            int slots
    ) {
        this.container = container;
        this.writer = writer;
        this.type = type;
        this.slotOffset = slotOffset;
        this.slots = slots;
    }

    private int absolute(int slot) {
        return slotOffset + slot;
    }

    private ModularItemData data() {
        return container.get().getOrDefault(ModDataComponents.MODULAR, ModularItemData.EMPTY);
    }

    private void set(ModularItemData data) {
        ItemStack stack = container.get();
        stack.set(ModDataComponents.MODULAR, data);
        writer.accept(stack);
    }

    @Override
    public int getSlots() {
        return slots;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return data().stackInSlot(absolute(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack) || !getStackInSlot(slot).isEmpty()) {
            return stack;
        }
        if (!simulate) {
            setStackInSlot(slot, stack.copyWithCount(1));
        }
        return stack.getCount() <= 1 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack existing = getStackInSlot(slot);
        if (existing.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            setStackInSlot(slot, ItemStack.EMPTY);
        }
        return existing;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IModule module && module.getModuleType(stack) == type;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        set(data().withSlot(absolute(slot), stack));
    }
}
