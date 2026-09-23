package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.state.BlockState;

public interface CamouflageHolder {
    @Nullable
    BlockState getCamouflage();

    void setCamouflage(@Nullable BlockState camouflage);
}
