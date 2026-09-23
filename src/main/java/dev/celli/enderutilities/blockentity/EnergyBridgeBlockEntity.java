package dev.celli.enderutilities.blockentity;

import java.util.List;

import dev.celli.enderutilities.block.EnergyBridgeBlock;
import dev.celli.enderutilities.registry.ModBlockEntities;
import dev.celli.enderutilities.registry.ModBlocks;
import dev.celli.enderutilities.util.EnergyBridgeTracker;
import dev.celli.enderutilities.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class EnergyBridgeBlockEntity extends BlockEntity {
    private static final StructurePart[] STRUCTURE_TRANSMITTER = new StructurePart[] {
            new StructurePart(BlockPos.ZERO.above(3), Type.TRANSMITTER, Direction.NORTH),
            new StructurePart(BlockPos.ZERO.north(3), Type.RESONATOR, Direction.SOUTH),
            new StructurePart(BlockPos.ZERO.south(3), Type.RESONATOR, Direction.NORTH),
            new StructurePart(BlockPos.ZERO.east(3), Type.RESONATOR, Direction.WEST),
            new StructurePart(BlockPos.ZERO.west(3), Type.RESONATOR, Direction.EAST)
    };
    private static final StructurePart[] STRUCTURE_RECEIVER = new StructurePart[] {
            new StructurePart(BlockPos.ZERO, Type.RECEIVER, Direction.NORTH),
            new StructurePart(BlockPos.ZERO.north(3), Type.RESONATOR, Direction.SOUTH),
            new StructurePart(BlockPos.ZERO.south(3), Type.RESONATOR, Direction.NORTH),
            new StructurePart(BlockPos.ZERO.east(3), Type.RESONATOR, Direction.WEST),
            new StructurePart(BlockPos.ZERO.west(3), Type.RESONATOR, Direction.EAST)
    };

    private boolean isActive;
    private boolean isPowered;
    private int timer;
    private Type type = Type.RESONATOR;
    public int beamYMin;
    public int beamYMax;

    public EnergyBridgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_BRIDGE.get(), pos, state);
        this.type = typeFromState(state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (getBridgeType() != Type.RESONATOR && ++timer >= 100) {
            tryAssembleMultiBlock();
            timer = 0;
        }
    }

    public Type getBridgeType() {
        return typeFromState(getBlockState());
    }

    public Direction getFacing() {
        if (getBridgeType() == Type.RESONATOR) {
            return getBlockState().getValue(EnergyBridgeBlock.FACING);
        }
        return Direction.NORTH;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public boolean getIsPowered() {
        return isPowered;
    }

    public void tryAssembleMultiBlock() {
        Level world = this.level;
        if (world == null || world.isClientSide) {
            return;
        }
        Type masterType = getMasterType();
        BlockPos center = getCenterPos();
        boolean isValid = isStructureValid(world, center, masterType);
        if (isValid) {
            if (!isActive) {
                activateMultiBlock(world, center, masterType);
                EnergyBridgeTracker.addBridgeLocation(world, getMasterPos(center));
            }
            updatePoweredState(world, center, masterType);
        } else if (isActive) {
            disableMultiBlock(world, center, masterType);
        }
    }

    public void disassembleMultiblock() {
        if (level == null || level.isClientSide) {
            return;
        }
        disableMultiBlock(level, getCenterPos(), getMasterType());
    }

    private BlockPos getCenterPos() {
        return switch (getBridgeType()) {
            case TRANSMITTER -> worldPosition.below(3);
            case RECEIVER -> worldPosition;
            case RESONATOR -> worldPosition.relative(getFacing(), 3);
        };
    }

    private Type getMasterType() {
        return WorldUtils.isEndDimension(level) ? Type.TRANSMITTER : Type.RECEIVER;
    }

    private BlockPos getMasterPos(BlockPos center) {
        return WorldUtils.isEndDimension(level) ? center.above(3) : center;
    }

    private StructurePart[] getStructure(Type masterType) {
        return masterType == Type.TRANSMITTER ? STRUCTURE_TRANSMITTER : STRUCTURE_RECEIVER;
    }

    private boolean isStructureValid(Level world, BlockPos center, Type masterType) {
        for (StructurePart part : getStructure(masterType)) {
            BlockPos posTmp = center.offset(part.offset());
            BlockState state = world.getBlockState(posTmp);
            if (!matchesRequired(state, part)) {
                return false;
            }
        }

        boolean isValid;
        if (masterType == Type.TRANSMITTER) {
            double xd = center.getX();
            double yd = center.getY();
            double zd = center.getZ();
            double d = 1.0d;
            List<EndCrystal> list = world.getEntitiesOfClass(EndCrystal.class,
                    new AABB(xd - d, yd - d, zd - d, xd + d, yd + d, zd + d));
            isValid = list.size() >= 1;
        } else {
            isValid = true;
        }

        return isValid && !isObstructed(world, center, masterType);
    }

    private static boolean matchesRequired(BlockState state, StructurePart part) {
        if (!(state.getBlock() instanceof EnergyBridgeBlock bridge) || bridge.getBridgeType() != part.type()) {
            return false;
        }
        return state.getValue(EnergyBridgeBlock.FACING) == part.facing();
    }

    private boolean isObstructedQuadrant(Level world, BlockPos basePos, Direction facing, BlockPos... positions) {
        Direction dirNext = facing.getClockWise();
        for (BlockPos pos : positions) {
            int x = pos.getX() * facing.getStepX() + pos.getZ() * facing.getStepZ();
            int y = pos.getY();
            int z = pos.getX() * dirNext.getStepX() + pos.getZ() * dirNext.getStepZ();
            if (!world.isEmptyBlock(basePos.offset(x, y, z))) {
                return true;
            }
        }
        return false;
    }

    private boolean isObstructed(Level world, BlockPos center, Type masterType) {
        BlockPos posMaster = getMasterPos(center);
        BlockPos[] positionsToCheck = new BlockPos[] {
                new BlockPos(1, 0, 0),
                new BlockPos(2, 0, 0),
                new BlockPos(1, 0, 3),
                new BlockPos(1, 0, 2),
                new BlockPos(2, 0, 2),
                new BlockPos(2, 0, 1),
                new BlockPos(3, 0, 1)
        };

        if (isObstructedQuadrant(world, center, Direction.EAST, positionsToCheck)
                || isObstructedQuadrant(world, center, Direction.SOUTH, positionsToCheck)
                || isObstructedQuadrant(world, center, Direction.WEST, positionsToCheck)
                || isObstructedQuadrant(world, center, Direction.NORTH, positionsToCheck)) {
            return true;
        }

        if (masterType == Type.TRANSMITTER) {
            if (isObstructedQuadrant(world, posMaster, Direction.EAST, new BlockPos(0, -1, 0), new BlockPos(0, -2, 0))) {
                return true;
            }
        } else if (isVerticalBeamObstructed(world, posMaster.getX(), posMaster.getZ(), posMaster.getY() - 1, world.getMinBuildHeight())) {
            return true;
        }

        int top = world.getMaxBuildHeight() - 1;
        return isVerticalBeamObstructed(world, posMaster.getX(), posMaster.getZ(), posMaster.getY() + 1, top);
    }

    private boolean isVerticalBeamObstructed(Level world, int x, int z, int yStart, int yEnd) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, yStart, z);
        int increment = yStart < yEnd ? 1 : -1;
        yEnd += increment;
        while (pos.getY() != yEnd) {
            if (!world.isEmptyBlock(pos)) {
                BlockState state = world.getBlockState(pos);
                if (state.getLightBlock(world, pos) > 3) {
                    return !WorldUtils.isBlockValidBedrockForEnergyBridge(state);
                }
            }
            pos.setY(pos.getY() + increment);
        }
        return false;
    }

    private void activateMultiBlock(Level world, BlockPos center, Type masterType) {
        setActiveStateForStructure(world, center, masterType, true);
    }

    private void disableMultiBlock(Level world, BlockPos center, Type masterType) {
        setActiveStateForStructure(world, center, masterType, false);
        EnergyBridgeTracker.removeBridgeLocation(world, getMasterPos(center));
    }

    private void setActiveStateForStructure(Level world, BlockPos center, Type masterType, boolean active) {
        for (StructurePart part : getStructure(masterType)) {
            setActiveState(world, center.offset(part.offset()), active);
        }
    }

    private void setActiveState(Level world, BlockPos pos, boolean state) {
        if (world.getBlockEntity(pos) instanceof EnergyBridgeBlockEntity bridge) {
            bridge.setActiveState(state);
        }
    }

    private void setActiveState(boolean isActive) {
        this.isActive = isActive;
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(EnergyBridgeBlock.ACTIVE) && state.getValue(EnergyBridgeBlock.ACTIVE) != isActive) {
                level.setBlock(worldPosition, state.setValue(EnergyBridgeBlock.ACTIVE, isActive), Block.UPDATE_ALL);
            } else {
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            }
            setChanged();
        }
    }

    private void updatePoweredState(Level world, BlockPos center, Type masterType) {
        boolean powered = WorldUtils.isEndDimension(world)
                ? EnergyBridgeTracker.dimensionHasEnergyBridge(world)
                : EnergyBridgeTracker.endHasEnergyBridges(world);
        for (StructurePart part : getStructure(masterType)) {
            updatePoweredState(world, center.offset(part.offset()), powered);
        }
    }

    private void updatePoweredState(Level world, BlockPos pos, boolean value) {
        if (world.getBlockEntity(pos) instanceof EnergyBridgeBlockEntity bridge) {
            bridge.setPoweredState(value);
        }
    }

    private void setPoweredState(boolean value) {
        if (isPowered == value && level != null && !level.isClientSide) {
            return;
        }
        isPowered = value;
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            setChanged();
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("f")) {
            byte f = tag.getByte("f");
            isActive = (f & 0x80) != 0;
            isPowered = (f & 0x40) != 0;
        }
        super.handleUpdateTag(tag, registries);
        if (level != null && level.isClientSide) {
            getBeamEndPoints();
        }
    }

    public void getBeamEndPoints() {
        if (level == null) {
            return;
        }
        int posX = worldPosition.getX();
        int posY = worldPosition.getY();
        int posZ = worldPosition.getZ();
        int top = level.getMaxBuildHeight() - 1;
        Type current = getBridgeType();
        if (current == Type.TRANSMITTER) {
            beamYMin = posY - 2;
        } else if (current == Type.RECEIVER) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(posX, posY, posZ);
            while (pos.getY() >= level.getMinBuildHeight()) {
                BlockState state = level.getBlockState(pos);
                if (WorldUtils.isBlockValidBedrockForEnergyBridge(state)) {
                    break;
                }
                pos.setY(pos.getY() - 1);
            }
            beamYMin = pos.getY() + 1;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(posX, posY, posZ);
        int y = 512;
        while (pos.getY() <= top) {
            BlockState state = level.getBlockState(pos);
            if (WorldUtils.isBlockValidBedrockForEnergyBridge(state)) {
                y = pos.getY();
                break;
            }
            pos.setY(pos.getY() + 1);
        }
        beamYMax = y;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("Flags", (byte) ((isActive ? 0x80 : 0x00) | (isPowered ? 0x40 : 0x00) | getBridgeType().getMeta()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        byte f = tag.getByte("Flags");
        isActive = (f & 0x80) != 0;
        isPowered = (f & 0x40) != 0;
        type = Type.fromMeta(f & 0x03);
        if (level != null && level.isClientSide) {
            getBeamEndPoints();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putByte("f", (byte) ((isActive ? 0x80 : 0x00) | (isPowered ? 0x40 : 0x00) | getBridgeType().getMeta()));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static Type typeFromState(BlockState state) {
        if (state.is(ModBlocks.ENERGY_BRIDGE_TRANSMITTER.get())) {
            return Type.TRANSMITTER;
        }
        if (state.is(ModBlocks.ENERGY_BRIDGE_RECEIVER.get())) {
            return Type.RECEIVER;
        }
        return Type.RESONATOR;
    }

    public enum Type {
        RESONATOR(0),
        RECEIVER(1),
        TRANSMITTER(2);

        private final int meta;

        Type(int meta) {
            this.meta = meta;
        }

        public int getMeta() {
            return meta;
        }

        public static Type fromMeta(int meta) {
            Type[] values = values();
            return meta < values.length ? values[meta] : RESONATOR;
        }
    }

    private record StructurePart(BlockPos offset, Type type, Direction facing) {
    }
}
