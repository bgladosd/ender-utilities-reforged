package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.block.HorizontalMachineBlock;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.item.ItemLinkCrystal;
import dev.celli.enderutilities.item.ItemMemoryCard;
import dev.celli.enderutilities.item.ItemQuickStacker;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.menu.QuickStackerAdvancedMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class QuickStackerAdvancedBlockEntity extends InventoryBlockEntity implements GuiActionHandler,
        HorizontalMachineBlock.NeighborAwareBlockEntity {
    public static final int TARGETS = 9;
    public static final int CARDS = 9;
    public static final int FILTERS = 36;
    public static final int FILTER_START = TARGETS + CARDS;

    public static final int GUI_ACTION_TOGGLE_TARGET_TYPE = 0;
    public static final int GUI_ACTION_TOGGLE_COLUMNS = 1;
    public static final int GUI_ACTION_TOGGLE_ROWS = 2;
    public static final int GUI_ACTION_TOGGLE_TARGET_ENABLED = 3;
    public static final int GUI_ACTION_SET_ACTIVE_TARGET = 4;
    public static final int GUI_ACTION_TOGGLE_FILTER_SETTINGS = 5;

    public static final int AREA_RANGE = 16;
    public static final int CRYSTAL_RANGE = 32;

    private static final int FILTER_ENABLED = 0x01;
    private static final int FILTER_BLACKLIST = 0x02;
    private static final int FILTER_META = 0x04;
    private static final int FILTER_NBT = 0x08;
    private static final int FILTER_DEFAULT = FILTER_ENABLED | FILTER_META;

    private boolean powered;
    private boolean areaMode;
    private int enabledTargetsMask;
    private int selectedTarget;
    private long slotMask;
    private int filterSettings = FILTER_DEFAULT;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> areaMode ? 1 : 0;
                case 1 -> currentFilterSettings();
                case 2 -> enabledTargetsMask;
                case 3 -> selectedTarget;
                case 4 -> (int) (slotMask >>> 32);
                case 5 -> (int) slotMask;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> areaMode = value != 0;
                case 1 -> filterSettings = value;
                case 2 -> enabledTargetsMask = value;
                case 3 -> selectedTarget = value;
                case 4 -> slotMask = (slotMask & 0xFFFFFFFFL) | ((long) value << 32);
                case 5 -> slotMask = (slotMask & 0xFFFFFFFF00000000L) | (value & 0xFFFFFFFFL);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public QuickStackerAdvancedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUICK_STACKER_ADVANCED.get(), pos, state,
                new LimitedItemStackHandler(FILTER_START + FILTERS, 1)
                        .validator((slot, stack) -> {
                            if (slot < TARGETS) {
                                return stack.isEmpty() || stack.getItem() instanceof ItemLinkCrystal;
                            }
                            if (slot < FILTER_START) {
                                return stack.isEmpty() || (stack.getItem() instanceof ItemMemoryCard card
                                        && card.getModuleType(stack) == ModuleType.MEMORY_CARD_MISC);
                            }
                            return true;
                        }));
        inventory.onChanged(this::setChanged);
        inventory.setVisibleSlots(FILTER_START + FILTERS);
    }

    public LimitedItemStackHandler getFilters() {
        return inventory;
    }

    public IItemHandler getFilterHandler() {
        return new CardBoundFilterHandler();
    }

    public int currentFilterSettings() {
        if (areaMode) {
            return filterSettings;
        }
        ItemStack card = inventory.getStackInSlot(TARGETS + selectedTarget);
        return card.getOrDefault(ModDataComponents.ITEM_MODE, FILTER_DEFAULT);
    }

    private void writeCurrentFilterSettings(int value) {
        if (areaMode) {
            filterSettings = value;
            return;
        }
        ItemStack card = inventory.getStackInSlot(TARGETS + selectedTarget);
        if (!card.isEmpty()) {
            card.set(ModDataComponents.ITEM_MODE, value);
            inventory.setStackInSlot(TARGETS + selectedTarget, card);
        }
    }

    public ContainerData getData() {
        return data;
    }

    public boolean isAreaMode() {
        return areaMode;
    }

    public int getEnabledTargetsMask() {
        return enabledTargetsMask;
    }

    public int getSelectedTarget() {
        return selectedTarget;
    }

    public long getEnabledSlotsMask() {
        return slotMask;
    }

    public void setEnabledSlotsMask(long mask) {
        this.slotMask = mask;
        setChanged();
    }

    public int getFilterSettings() {
        return filterSettings;
    }

    public boolean isInventoryAccessible() {
        return areaMode || !inventory.getStackInSlot(FILTER_START - CARDS + selectedTarget).isEmpty();
    }

    @Override
    public void onNeighborChanged() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean now = level.hasNeighborSignal(worldPosition);
        if (now && !powered) {
            Player player = level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, 8.0, false);
            if (player != null) {
                quickStack(player);
            }
        }
        powered = now;
    }

    public void quickStack(Player player) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (areaMode) {
            quickStackArea(server, player);
            return;
        }
        boolean usedCrystal = false;
        for (int i = 0; i < TARGETS; i++) {
            if ((enabledTargetsMask & (1 << i)) == 0) {
                continue;
            }
            ItemStack crystal = inventory.getStackInSlot(i);
            TargetLocation target = crystal.get(ModDataComponents.TARGET);
            if (target == null || !target.dimension().equals(server.dimension())) {
                continue;
            }
            BlockPos dest = target.pos();
            if (!server.hasChunkAt(dest) || !within(dest, CRYSTAL_RANGE) || !target.isTargetBlockUnchanged(server)) {
                continue;
            }
            IItemHandler handler = InventoryHelper.inventoryAt(server, dest, target.face());
            if (handler != null) {
                int targetIndex = i;
                ItemQuickStacker.quickStackMasked(player, handler, slotMask, !player.isShiftKeyDown(),
                        stack -> itemAllowed(stack, targetIndex));
                usedCrystal = true;
            }
        }
        if (!usedCrystal) {
            quickStackArea(server, player);
        }
    }

    private void quickStackArea(ServerLevel server, Player player) {
        int minCx = (worldPosition.getX() - AREA_RANGE) >> 4;
        int maxCx = (worldPosition.getX() + AREA_RANGE) >> 4;
        int minCz = (worldPosition.getZ() - AREA_RANGE) >> 4;
        int maxCz = (worldPosition.getZ() + AREA_RANGE) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!server.hasChunk(cx, cz)) {
                    continue;
                }
                for (var be : server.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (pos.equals(worldPosition) || !within(pos, AREA_RANGE)) {
                        continue;
                    }
                    IItemHandler handler = InventoryHelper.inventoryAt(server, pos, null);
                    if (handler != null) {
                        ItemQuickStacker.quickStackMasked(player, handler, slotMask, !player.isShiftKeyDown(),
                                stack -> itemAllowed(stack, -1));
                    }
                }
            }
        }
    }

    private boolean within(BlockPos dest, int range) {
        return Math.abs(dest.getX() - worldPosition.getX()) <= range
                && Math.abs(dest.getY() - worldPosition.getY()) <= range
                && Math.abs(dest.getZ() - worldPosition.getZ()) <= range;
    }

    private boolean itemAllowed(ItemStack stack, int targetIndex) {
        int settings;
        IItemHandler filters;
        if (areaMode || targetIndex < 0) {
            settings = filterSettings;
            filters = getFilterHandler();
        } else {
            ItemStack card = inventory.getStackInSlot(TARGETS + targetIndex);
            settings = card.isEmpty() ? FILTER_DEFAULT : card.getOrDefault(ModDataComponents.ITEM_MODE, FILTER_DEFAULT);
            filters = new dev.celli.enderutilities.inventory.ItemStackBackedHandler(
                    () -> inventory.getStackInSlot(TARGETS + targetIndex),
                    updated -> inventory.setStackInSlot(TARGETS + targetIndex, updated),
                    FILTERS,
                    1
            );
        }
        if ((settings & FILTER_ENABLED) == 0) {
            return true;
        }
        boolean matchMeta = (settings & FILTER_META) != 0;
        boolean matchNbt = (settings & FILTER_NBT) != 0;
        boolean blacklist = (settings & FILTER_BLACKLIST) != 0;
        boolean found = false;
        for (int i = 0; i < FILTERS; i++) {
            ItemStack filter = filters.getStackInSlot(i);
            if (filter.isEmpty()) {
                continue;
            }
            boolean same;
            if (!matchMeta) {
                same = filter.getItem() == stack.getItem();
            } else if (matchNbt) {
                same = ItemStack.isSameItemSameComponents(filter, stack);
            } else {
                same = ItemStack.isSameItem(filter, stack);
            }
            if (same) {
                found = true;
                break;
            }
        }
        return blacklist != found;
    }

    @Override
    public IItemHandler getHopperHandler() {
        return new LimitedItemStackHandler(0, 64);
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        switch (action) {
            case GUI_ACTION_TOGGLE_TARGET_TYPE -> areaMode = !areaMode;
            case GUI_ACTION_TOGGLE_COLUMNS -> {
                if (element >= 0 && element < 9) {
                    slotMask ^= 0x08040201L << element;
                }
            }
            case GUI_ACTION_TOGGLE_ROWS -> {
                if (element >= 0 && element <= 3) {
                    slotMask ^= 0x1FFL << (element * 9);
                }
            }
            case GUI_ACTION_TOGGLE_TARGET_ENABLED -> enabledTargetsMask ^= 1 << element;
            case GUI_ACTION_SET_ACTIVE_TARGET -> {
                if (element >= 0 && element < TARGETS) {
                    selectedTarget = element;
                }
            }
            case GUI_ACTION_TOGGLE_FILTER_SETTINGS -> {
                int bit = switch (element) {
                    case 0 -> FILTER_ENABLED;
                    case 1 -> FILTER_BLACKLIST;
                    case 2 -> FILTER_META;
                    case 3 -> FILTER_NBT;
                    default -> 0;
                };
                writeCurrentFilterSettings(currentFilterSettings() ^ bit);
            }
            default -> {
            }
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("AreaMode", areaMode);
        tag.putByte("EnabledTargets", (byte) enabledTargetsMask);
        tag.putByte("SelectedTarget", (byte) selectedTarget);
        tag.putLong("EnabledSlots", slotMask);
        tag.putByte("FilterMask", (byte) filterSettings);
        tag.putBoolean("Powered", powered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        areaMode = tag.getBoolean("AreaMode");
        enabledTargetsMask = tag.getByte("EnabledTargets");
        selectedTarget = tag.getByte("SelectedTarget");
        slotMask = tag.getLong("EnabledSlots");
        filterSettings = tag.contains("FilterMask") ? tag.getByte("FilterMask") : FILTER_DEFAULT;
        powered = tag.getBoolean("Powered");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new QuickStackerAdvancedMenu(id, playerInventory, this);
    }

    private final class CardBoundFilterHandler implements net.neoforged.neoforge.items.IItemHandlerModifiable {
        private IItemHandler backing() {
            if (areaMode) {
                return new IItemHandler() {
                    @Override
                    public int getSlots() {
                        return FILTERS;
                    }

                    @Override
                    public ItemStack getStackInSlot(int slot) {
                        return inventory.getStackInSlot(FILTER_START + slot);
                    }

                    @Override
                    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                        return inventory.insertItem(FILTER_START + slot, stack, simulate);
                    }

                    @Override
                    public ItemStack extractItem(int slot, int amount, boolean simulate) {
                        return inventory.extractItem(FILTER_START + slot, amount, simulate);
                    }

                    @Override
                    public int getSlotLimit(int slot) {
                        return 1;
                    }

                    @Override
                    public boolean isItemValid(int slot, ItemStack stack) {
                        return true;
                    }
                };
            }
            int cardSlot = TARGETS + selectedTarget;
            return new dev.celli.enderutilities.inventory.ItemStackBackedHandler(
                    () -> inventory.getStackInSlot(cardSlot),
                    stack -> inventory.setStackInSlot(cardSlot, stack),
                    FILTERS,
                    1
            );
        }

        @Override
        public int getSlots() {
            return FILTERS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return backing().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return backing().insertItem(slot, stack.copyWithCount(1), simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return backing().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            IItemHandler backing = backing();
            if (backing instanceof net.neoforged.neoforge.items.IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            } else if (areaMode) {
                inventory.setStackInSlot(FILTER_START + slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            }
        }
    }
}
