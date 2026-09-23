package dev.celli.enderutilities.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.block.PortalBlock;
import dev.celli.enderutilities.blockentity.PortalBlockEntity;
import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PortalFormer {
    private final Level world;
    private PortalBlockEntity.PortalData portalData;
    private final List<BlockPos> visited = new ArrayList<>();
    private final List<BlockPos> branches = new ArrayList<>();
    private final List<BlockPos> corners = new ArrayList<>();
    private final Block blockFrame;
    private final Block blockPortal;
    private final BlockPos startPos;
    private BlockPos lastPos;
    private Direction nextSide;
    private Direction.Axis portalAxis;
    private int frameCheckLimit;
    private int frameLoopCheckLimit;
    private int portalAreaCheckLimit;
    private int portalsFound;
    private boolean analyzed;
    private boolean validated;
    private boolean formed;

    public PortalFormer(Level world, BlockPos startPos, Block frameBlock, Block portalBlock) {
        this.world = world;
        this.blockFrame = frameBlock;
        this.blockPortal = portalBlock;
        this.startPos = startPos;
        this.lastPos = startPos;
        this.frameCheckLimit = ModCommonConfig.PORTAL_FRAME_CHECK_LIMIT.get();
        this.frameLoopCheckLimit = ModCommonConfig.PORTAL_LOOP_CHECK_LIMIT.get();
        this.portalAreaCheckLimit = ModCommonConfig.PORTAL_AREA_CHECK_LIMIT.get();
    }

    public void setPortalData(PortalBlockEntity.PortalData data) {
        this.portalData = data;
    }

    public boolean getPortalState() {
        return this.portalsFound > 0;
    }

    public boolean togglePortalState(boolean recreate) {
        this.analyzePortal();
        if (this.getPortalState()) {
            this.destroyPortals();
            if (!recreate) {
                return true;
            }
        }
        this.validatePortalAreas();
        return this.formPortals();
    }

    public void analyzePortal() {
        if (this.analyzed) {
            return;
        }
        this.visited.clear();
        this.branches.clear();
        this.corners.clear();
        this.portalsFound = 0;
        if (!this.world.getBlockState(this.startPos).is(this.blockFrame)) {
            this.analyzed = true;
            return;
        }
        int counter = 0;
        int branchIndex = 0;
        BlockPos pos = this.startPos;
        while (counter < this.frameCheckLimit) {
            Direction side = this.checkFramePositionIgnoringSide(pos);
            counter++;
            if (side == null) {
                if (branchIndex < this.branches.size()) {
                    pos = this.branches.get(branchIndex);
                    branchIndex++;
                } else {
                    break;
                }
            } else {
                pos = pos.relative(side);
            }
        }
        this.analyzed = true;
    }

    private void validatePortalAreas() {
        if (this.validated) {
            return;
        }
        this.visited.clear();
        Iterator<BlockPos> iter = this.corners.iterator();
        while (iter.hasNext()) {
            BlockPos pos = iter.next();
            if (this.checkForCorner(pos, true)) {
                Direction.Axis axis = this.getPortalAxisFromCorner(pos);
                if (axis == null) {
                    EnderUtilities.LOGGER.warn("null axis in PortalFormer#validateCorners()");
                    break;
                }
                Direction side = this.getSideWithFrame(pos, axis);
                if (side == null) {
                    EnderUtilities.LOGGER.warn("Didn't find an adjacent portal frame in PortalFormer#validateCorners()");
                    break;
                }
                if (!this.walkFrameLoop(pos, axis, side, this.frameLoopCheckLimit)) {
                    iter.remove();
                }
            } else {
                iter.remove();
            }
        }
        this.validated = true;
    }

    private boolean formPortals() {
        if (this.formed) {
            return false;
        }
        boolean valid;
        boolean success = false;
        BlockPos posTmp;
        for (BlockPos pos : this.corners) {
            this.branches.clear();
            this.visited.clear();
            this.portalAxis = this.getPortalAxisFromCorner(pos);
            if (this.portalAxis == null) {
                continue;
            }
            int counter = 0;
            int branchIndex = 0;
            posTmp = pos;
            valid = true;
            while (counter < this.portalAreaCheckLimit) {
                valid = this.checkForValidPortalPosition(posTmp);
                counter++;
                if (!valid) {
                    break;
                }
                if (this.nextSide == null) {
                    if (branchIndex < this.branches.size()) {
                        posTmp = this.branches.get(branchIndex);
                        branchIndex++;
                    } else {
                        break;
                    }
                } else {
                    posTmp = posTmp.relative(this.nextSide);
                }
            }
            if (valid && counter < this.portalAreaCheckLimit) {
                Direction facing = this.portalAxis == Direction.Axis.X ? Direction.EAST
                        : this.portalAxis == Direction.Axis.Z ? Direction.NORTH : Direction.UP;
                BlockState state = ModBlocks.PORTAL.get().defaultBlockState().setValue(PortalBlock.FACING, facing);
                for (BlockPos posPortal : this.visited) {
                    this.world.setBlock(posPortal, state, Block.UPDATE_CLIENTS);
                    if (this.world.getBlockEntity(posPortal) instanceof PortalBlockEntity te) {
                        te.setPortalData(this.portalData);
                    }
                }
                success = true;
            }
        }
        this.formed = true;
        return success;
    }

    private boolean destroyPortals() {
        boolean success = false;
        for (BlockPos pos : this.corners) {
            if (this.world.getBlockState(pos).is(this.blockPortal)) {
                this.world.removeBlock(pos, false);
                success = true;
            }
        }
        return success;
    }

    private boolean walkFrameLoop(BlockPos pos, Direction.Axis axis, Direction frameSide, int distanceLimit) {
        int counter = 0;
        int turns = 0;
        int tries;
        BlockPos start = pos;
        BlockPos posLast = start;
        Direction firstTrySide = frameSide;
        Direction moveDirection;
        while (counter < distanceLimit) {
            moveDirection = firstTrySide;
            for (tries = 0; tries < 4; tries++) {
                pos = posLast.relative(moveDirection);
                BlockState state = this.world.getBlockState(pos);
                Block block = state.getBlock();
                if (state.isAir()) {
                    posLast = pos;
                    if (tries > 1) {
                        turns++;
                    } else if (tries == 0) {
                        turns--;
                    }
                    firstTrySide = moveDirection.getClockWise(axis).getOpposite();
                    break;
                } else if (block == this.blockFrame) {
                    moveDirection = moveDirection.getClockWise(axis);
                } else {
                    return false;
                }
            }
            if ((tries == 4 && counter == 0) || pos.equals(start)) {
                return turns >= 0;
            }
            counter++;
        }
        return false;
    }

    @Nullable
    private Direction checkFramePositionIgnoringSide(BlockPos posIn) {
        if (this.visited.contains(posIn)) {
            return null;
        }
        Direction continueTo = null;
        int frames = 0;
        for (Direction side : Direction.values()) {
            BlockPos pos = posIn.relative(side);
            if (pos.equals(this.lastPos)) {
                continue;
            }
            BlockState state = this.world.getBlockState(pos);
            Block block = state.getBlock();
            if (state.isAir() || block == this.blockPortal) {
                this.checkForCorner(pos, false);
                if (block == this.blockPortal) {
                    this.portalsFound++;
                }
            } else if (block == this.blockFrame) {
                if (!this.visited.contains(pos)) {
                    if (frames == 0) {
                        continueTo = side;
                    } else {
                        this.branches.add(pos);
                    }
                }
                frames++;
            }
        }
        this.visited.add(posIn);
        this.lastPos = posIn;
        return continueTo;
    }

    private boolean checkForCorner(BlockPos posIn, boolean checkIsAir) {
        if (checkIsAir && !this.world.isEmptyBlock(posIn)) {
            return false;
        }
        if (this.corners.contains(posIn)) {
            return true;
        }
        int adjacents = 0;
        Direction[] frames = new Direction[6];
        for (Direction side : Direction.values()) {
            Block block = this.world.getBlockState(posIn.relative(side)).getBlock();
            if (block == this.blockFrame) {
                frames[adjacents] = side;
                adjacents++;
            } else if (block == this.blockPortal) {
                this.portalsFound++;
            }
        }
        if (adjacents >= 3 || (adjacents == 2 && frames[0] != frames[1].getOpposite())) {
            this.corners.add(posIn);
            return true;
        }
        return false;
    }

    private boolean checkForValidPortalPosition(BlockPos posIn) {
        if (this.visited.contains(posIn)) {
            return true;
        }
        Direction continueTo = null;
        int adjacent = 0;
        Direction[] sides = PositionUtils.getSidesForAxis(this.portalAxis);
        for (Direction side : sides) {
            BlockPos pos = posIn.relative(side);
            if (pos.equals(this.lastPos)) {
                continue;
            }
            BlockState state = this.world.getBlockState(pos);
            Block block = state.getBlock();
            if (state.isAir()) {
                if (!this.visited.contains(pos)) {
                    if (adjacent == 0) {
                        continueTo = side;
                    } else {
                        this.branches.add(pos);
                    }
                    adjacent++;
                }
            } else if (block != this.blockFrame) {
                return false;
            }
        }
        this.visited.add(posIn);
        this.lastPos = posIn;
        this.nextSide = continueTo;
        return true;
    }

    @Nullable
    private Direction.Axis getPortalAxisFromCorner(BlockPos posIn) {
        int numAdjacents = 0;
        Direction[] adjacents = new Direction[6];
        for (Direction side : Direction.values()) {
            if (numAdjacents > 0 && adjacents[numAdjacents - 1] == side.getOpposite()) {
                continue;
            }
            if (this.world.getBlockState(posIn.relative(side)).is(this.blockFrame)) {
                adjacents[numAdjacents] = side;
                numAdjacents++;
            }
        }
        if (numAdjacents >= 3 || (numAdjacents == 2 && adjacents[0] != adjacents[1].getOpposite())) {
            if (adjacents[0] == Direction.DOWN || adjacents[0] == Direction.UP) {
                if (adjacents[1] == Direction.NORTH || adjacents[1] == Direction.SOUTH) {
                    return Direction.Axis.X;
                }
                return Direction.Axis.Z;
            }
            return Direction.Axis.Y;
        }
        return null;
    }

    @Nullable
    private Direction getSideWithFrame(BlockPos pos, Direction.Axis axis) {
        for (Direction side : PositionUtils.getSidesForAxis(axis)) {
            if (this.world.getBlockState(pos.relative(side)).is(this.blockFrame)) {
                return side;
            }
        }
        return null;
    }
}
