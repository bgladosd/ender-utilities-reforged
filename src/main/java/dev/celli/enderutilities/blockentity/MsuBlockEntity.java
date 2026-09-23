package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.menu.MsuMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import dev.celli.enderutilities.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class MsuBlockEntity extends InventoryBlockEntity implements GuiActionHandler {
    private final int tier;
    private boolean creative;

    public MsuBlockEntity(BlockPos pos, BlockState state, int tier) {
        super(ModBlockEntities.MSU.get(), pos, state,
                new LimitedItemStackHandler(tier == 1 ? 9 : 1, ModCommonConfig.MSU_MAX_ITEMS.get()));
        this.tier = tier;
        inventory.onChanged(this::setChanged);
        inventory.setStackLimit(ModCommonConfig.MSU_MAX_ITEMS.get());
    }

    public int getStorageTier() {
        return tier;
    }

    public boolean isCreative() {
        return creative;
    }

    public void setCreative(boolean creative) {
        this.creative = creative;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean tryCreativeKey(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.STORAGE_KEY_CREATIVE.get())) {
            setCreative(!creative);
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return true;
        }
        return false;
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
                ItemStack leftover = inventory.insertItem(slot, stack, simulate);
                if (creative) {
                    return ItemStack.EMPTY;
                }
                return leftover;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (creative) {
                    ItemStack existing = inventory.getStackInSlot(slot);
                    if (existing.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    return existing.copyWithCount(Math.min(amount, existing.getCount()));
                }
                return inventory.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return inventory.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return inventory.isItemValid(slot, stack);
            }
        };
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        if (action == 1 && player.getAbilities().instabuild) {
            setCreative(!creative);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Creative", creative);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        creative = tag.getBoolean("Creative");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("Tier", (byte) tier);
        tag.putBoolean("Creative", creative);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        creative = tag.getBoolean("Creative");
        super.loadAdditional(tag, registries);
        inventory.setStackLimit(ModCommonConfig.MSU_MAX_ITEMS.get());
    }

    @Override
    public void dropContents() {
        // Orig BlockMSU: contents stay on the dropped block item.
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MsuMenu(id, playerInventory, this);
    }
}
