package dev.celli.enderutilities.blockentity;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.block.HorizontalMachineBlock;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.menu.InserterMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import dev.celli.enderutilities.util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class InserterBlockEntity extends InventoryBlockEntity implements GuiActionHandler,
        HorizontalMachineBlock.ServerTickingBlockEntity, HorizontalMachineBlock.NeighborAwareBlockEntity {
    public static final int FILTER_SLOTS = 27;
    private final boolean filtered;
    private final LimitedItemStackHandler filters;
    private final List<Direction> enabledSides = new ArrayList<>();
    private final List<Direction> validSides = new ArrayList<>();
    private int delay = 4;
    private int cooldown;
    private int filterMask = 0x04;
    private int redstoneMode;
    private int outputSideIndex;
    private boolean disableScheduling;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> delay;
                case 1 -> inventory.getStackLimit();
                case 2 -> filterMask;
                case 3 -> redstoneMode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> delay = value;
                case 1 -> inventory.setStackLimit(value);
                case 2 -> filterMask = value;
                case 3 -> redstoneMode = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public InserterBlockEntity(BlockPos pos, BlockState state, boolean filtered) {
        super(ModBlockEntities.INSERTER.get(), pos, state, new LimitedItemStackHandler(1, 1));
        this.filtered = filtered;
        this.filters = new LimitedItemStackHandler(FILTER_SLOTS, 64);
        inventory.onChanged(() -> {
            setChanged();
            if (!disableScheduling) {
                cooldown = 0;
            }
        });
        filters.onChanged(this::setChanged);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateValidSides();
    }

    public boolean isFiltered() {
        return filtered;
    }

    public LimitedItemStackHandler getFilters() {
        return filters;
    }

    public ContainerData getData() {
        return data;
    }

    public Direction getFacing() {
        return getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    public void toggleOutputSide(Direction side) {
        Direction facing = getFacing();
        if (side == facing || side == facing.getOpposite()) {
            return;
        }
        if (enabledSides.contains(side)) {
            enabledSides.remove(side);
        } else {
            enabledSides.add(side);
        }
        updateValidSides();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public List<Direction> getEnabledOutputSides() {
        return enabledSides;
    }

    public void applyPlacementProperties(int delay, int redstoneMode, int stackLimit) {
        this.delay = Mth.clamp(delay, 0, 72000);
        this.redstoneMode = Mth.clamp(redstoneMode, 0, 2);
        inventory.setStackLimit(Mth.clamp(stackLimit, 1, 64));
        cooldown = 0;
        setChanged();
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
                ItemStack leftover = inventory.insertItem(0, stack, simulate);
                if (!simulate) {
                    cooldown = 0;
                }
                return leftover;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
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
    public void onNeighborChanged() {
        cooldown = 0;
        updateValidSides();
    }

    private void updateValidSides() {
        validSides.clear();
        if (level == null) {
            return;
        }
        for (Direction side : enabledSides) {
            if (InventoryHelper.inventoryAt(level, worldPosition.relative(side), side.getOpposite()) != null) {
                validSides.add(side);
            }
        }
    }

    private boolean shouldOperate() {
        if (level == null) {
            return false;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        return redstoneMode == 0 || (redstoneMode == 1 && !powered) || (redstoneMode == 2 && powered);
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide || !shouldOperate()) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        boolean moved = inventory.getStackInSlot(0).isEmpty() ? tryPull() : tryPush();
        if (moved) {
            cooldown = delay;
            setChanged();
        }
    }

    private boolean tryPull() {
        Direction facing = getFacing();
        IItemHandler source = InventoryHelper.inventoryAt(level, worldPosition.relative(facing.getOpposite()), facing);
        if (source == null || level.getBlockEntity(worldPosition.relative(facing.getOpposite())) instanceof InserterBlockEntity) {
            return false;
        }
        disableScheduling = true;
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (filtered && isInputFilter() && !itemAllowed(stack)) {
                continue;
            }
            ItemStack extracted = source.extractItem(slot, inventory.getStackLimit(), false);
            if (!extracted.isEmpty()) {
                inventory.insertItem(0, extracted, false);
                disableScheduling = false;
                return true;
            }
        }
        disableScheduling = false;
        return false;
    }

    private boolean tryPush() {
        ItemStack held = inventory.getStackInSlot(0);
        boolean toSides = true;
        if (filtered && !isInputFilter()) {
            toSides = itemAllowed(held);
        }
        List<Direction> sides = validSides.isEmpty() ? enabledSides : validSides;
        if (toSides && !sides.isEmpty()) {
            for (int i = 0; i < sides.size(); i++) {
                if (outputSideIndex >= sides.size()) {
                    outputSideIndex = 0;
                }
                Direction side = sides.get(outputSideIndex);
                outputSideIndex++;
                if (pushTo(side)) {
                    return true;
                }
            }
        }
        return pushTo(getFacing());
    }

    private boolean pushTo(Direction side) {
        IItemHandler dest = InventoryHelper.inventoryAt(level, worldPosition.relative(side), side.getOpposite());
        if (dest == null) {
            return false;
        }
        disableScheduling = true;
        ItemStack stack = inventory.extractItem(0, 64, false);
        int orig = stack.getCount();
        ItemStack leftover = ItemHandlerHelper.insertItemStacked(dest, stack, false);
        boolean moved = leftover.getCount() != orig || leftover.isEmpty();
        if (!leftover.isEmpty()) {
            inventory.insertItem(0, leftover, false);
        }
        disableScheduling = false;
        return moved;
    }

    private boolean isInputFilter() {
        return (filterMask & 0x01) != 0;
    }

    private boolean isWhitelist() {
        return (filterMask & 0x02) != 0;
    }

    private boolean matchMeta() {
        return (filterMask & 0x04) != 0;
    }

    private boolean matchNbt() {
        return (filterMask & 0x08) != 0;
    }

    private boolean itemAllowed(ItemStack stack) {
        boolean match = false;
        for (int i = 0; i < filters.getSlots(); i++) {
            ItemStack filter = filters.getStackInSlot(i);
            if (filter.isEmpty()) {
                continue;
            }
            boolean same = matchNbt()
                    ? ItemStack.isSameItemSameComponents(filter, stack)
                    : ItemStack.isSameItem(filter, stack);
            if (!matchMeta() && filter.is(stack.getItem())) {
                same = !matchNbt() || ItemStack.isSameItemSameComponents(filter, stack);
            }
            if (same) {
                match = true;
                break;
            }
        }
        return match == isWhitelist();
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        switch (action) {
            case 0 -> delay = Mth.clamp(delay + element, 0, 72000);
            case 1 -> inventory.setStackLimit(Mth.clamp(inventory.getStackLimit() + element, 1, 64));
            case 2 -> filterMask = (filterMask ^ element) & 0xF;
            case 3 -> redstoneMode = Math.floorMod(redstoneMode + element, 3);
            default -> {
            }
        }
        cooldown = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ItemsFilter", filters.serializeNBT(registries));
        tag.putInt("Delay", delay);
        tag.putByte("StackLimit", (byte) inventory.getStackLimit());
        int mask = filterMask;
        if (filtered) {
            mask |= 0x80;
        }
        mask |= outputSideIndex << 8;
        mask |= redstoneMode << 12;
        tag.putShort("SettingsMask", (short) mask);
        int sides = 0;
        for (Direction side : enabledSides) {
            sides |= 1 << side.get3DDataValue();
        }
        tag.putShort("Sides", (short) sides);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ItemsFilter")) {
            filters.deserializeNBT(registries, tag.getCompound("ItemsFilter"));
        }
        delay = tag.getInt("Delay");
        inventory.setStackLimit(Math.max(1, tag.getByte("StackLimit")));
        int mask = tag.getShort("SettingsMask");
        filterMask = mask & 0x0F;
        outputSideIndex = (mask >>> 8) & 0x7;
        redstoneMode = (mask >>> 12) & 0x3;
        enabledSides.clear();
        int sides = tag.getShort("Sides");
        for (Direction side : Direction.values()) {
            if ((sides & (1 << side.get3DDataValue())) != 0) {
                enabledSides.add(side);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new InserterMenu(id, playerInventory, this);
    }
}
