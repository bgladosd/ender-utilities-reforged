package dev.celli.enderutilities.blockentity;

import java.util.List;

import dev.celli.enderutilities.block.ElevatorBlock;
import dev.celli.enderutilities.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ElevatorBlockEntity extends BlockEntity implements CamouflageHolder {
    private boolean redstoneState;
    private final CamouflageStorage camouflage = new CamouflageStorage();

    public ElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELEVATOR.get(), pos, state);
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

    public void neighborChanged() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean redstone = level.hasNeighborSignal(worldPosition);
        if (redstone != redstoneState && redstone) {
            activateByRedstone(level.getBestNeighborSignal(worldPosition) < 8);
        }
        redstoneState = redstone;
    }

    public void activateByRedstone(boolean goingUp) {
        if (level == null) {
            return;
        }
        VoxelShape shape = getBlockState().getShape(level, worldPosition);
        AABB bounds = shape.isEmpty() ? new AABB(worldPosition) : shape.bounds().move(worldPosition);
        AABB search = new AABB(bounds.minX, bounds.maxY, bounds.minZ, bounds.maxX, bounds.maxY + 1.0, bounds.maxZ);
        for (Entity entity : level.getEntities((Entity) null, search, entity -> true)) {
            activateForEntity(entity, goingUp);
        }
    }

    public void activateForEntity(Entity entity, boolean goingUp) {
        Vec3 delta = getMoveVector(goingUp);
        if (delta != null && level != null) {
            entity.teleportTo(entity.getX() + delta.x, entity.getY() + delta.y, entity.getZ() + delta.z);
            level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 0.3F, 1.8F);
        }
    }

    private Vec3 getMoveVector(boolean goingUp) {
        if (level == null || !(getBlockState().getBlock() instanceof ElevatorBlock)) {
            return null;
        }
        DyeColor color = getBlockState().getValue(ElevatorBlock.COLOR);
        BlockPos origin = goingUp ? worldPosition.above() : worldPosition.below();
        BlockPos found = findNearest(level, origin, color, goingUp);
        if (found == null) {
            return null;
        }
        VoxelShape here = getBlockState().getShape(level, worldPosition);
        VoxelShape there = level.getBlockState(found).getShape(level, found);
        double hereMax = here.isEmpty() ? 1.0 : here.bounds().maxY;
        double thereMax = there.isEmpty() ? 1.0 : there.bounds().maxY;
        double yDiff = (found.getY() + thereMax) - (worldPosition.getY() + hereMax);
        return new Vec3(found.getX() - worldPosition.getX(), yDiff, found.getZ() - worldPosition.getZ());
    }

    private static BlockPos findNearest(Level level, BlockPos origin, DyeColor color, boolean goingUp) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int minY = goingUp ? origin.getY() : Math.max(level.getMinBuildHeight(), origin.getY() - 256);
        int maxY = goingUp ? Math.min(level.getMaxBuildHeight() - 1, origin.getY() + 256) : origin.getY();
        for (int y = minY; y <= maxY; y++) {
            for (int dx = -64; dx <= 64; dx++) {
                for (int dz = -64; dz <= 64; dz++) {
                    BlockPos pos = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof ElevatorBlock && state.getValue(ElevatorBlock.COLOR) == color) {
                        double dist = pos.distSqr(origin);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = pos.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        redstoneState = tag.getBoolean("Redstone");
        camouflage.load(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Redstone", redstoneState);
        camouflage.save(tag, registries);
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

    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
