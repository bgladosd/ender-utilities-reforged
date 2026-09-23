package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.inventory.LockableItemStackHandler;
import dev.celli.enderutilities.menu.MemoryChestMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class MemoryChestBlockEntity extends InventoryBlockEntity implements GuiActionHandler {
    public static final int[] INV_SIZES = {9, 27, 54};
    public static final int GUI_ACTION_TOGGLE_LOCKED = 1;
    private final int tier;
    private OwnerInfo owner;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return isPublic() ? 1 : 0;
        }

        @Override
        public void set(int index, int value) {
            setIsPublic(value == 1);
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public MemoryChestBlockEntity(BlockPos pos, BlockState state, int tier) {
        super(ModBlockEntities.MEMORY_CHEST.get(), pos, state, new LockableItemStackHandler(INV_SIZES[tier], 64));
        this.tier = tier;
        inventory.onChanged(this::setChanged);
    }

    public int getStorageTier() {
        return tier;
    }

    public LockableItemStackHandler getLockable() {
        return (LockableItemStackHandler) inventory;
    }

    public ContainerData getData() {
        return data;
    }

    public boolean isPublic() {
        return owner == null || owner.isPublic();
    }

    public void setIsPublic(boolean value) {
        if (owner != null) {
            owner = owner.withPublic(value);
        }
    }

    public boolean canAccess(Player player) {
        return owner == null || owner.canAccess(player) || player.getAbilities().instabuild;
    }

    public String getOwnerName() {
        return owner == null ? "" : owner.name();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("enderutilities.container.memory_chest");
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
                return isPublic() ? inventory.getStackInSlot(slot) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return isPublic() ? inventory.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return isPublic() ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return inventory.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isPublic() && inventory.isItemValid(slot, stack);
            }
        };
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        if (action == GUI_ACTION_TOGGLE_LOCKED) {
            if (owner == null) {
                owner = OwnerInfo.of(player, false);
            } else if (player.getAbilities().instabuild || owner.isOwner(player)) {
                owner = owner.withPublic(!owner.isPublic());
            }
            setChanged();
            syncToClient();
        }
    }

    public void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("ChestTier", (byte) tier);
        if (owner != null) {
            tag.putUUID("Owner", owner.owner());
            tag.putString("OwnerName", owner.name());
            tag.putBoolean("Public", owner.isPublic());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        getLockable().ensureSize(INV_SIZES[tier]);
        if (tag.hasUUID("Owner")) {
            owner = new OwnerInfo(tag.getUUID("Owner"), tag.getString("OwnerName"), tag.getBoolean("Public"));
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        if (!canAccess(player)) {
            return null;
        }
        return new MemoryChestMenu(id, playerInventory, this);
    }
}
