package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PortalBlockEntity extends BlockEntity {
    public static final int DEFAULT_COLOR = 0xA010E0;

    private TargetLocation destination;
    private OwnerInfo owner;
    private int color = DEFAULT_COLOR;
    private boolean targetIsPortal;

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTAL.get(), pos, state);
    }

    public int getColor() {
        return color;
    }

    public TargetLocation getDestination() {
        return destination;
    }

    public OwnerInfo getOwner() {
        return owner;
    }

    public boolean targetIsPortal() {
        return targetIsPortal;
    }

    public void setPortalData(PortalData data) {
        if (data == null) {
            this.destination = null;
            this.owner = null;
            this.color = DEFAULT_COLOR;
            this.targetIsPortal = false;
        } else {
            this.destination = data.destination();
            this.owner = data.owner();
            this.color = data.color();
            this.targetIsPortal = data.targetIsPortal();
        }
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Color", color);
        if (targetIsPortal) {
            tag.putBoolean("Paired", true);
        }
        if (destination != null) {
            TargetLocation.CODEC.encodeStart(NbtOps.INSTANCE, destination).result().ifPresent(t -> tag.put("Dest", t));
        }
        if (owner != null) {
            OwnerInfo.CODEC.encodeStart(NbtOps.INSTANCE, owner).result().ifPresent(t -> tag.put("Owner", t));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        color = tag.getInt("Color");
        targetIsPortal = tag.getBoolean("Paired");
        destination = tag.contains("Dest")
                ? TargetLocation.CODEC.parse(NbtOps.INSTANCE, tag.get("Dest")).result().orElse(null)
                : null;
        owner = tag.contains("Owner")
                ? OwnerInfo.CODEC.parse(NbtOps.INSTANCE, tag.get("Owner")).result().orElse(null)
                : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Color", color);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Color")) {
            color = tag.getInt("Color");
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public record PortalData(TargetLocation destination, OwnerInfo owner, int color, boolean targetIsPortal) {
    }
}
