package dev.celli.enderutilities.blockentity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.menu.BarrelMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import dev.celli.enderutilities.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class BarrelBlockEntity extends InventoryBlockEntity implements GuiActionHandler, CamouflageHolder {
    private final LimitedItemStackHandler upgrades = new LimitedItemStackHandler(4, 64);
    private boolean creative;
    private boolean locked;
    private int labelMask;
    private final Map<UUID, Long> clickTimes = new HashMap<>();
    private final CamouflageStorage camouflage = new CamouflageStorage();

    public BarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL.get(), pos, state, new LimitedItemStackHandler(1, 64 * 64));
        inventory.onChanged(() -> {
            refreshCapacity();
            CamouflageStorage.sync(this);
        });
        upgrades.onChanged(() -> {
            refreshCapacity();
            pruneLabels();
        });
        updateLabels();
    }

    public LimitedItemStackHandler getUpgrades() {
        return upgrades;
    }

    public boolean isCreative() {
        return creative;
    }

    public void setCreative(boolean creative) {
        this.creative = creative;
    }

    public boolean hasVoidUpgrade() {
        return !upgrades.getStackInSlot(3).isEmpty();
    }

    public boolean retainsContentsWhenBroken() {
        return !upgrades.getStackInSlot(1).isEmpty();
    }

    public boolean isOverSpillCapacity() {
        ItemStack stored = inventory.getStackInSlot(0);
        return !stored.isEmpty() && stored.getCount() > 4096;
    }

    private void refreshCapacity() {
        int upgradesCount = upgrades.getStackInSlot(2).getCount();
        int stacks = 64 + upgradesCount * ModCommonConfig.BARREL_CAPACITY_UPGRADE_STACKS_PER.get();
        ItemStack stored = inventory.getStackInSlot(0);
        int per = stored.isEmpty() ? 64 : stored.getMaxStackSize();
        inventory.setStackLimit(stacks * per);
        setChanged();
    }

    public void onRightClick(Player player, Direction side) {
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            if (held.is(ModItems.STORAGE_KEY.get())) {
                locked = !locked;
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7F, 1.0F);
                setChanged();
                CamouflageStorage.sync(this);
                return;
            }
            if (held.is(ModItems.STORAGE_KEY_CREATIVE.get())) {
                creative = !creative;
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7F, 1.0F);
                setChanged();
                CamouflageStorage.sync(this);
                return;
            }
            if (held.is(ModItems.BARREL_LABEL.get()) && tryApplyLabel(player, held, side)) {
                return;
            }
            if (tryApplyUpgrade(player, held)) {
                return;
            }
            ItemStack leftover = getHopperHandler().insertItem(0, held.copy(), false);
            player.setItemInHand(player.getUsedItemHand(), leftover);
        }
        long now = System.currentTimeMillis();
        Long last = clickTimes.get(player.getUUID());
        if (last != null && now - last < 300) {
            IItemHandler playerInv = player.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY);
            if (playerInv != null) {
                for (int i = 0; i < playerInv.getSlots(); i++) {
                    ItemStack stack = playerInv.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ItemStack leftover = getHopperHandler().insertItem(0, stack.copy(), false);
                        playerInv.extractItem(i, stack.getCount() - leftover.getCount(), false);
                    }
                }
            }
        }
        clickTimes.put(player.getUUID(), now);
    }

    public int getLabelMask() {
        updateLabels();
        return labelMask;
    }

    public boolean hasLabel(Direction side) {
        return (getLabelMask() & (1 << side.get3DDataValue())) != 0;
    }

    public ItemStack getRenderStack() {
        return inventory.getStackInSlot(0);
    }

    public Direction getFacing() {
        if (getBlockState().hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
            return getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        }
        return Direction.NORTH;
    }

    private boolean tryApplyLabel(Player player, ItemStack held, Direction side) {
        Direction facing = getFacing();
        if (side == facing || hasLabel(side)) {
            return false;
        }
        if (tryApplyUpgrade(player, held)) {
            labelMask |= 1 << side.get3DDataValue();
            updateLabels();
            CamouflageStorage.sync(this);
            return true;
        }
        return false;
    }

    private void pruneLabels() {
        int extras = Math.max(0, Integer.bitCount(labelMask) - 1);
        int stored = upgrades.getStackInSlot(0).getCount();
        if (stored < extras) {
            labelMask = 0;
            updateLabels();
        }
    }

    private void updateLabels() {
        Direction facing = getFacing();
        labelMask |= 1 << facing.get3DDataValue();
        setChanged();
    }

    private boolean tryApplyUpgrade(Player player, ItemStack held) {
        int slot = -1;
        if (held.is(ModItems.BARREL_LABEL.get())) {
            slot = 0;
        } else if (held.is(ModItems.BARREL_STRUCTURAL.get())) {
            slot = 1;
        } else if (held.is(ModItems.BARREL_CAPACITY.get())) {
            slot = 2;
        } else if (held.is(ModItems.BARREL_VOID.get())) {
            slot = 3;
        }
        if (slot < 0) {
            return false;
        }
        ItemStack one = held.copyWithCount(1);
        if (upgrades.insertItem(slot, one, false).isEmpty()) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
            refreshCapacity();
            return true;
        }
        return false;
    }

    @Override
    public IItemHandler getHopperHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return inventory.getStackInSlot(0);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (locked && !ItemStack.isSameItemSameComponents(inventory.getStackInSlot(0), stack)
                        && !inventory.getStackInSlot(0).isEmpty()) {
                    return stack;
                }
                ItemStack leftover = inventory.insertItem(0, stack, simulate);
                if (hasVoidUpgrade()) {
                    return ItemStack.EMPTY;
                }
                if (creative) {
                    return ItemStack.EMPTY;
                }
                return leftover;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (creative) {
                    ItemStack existing = inventory.getStackInSlot(0);
                    return existing.isEmpty() ? ItemStack.EMPTY : existing.copyWithCount(Math.min(amount, existing.getCount()));
                }
                return inventory.extractItem(0, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return inventory.getSlotLimit(0);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return true;
            }
        };
    }

    @Override
    public void dropContents() {
        if (creative || retainsContentsWhenBroken()) {
            return;
        }
        ItemStack stored = inventory.getStackInSlot(0);
        if (!stored.isEmpty() && stored.getCount() > 4096) {
            inventory.setStackInSlot(0, stored.copyWithCount(4096));
        }
        super.dropContents();
        EnderMenuDrop.drop(upgrades, level, worldPosition);
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        if (action == 0) {
            ItemStack taken = getHopperHandler().extractItem(0, element == 1 ? 64 : 1, false);
            if (!taken.isEmpty() && level != null) {
                ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, taken);
                level.addFreshEntity(entity);
            }
        } else if (action == 1 && player.getAbilities().instabuild) {
            creative = !creative;
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ItemsUpgrades", upgrades.serializeNBT(registries));
        tag.putBoolean("Creative", creative);
        tag.putBoolean("Locked", locked);
        tag.putByte("Labels", (byte) getLabelMask());
        camouflage.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ItemsUpgrades")) {
            upgrades.deserializeNBT(registries, tag.getCompound("ItemsUpgrades"));
        }
        creative = tag.getBoolean("Creative");
        locked = tag.getBoolean("Locked");
        labelMask = tag.getByte("Labels") & 0xFF;
        camouflage.load(tag, registries);
        refreshCapacity();
        updateLabels();
    }

    @Override
    public BlockState getCamouflage() {
        return camouflage.get();
    }

    @Override
    public void setCamouflage(@org.jetbrains.annotations.Nullable BlockState camouflage) {
        this.camouflage.set(camouflage);
        CamouflageStorage.sync(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        camouflage.save(tag, registries);
        tag.putByte("Labels", (byte) getLabelMask());
        tag.put("Inventory", inventory.serializeNBT(registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        camouflage.load(tag, registries);
        if (tag.contains("Labels")) {
            labelMask = tag.getByte("Labels") & 0xFF;
        }
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        updateLabels();
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new BarrelMenu(id, playerInventory, this);
    }

    private static final class EnderMenuDrop {
        static void drop(IItemHandler handler, net.minecraft.world.level.Level level, BlockPos pos) {
            dev.celli.enderutilities.menu.EnderMenu.dropContents(handler, level, pos);
        }
    }
}
