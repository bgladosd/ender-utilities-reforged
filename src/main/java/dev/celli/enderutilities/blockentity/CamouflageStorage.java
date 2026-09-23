package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CamouflageStorage {
    private BlockState camouflage;

    @Nullable
    public BlockState get() {
        return camouflage;
    }

    public void set(@Nullable BlockState camouflage) {
        this.camouflage = camouflage != null && camouflage.isAir() ? null : camouflage;
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (camouflage != null) {
            tag.put("Camouflage", NbtUtils.writeBlockState(camouflage));
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Camouflage")) {
            camouflage = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("Camouflage"));
        } else {
            camouflage = null;
        }
    }

    public static void sync(BlockEntity be) {
        be.setChanged();
        if (be.getLevel() != null && !be.getLevel().isClientSide) {
            BlockState state = be.getBlockState();
            be.getLevel().sendBlockUpdated(be.getBlockPos(), state, state, 3);
        }
    }
}
