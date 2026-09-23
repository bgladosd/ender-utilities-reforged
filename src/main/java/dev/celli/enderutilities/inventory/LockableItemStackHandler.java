package dev.celli.enderutilities.inventory;

import java.util.BitSet;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class LockableItemStackHandler extends LimitedItemStackHandler {
    private NonNullList<ItemStack> templateStacks;
    private BitSet locked;

    public LockableItemStackHandler(int size, int stackLimit) {
        super(size, stackLimit);
        this.templateStacks = NonNullList.withSize(size, ItemStack.EMPTY);
        this.locked = new BitSet(size);
    }

    public boolean isSlotLocked(int slot) {
        return slot >= 0 && slot < getSlots() && locked.get(slot);
    }

    public void setSlotLocked(int slot, boolean isLocked) {
        if (slot < 0 || slot >= getSlots()) {
            return;
        }
        locked.set(slot, isLocked);
        onContentsChanged(slot);
    }

    public boolean toggleSlotLocked(int slot) {
        boolean next = !isSlotLocked(slot);
        setSlotLocked(slot, next);
        return next;
    }

    public ItemStack getTemplateStackInSlot(int slot) {
        if (slot < 0 || slot >= templateStacks.size()) {
            return ItemStack.EMPTY;
        }
        return templateStacks.get(slot);
    }

    public void setTemplateStackInSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= templateStacks.size()) {
            return;
        }
        if (stack.isEmpty()) {
            templateStacks.set(slot, ItemStack.EMPTY);
        } else {
            ItemStack template = stack.copy();
            template.setCount(1);
            templateStacks.set(slot, template);
        }
        onContentsChanged(slot);
    }

    public void toggleLockFromClick(int slot, ItemStack cursor, ItemStack slotStack) {
        setTemplateStackInSlot(slot, cursor.isEmpty() ? slotStack : cursor);
        toggleSlotLocked(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!super.isItemValid(slot, stack)) {
            return false;
        }
        return !isSlotLocked(slot) || ItemStack.isSameItemSameComponents(getTemplateStackInSlot(slot), stack);
    }

    @Override
    public void ensureSize(int size) {
        super.ensureSize(size);
        if (templateStacks.size() != size) {
            NonNullList<ItemStack> next = NonNullList.withSize(size, ItemStack.EMPTY);
            int copy = Math.min(templateStacks.size(), size);
            for (int i = 0; i < copy; i++) {
                next.set(i, templateStacks.get(i));
            }
            templateStacks = next;
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = super.serializeNBT(provider);
        ListTag templates = new ListTag();
        for (int i = 0; i < templateStacks.size(); i++) {
            ItemStack stack = templateStacks.get(i);
            if (!stack.isEmpty()) {
                Tag saved = stack.save(provider);
                if (saved instanceof CompoundTag item) {
                    item.putByte("Slot", (byte) i);
                    templates.add(item);
                }
            }
        }
        nbt.put("TemplateItems", templates);
        nbt.putByteArray("LockedSlots", locked.toByteArray());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        super.deserializeNBT(provider, nbt);
        int size = Math.max(getSlots(), stacks.size());
        templateStacks = NonNullList.withSize(size, ItemStack.EMPTY);
        locked = new BitSet(size);
        ListTag templates = nbt.getList("TemplateItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < templates.size(); i++) {
            CompoundTag item = templates.getCompound(i);
            int slot = item.getByte("Slot") & 0xFF;
            if (slot < templateStacks.size()) {
                templateStacks.set(slot, ItemStack.parseOptional(provider, item));
            }
        }
        locked.or(BitSet.valueOf(nbt.getByteArray("LockedSlots")));
    }
}
