package dev.celli.enderutilities.blockentity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.inventory.ItemStackBackedHandler;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.item.ItemMemoryCard;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.menu.HandyChestMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class HandyChestBlockEntity extends InventoryBlockEntity implements GuiActionHandler {
    public static final int GUI_ACTION_SELECT_MODULE = 0;
    public static final int GUI_ACTION_LOCK_MODULE = 1;
    public static final int GUI_ACTION_MOVE_ITEMS = 2;
    public static final int GUI_ACTION_SET_QUICK_ACTION = 3;
    public static final int GUI_ACTION_SORT_ITEMS = 4;

    public static final int[] INV_SIZES = {18, 36, 54, 104};
    public static final int CARD_SLOTS = 4;
    private final int tier;
    private int selectedModule;
    private int lockMask;
    private int actionMode;
    private final ChestCardHandler itemHandler = new ChestCardHandler();
    private final Map<UUID, Long> clickTimes = new HashMap<>();

    public HandyChestBlockEntity(BlockPos pos, BlockState state, int tier) {
        super(ModBlockEntities.HANDY_CHEST.get(), pos, state, new LimitedItemStackHandler(CARD_SLOTS, 1)
                .validator((slot, stack) -> stack.getItem() instanceof ItemMemoryCard card
                        && card.getModuleType(stack) == ModuleType.MEMORY_CARD_ITEMS));
        this.tier = tier;
        inventory.onChanged(this::setChanged);
    }

    public int getStorageTier() {
        return tier;
    }

    public int getInvSize() {
        return INV_SIZES[tier];
    }

    public int getSelectedModule() {
        return selectedModule;
    }

    public void setSelectedModule(int index) {
        selectedModule = Mth.clamp(index, 0, CARD_SLOTS - 1);
    }

    public int getLockMask() {
        return lockMask;
    }

    public void setLockMask(int mask) {
        lockMask = mask;
    }

    public int getQuickMode() {
        return actionMode;
    }

    public void setQuickMode(int mode) {
        actionMode = mode;
    }

    public LimitedItemStackHandler getCardInventory() {
        return inventory;
    }

    public ItemStack getSelectedCard() {
        return inventory.getStackInSlot(selectedModule);
    }

    public int stackLimitFromSelectedCard() {
        return ItemMemoryCard.getInventoryStackLimitFromContainerStack(getSelectedCard());
    }

    public boolean isInventoryAccessible(Player player) {
        ItemStack card = getSelectedCard();
        if (card.isEmpty()) {
            return false;
        }
        OwnerInfo owner = card.get(ModDataComponents.OWNER);
        return owner == null || owner.canAccess(player);
    }

    public IItemHandlerModifiable getItemHandler() {
        return itemHandler;
    }

    @Override
    public IItemHandler getHopperHandler() {
        ItemStack card = getSelectedCard();
        if (card.isEmpty()) {
            return new LimitedItemStackHandler(0, 64);
        }
        OwnerInfo owner = card.get(ModDataComponents.OWNER);
        if (owner != null && !owner.isPublic()) {
            return new LimitedItemStackHandler(0, 64);
        }
        return itemHandler;
    }

    public void onLeftClickBlock(Player player) {
        if (level == null || level.isClientSide) {
            return;
        }
        Long last = clickTimes.get(player.getUUID());
        long now = level.getGameTime();
        if (last != null && now - last < 5) {
            performGuiAction(player, GUI_ACTION_MOVE_ITEMS, actionMode);
            level.playSound(null, worldPosition, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.2F, 1.8F);
            clickTimes.remove(player.getUUID());
        } else {
            clickTimes.put(player.getUUID(), now);
        }
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        if (action == GUI_ACTION_SELECT_MODULE && element >= 0 && element < CARD_SLOTS) {
            setSelectedModule(element);
        } else if (action == GUI_ACTION_LOCK_MODULE && element >= 0 && element < CARD_SLOTS) {
            toggleCardLocked(player, element);
        } else if (action == GUI_ACTION_MOVE_ITEMS && element >= 0 && element < 6) {
            moveItems(player, element);
        } else if (action == GUI_ACTION_SET_QUICK_ACTION && element >= 0 && element < 6) {
            actionMode = element;
        } else if (action == GUI_ACTION_SORT_ITEMS && element >= 0 && element <= 1) {
            sortItems(player, element);
        }
        setChanged();
    }

    private void toggleCardLocked(Player player, int cardIndex) {
        ItemStack card = inventory.getStackInSlot(cardIndex);
        if (card.isEmpty()) {
            return;
        }
        OwnerInfo owner = card.get(ModDataComponents.OWNER);
        if (owner != null && owner.isOwner(player)) {
            lockMask ^= 1 << cardIndex;
        }
    }

    private void moveItems(Player player, int element) {
        if (!isInventoryAccessible(player)) {
            return;
        }
        IItemHandlerModifiable playerInv = InventoryHelper.playerMainAndOffhand(player);
        switch (element) {
            case 0 -> InventoryHelper.moveAll(playerInv, itemHandler, 9, 36);
            case 1 -> InventoryHelper.moveMatching(playerInv, itemHandler, 9, 36);
            case 2 -> InventoryHelper.leaveOneFullStack(playerInv, itemHandler);
            case 3 -> InventoryHelper.fillMatching(itemHandler, playerInv);
            case 4 -> InventoryHelper.moveMatching(itemHandler, playerInv, 0, itemHandler.getSlots());
            case 5 -> InventoryHelper.moveAll(itemHandler, playerInv, 0, itemHandler.getSlots());
            default -> {
            }
        }
    }

    private void sortItems(Player player, int element) {
        if (element == 0) {
            if (!isInventoryAccessible(player)) {
                return;
            }
            InventoryHelper.sortRange(itemHandler, 0, getInvSize());
            return;
        }
        InventoryHelper.sortRange(new net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()), 9, 36);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("ChestTier", (byte) tier);
        tag.putByte("QuickMode", (byte) actionMode);
        tag.putByte("SelModule", (byte) selectedModule);
        tag.putByte("LockMask", (byte) lockMask);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selectedModule = Mth.clamp(tag.getByte("SelModule"), 0, CARD_SLOTS - 1);
        lockMask = tag.getByte("LockMask");
        actionMode = tag.getByte("QuickMode");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new HandyChestMenu(id, playerInventory, this);
    }

    /**
     * Stable handler bound to the selected card. Empty/inaccessible cards reject inserts so
     * items never vanish into a throwaway inventory (orig {@code InventoryItem.isItemValidForSlot}).
     */
    private final class ChestCardHandler implements IItemHandlerModifiable {
        private ItemStackBackedHandler backing() {
            return new ItemStackBackedHandler(
                    HandyChestBlockEntity.this::getSelectedCard,
                    stack -> inventory.setStackInSlot(selectedModule, stack),
                    getInvSize(),
                    HandyChestBlockEntity.this::stackLimitFromSelectedCard,
                    true
            );
        }

        private boolean hasCard() {
            return !getSelectedCard().isEmpty();
        }

        @Override
        public int getSlots() {
            return getInvSize();
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
            return hasCard() ? stackLimitFromSelectedCard() : 64;
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
}
