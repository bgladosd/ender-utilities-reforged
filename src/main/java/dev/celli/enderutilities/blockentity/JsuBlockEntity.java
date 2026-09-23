package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.menu.JsuMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class JsuBlockEntity extends InventoryBlockEntity {
    public static final int INV_SIZE = 270;
    public static final int MAX_STACK_SIZE = 256;
    public static final int VISIBLE_SLOTS = 54;

    public JsuBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JSU.get(), pos, state,
                new LimitedItemStackHandler(INV_SIZE, MAX_STACK_SIZE).validator((slot, stack) -> stack.getMaxStackSize() == 1));
        inventory.onChanged(this::setChanged);
    }

    @Override
    public IItemHandler getHopperHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return inventory.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return inventory.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (stack.getMaxStackSize() != 1) {
                    return stack;
                }
                return inventory.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return inventory.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return MAX_STACK_SIZE;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getMaxStackSize() == 1;
            }
        };
    }

    @Override
    public void dropContents() {
        // Orig EnumStorageType.JSU retainsContents=true: contents stay on the dropped block item.
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new JsuMenu(id, playerInventory, this);
    }
}
