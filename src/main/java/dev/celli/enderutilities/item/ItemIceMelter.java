package dev.celli.enderutilities.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;

public class ItemIceMelter extends ItemEnderUtilities {
    /**
     * Orig Super flag {@code 2}: notify clients, do not {@code neighborChanged}.
     * {@code UPDATE_KNOWN_SHAPE} also skips neighbor {@code updateShape}, which 1.21
     * {@code LiquidBlock} uses to {@code scheduleTick} (1.12 did not).
     * 1.21.1 has no {@code UPDATE_SKIP_ON_PLACE}.
     */
    private static final int SUPER_PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private final boolean superMelter;

    public ItemIceMelter(Properties properties, boolean superMelter) {
        super(properties.stacksTo(1));
        this.superMelter = superMelter;
    }

    @Override
    public String getDescriptionId() {
        return superMelter ? "item.enderutilities.icemelter_super" : "item.enderutilities.icemelter_basic";
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!level.getBlockState(pos).is(Blocks.ICE) || (player != null && !player.mayInteract(level, pos))) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int num = 8;
            float velocity = 0.2F;
            if (level.dimensionType().ultraWarm()) {
                level.removeBlock(pos, false);
                num = 32;
                velocity = 0.4F;
            } else if (superMelter) {
                placeStillWaterWithoutUpdates(level, pos);
            } else {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                        num, 0.5, 0.5, 0.5, velocity);
            }
            level.playSound(null, player != null ? player.blockPosition() : pos,
                    SoundEvents.LAVA_EXTINGUISH, SoundSource.MASTER, 0.8F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Orig Super used {@code setBlockState(..., 2)}: notify clients, skip neighborChanged.
     * 1.12 water only started flowing from neighborChanged, so that left a still source.
     * 1.21 {@code LiquidBlock.onPlace} always {@code scheduleTick}s, and
     * {@code updateShape}/{@code neighborChanged} do the same — source water still
     * {@code spread()}s when that tick runs. Clearing the one onPlace tick is not enough
     * if onPlace can run again or a neighbor shape update schedules a new one.
     */
    private static void placeStillWaterWithoutUpdates(Level level, BlockPos pos) {
        pos = pos.immutable();
        BlockState oldState = level.getBlockState(pos);
        BlockState water = Fluids.WATER.defaultFluidState().createLegacyBlock();
        setBlockSkippingOnPlace(level, pos, water, SUPER_PLACE_FLAGS);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(pos, oldState, water, SUPER_PLACE_FLAGS);
            BoundingBox box = new BoundingBox(pos);
            serverLevel.getFluidTicks().clearArea(box);
            serverLevel.getBlockTicks().clearArea(box);
        }
    }

    /**
     * 1.21.1 {@code LevelChunk.setBlockState} always calls {@code onPlace} (no
     * {@code UPDATE_SKIP_ON_PLACE}). NeoForge's snapshot capture skips that call
     * while still writing the chunk, heightmaps, and lighting.
     */
    private static void setBlockSkippingOnPlace(Level level, BlockPos pos, BlockState state, int flags) {
        boolean capturing = level.captureBlockSnapshots;
        int snapshotCount = level.capturedBlockSnapshots.size();
        level.captureBlockSnapshots = true;
        try {
            level.setBlock(pos, state, flags);
        } finally {
            level.captureBlockSnapshots = capturing;
            var snapshots = level.capturedBlockSnapshots;
            while (snapshots.size() > snapshotCount) {
                snapshots.removeLast();
            }
        }
    }
}
