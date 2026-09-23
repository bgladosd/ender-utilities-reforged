package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.menu.AsuMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;

public class AsuBlockEntity extends InventoryBlockEntity implements GuiActionHandler {
    public static final int MAX_INV_SIZE = 27;
    public static final int MAX_STACK_SIZE = 1024;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> inventory.getVisibleSlots();
                case 1 -> inventory.getStackLimit();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                inventory.setVisibleSlots(value);
            } else if (index == 1) {
                inventory.setStackLimit(value);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public AsuBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASU.get(), pos, state, new LimitedItemStackHandler(MAX_INV_SIZE, 1));
        inventory.onChanged(this::setChanged);
        inventory.setVisibleSlots(1);
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        if (action == 0) {
            int newSize = Mth.clamp(inventory.getVisibleSlots() + element, 1, MAX_INV_SIZE);
            if (element < 0) {
                int change = 0;
                for (int slot = inventory.getVisibleSlots() - 1; slot >= newSize && slot >= 1; slot--) {
                    if (inventory.getStackInSlot(slot).isEmpty()) {
                        change--;
                    } else {
                        break;
                    }
                }
                newSize = Mth.clamp(inventory.getVisibleSlots() + change, 1, MAX_INV_SIZE);
            }
            inventory.setVisibleSlots(newSize);
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        } else if (action == 1) {
            inventory.setStackLimit(Mth.clamp(inventory.getStackLimit() + element, 1, MAX_STACK_SIZE));
        }
        setChanged();
    }

    @Override
    public void dropContents() {
        // Orig BlockASU: contents stay on the dropped block item.
    }

    public void applyPlacementProperties(int stackLimit, int slots) {
        inventory.setStackLimit(Mth.clamp(stackLimit, 1, MAX_STACK_SIZE));
        inventory.setVisibleSlots(Mth.clamp(slots, 1, MAX_INV_SIZE));
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("Tier", (byte) inventory.getVisibleSlots());
        tag.putInt("StackLimit", inventory.getStackLimit());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        inventory.setVisibleSlots(Mth.clamp(tag.getByte("Tier"), 1, MAX_INV_SIZE));
        inventory.setStackLimit(Mth.clamp(tag.getInt("StackLimit"), 1, MAX_STACK_SIZE));
        super.loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AsuMenu(id, playerInventory, this);
    }
}
