package dev.celli.enderutilities.inventory;

import java.util.function.Supplier;

import dev.celli.enderutilities.item.module.IModular;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Installed-module slots are a view of the held tool's {@code MODULAR} component.
 * Orig Tool Workstation wrote clicks into the tool NBT, not into a separate BE inventory.
 */
public class InstalledToolModulesHandler implements IItemHandlerModifiable {
    public static final int SLOTS = 10;

    private final Supplier<ItemStack> tool;
    private final Runnable onChanged;

    public InstalledToolModulesHandler(Supplier<ItemStack> tool, Runnable onChanged) {
        this.tool = tool;
        this.onChanged = onChanged;
    }

    @Override
    public int getSlots() {
        return SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack container = tool.get();
        if (!(container.getItem() instanceof IModular)) {
            return ItemStack.EMPTY;
        }
        return ModularHelper.data(container).stackInSlot(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        ItemStack container = tool.get();
        if (!(container.getItem() instanceof IModular)) {
            return;
        }
        ModularHelper.setData(container, ModularHelper.data(container).withSlot(slot, stack));
        onChanged.run();
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
        ItemStack container = tool.get();
        if (!(container.getItem() instanceof IModular modular) || !(stack.getItem() instanceof IModule module)) {
            return false;
        }
        ModuleType requested = slotType(modular, container, slot);
        return requested != ModuleType.INVALID
                && module.getModuleType(stack) == requested
                && modular.getMaxModules(container, stack) > 0;
    }

    public static ModuleType slotType(IModular modular, ItemStack tool, int moduleIndex) {
        int slot = 0;
        for (ModuleType type : ModuleType.values()) {
            if (type == ModuleType.INVALID) {
                continue;
            }
            int max = modular.getMaxModules(tool, type);
            if (moduleIndex >= slot && moduleIndex < slot + max) {
                return type;
            }
            slot += max;
        }
        return ModuleType.INVALID;
    }

    public static int firstSlotOfType(IModular modular, ItemStack tool, ModuleType wanted) {
        int slot = 0;
        for (ModuleType type : ModuleType.values()) {
            if (type == ModuleType.INVALID) {
                continue;
            }
            if (type == wanted) {
                return slot;
            }
            slot += modular.getMaxModules(tool, type);
        }
        return 0;
    }
}
