package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.block.HorizontalMachineBlock;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.menu.DrawbridgeMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class DrawbridgeBlockEntity extends InventoryBlockEntity implements GuiActionHandler,
        HorizontalMachineBlock.ServerTickingBlockEntity, HorizontalMachineBlock.NeighborAwareBlockEntity, CamouflageHolder {
    public static final int MAX_LENGTH_NORMAL = 64;
    public static final int MAX_LENGTH_ADVANCED = 32;
    private final boolean advanced;
    private boolean redstoneState;
    private int state;
    private int redstoneMode;
    private int position;
    private int delay = 4;
    private int maxLength = 1;
    private int numPlaced;
    private int failedPlacements;
    private int numTaken;
    private int cooldown;
    private final BlockState[] placed = new BlockState[MAX_LENGTH_NORMAL];
    private final CompoundTag[] takenNbt = new CompoundTag[MAX_LENGTH_NORMAL];
    private final BlockState[] takenState = new BlockState[MAX_LENGTH_NORMAL];
    private final CamouflageStorage camouflage = new CamouflageStorage();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> delay;
                case 1 -> maxLength;
                case 2 -> redstoneMode;
                case 3 -> advanced ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> delay = value;
                case 1 -> maxLength = value;
                case 2 -> redstoneMode = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public DrawbridgeBlockEntity(BlockPos pos, BlockState state, boolean advanced) {
        super(ModBlockEntities.DRAWBRIDGE.get(), pos, state,
                new LimitedItemStackHandler(MAX_LENGTH_ADVANCED, 1).validator((slot, stack) -> stack.getItem() instanceof BlockItem));
        this.advanced = advanced;
        inventory.onChanged(this::setChanged);
        inventory.setVisibleSlots(advanced ? 1 : 1);
        inventory.setStackLimit(advanced ? 1 : 1);
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public int getSlotCount() {
        return advanced ? maxLength : 1;
    }

    public ContainerData getData() {
        return data;
    }

    public Direction getFacing() {
        if (getBlockState().hasProperty(BlockStateProperties.FACING)) {
            return getBlockState().getValue(BlockStateProperties.FACING);
        }
        if (getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public void onNeighborChanged() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean powered = false;
        Direction facing = getFacing();
        for (Direction dir : Direction.values()) {
            if (dir != facing && level.hasSignal(worldPosition.relative(dir), dir)) {
                powered = true;
                break;
            }
        }
        if (powered == redstoneState) {
            return;
        }
        switch (redstoneMode) {
            case 1 -> state = powered ? 2 : 1;
            case 2 -> {
                if (powered) {
                    if (state == 1) {
                        state = 2;
                    } else if (state == 2) {
                        state = 1;
                    } else if (position >= maxLength - 1 && (maxLength > 1 || numPlaced > 0 || failedPlacements > 0)) {
                        state = 2;
                    } else {
                        state = 1;
                    }
                }
            }
            default -> state = powered ? 1 : 2;
        }
        redstoneState = powered;
        cooldown = delay;
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide || state == 0) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        inventory.setVisibleSlots(getSlotCount());
        if (state == 1) {
            while (position < maxLength) {
                boolean result = extendOne(position);
                position++;
                if (result) {
                    break;
                }
            }
            if (position >= maxLength) {
                position = maxLength - 1;
                numTaken = 0;
                state = 0;
            } else {
                cooldown = delay;
            }
        } else if (state == 2) {
            boolean takeNonPlaced = numPlaced == 0 && failedPlacements == 0;
            while (position >= 0) {
                boolean result = retractOne(position, takeNonPlaced);
                position--;
                if (result) {
                    break;
                }
            }
            if (position < 0) {
                position = 0;
                numPlaced = 0;
                failedPlacements = 0;
                state = 0;
            } else {
                cooldown = delay;
            }
        }
        setChanged();
    }

    private boolean extendOne(int offset) {
        int invSlot = advanced ? offset : 0;
        BlockPos target = worldPosition.relative(getFacing(), offset + 1);
        ItemStack stack = inventory.getStackInSlot(invSlot);
        BlockState toPlace = takenState[offset];
        if (toPlace == null && stack.getItem() instanceof BlockItem blockItem && (advanced || numTaken == 0)) {
            toPlace = blockItem.getBlock().defaultBlockState();
        }
        if (toPlace == null || stack.isEmpty() || !level.getBlockState(target).canBeReplaced()) {
            if (!stack.isEmpty()) {
                failedPlacements++;
            }
            return false;
        }
        if (level.setBlock(target, toPlace, 3)) {
            inventory.extractItem(invSlot, 1, false);
            placed[offset] = toPlace;
            numPlaced++;
            if (takenNbt[offset] != null && toPlace.hasBlockEntity()) {
                BlockEntity te = level.getBlockEntity(target);
                if (te != null) {
                    te.loadWithComponents(takenNbt[offset], level.registryAccess());
                }
            }
            level.playSound(null, target, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.8F);
            return true;
        }
        failedPlacements++;
        return false;
    }

    private boolean retractOne(int offset, boolean takeNonPlaced) {
        BlockPos target = worldPosition.relative(getFacing(), offset + 1);
        BlockState current = level.getBlockState(target);
        if (current.isAir() || current.getDestroySpeed(level, target) < 0) {
            return false;
        }
        if (!takeNonPlaced && placed[offset] != null && current != placed[offset]) {
            return false;
        }
        ItemStack stack = current.getBlock().getCloneItemStack(level, target, current);
        if (stack.isEmpty() && current.getBlock().asItem() != net.minecraft.world.item.Items.AIR) {
            stack = new ItemStack(current.getBlock());
        }
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag teTag = null;
        if (current.hasBlockEntity()) {
            BlockEntity te = level.getBlockEntity(target);
            if (te != null) {
                teTag = te.saveWithFullMetadata(level.registryAccess());
                te.saveToItem(stack, level.registryAccess());
            }
        }
        int invSlot = advanced ? offset : 0;
        if (!inventory.insertItem(invSlot, stack, false).isEmpty()) {
            return false;
        }
        takenState[offset] = current;
        takenNbt[offset] = teTag;
        numTaken++;
        level.playSound(null, target, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 0.7F);
        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
        placed[offset] = null;
        return true;
    }

    public void applyPlacementProperties(int delay, int length, int redstoneMode) {
        this.delay = Mth.clamp(delay, 1, 255);
        this.maxLength = Mth.clamp(length, 1, advanced ? MAX_LENGTH_ADVANCED : MAX_LENGTH_NORMAL);
        this.redstoneMode = Mth.clamp(redstoneMode, 0, 2);
        inventory.setVisibleSlots(getSlotCount());
        if (!advanced) {
            inventory.setStackLimit(maxLength);
        }
        setChanged();
    }

    @Override
    public void performGuiAction(Player player, int action, int element) {
        switch (action) {
            case 0 -> {
                if (state == 0) {
                    for (int i = 0; i < maxLength; i++) {
                        retractOne(i, true);
                    }
                }
            }
            case 1 -> delay = Mth.clamp(delay + element, 1, 255);
            case 2 -> {
                if (state == 0) {
                    int old = maxLength;
                    maxLength = Mth.clamp(maxLength + element, 1, advanced ? MAX_LENGTH_ADVANCED : MAX_LENGTH_NORMAL);
                    inventory.setVisibleSlots(getSlotCount());
                    if (!advanced) {
                        inventory.setStackLimit(maxLength);
                    }
                    if (position >= old) {
                        position = maxLength - 1;
                    }
                }
            }
            case 3 -> {
                if (state == 0) {
                    redstoneMode = Math.floorMod(redstoneMode + (element > 0 ? 1 : -1), 3);
                }
            }
            default -> {
            }
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Advanced", advanced);
        tag.putBoolean("Powered", redstoneState);
        tag.putByte("Position", (byte) position);
        tag.putByte("Delay", (byte) delay);
        tag.putByte("Length", (byte) maxLength);
        tag.putByte("State", (byte) state);
        tag.putByte("RedstoneMode", (byte) redstoneMode);
        tag.putByte("NumPlaced", (byte) numPlaced);
        tag.putByte("Failed", (byte) failedPlacements);
        tag.putByte("NumTaken", (byte) numTaken);
        ListTag taken = new ListTag();
        for (int i = 0; i < maxLength; i++) {
            if (takenState[i] != null) {
                CompoundTag entry = new CompoundTag();
                entry.put("state", NbtUtils.writeBlockState(takenState[i]));
                if (takenNbt[i] != null) {
                    entry.put("nbt", takenNbt[i]);
                }
                entry.putByte("pos", (byte) i);
                taken.add(entry);
            }
        }
        tag.put("Blocks", taken);
        ListTag placedList = new ListTag();
        for (int i = 0; i < maxLength; i++) {
            if (placed[i] != null) {
                CompoundTag entry = new CompoundTag();
                entry.put("state", NbtUtils.writeBlockState(placed[i]));
                entry.putByte("pos", (byte) i);
                placedList.add(entry);
            }
        }
        tag.put("Placed", placedList);
        camouflage.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        redstoneState = tag.getBoolean("Powered");
        position = tag.getByte("Position");
        delay = Math.max(1, tag.getByte("Delay") & 0xFF);
        maxLength = Math.max(1, tag.getByte("Length"));
        state = tag.getByte("State");
        redstoneMode = tag.getByte("RedstoneMode");
        numPlaced = tag.getByte("NumPlaced");
        failedPlacements = tag.getByte("Failed");
        numTaken = tag.getByte("NumTaken");
        inventory.setVisibleSlots(getSlotCount());
        if (tag.contains("Blocks", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int pos = entry.getByte("pos");
                if (pos >= 0 && pos < takenState.length) {
                    takenState[pos] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry.getCompound("state"));
                    if (entry.contains("nbt")) {
                        takenNbt[pos] = entry.getCompound("nbt");
                    }
                }
            }
        }
        if (tag.contains("Placed", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Placed", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int pos = entry.getByte("pos");
                if (pos >= 0 && pos < placed.length) {
                    placed[pos] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry.getCompound("state"));
                }
            }
        }
        camouflage.load(tag, registries);
    }

    @Override
    public BlockState getCamouflage() {
        return camouflage.get();
    }

    @Override
    public void setCamouflage(@Nullable BlockState camouflage) {
        this.camouflage.set(camouflage);
        CamouflageStorage.sync(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        camouflage.save(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        camouflage.load(tag, registries);
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new DrawbridgeMenu(id, playerInventory, this);
    }
}
