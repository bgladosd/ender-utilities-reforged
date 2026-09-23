package dev.celli.enderutilities.block;

import com.mojang.serialization.MapCodec;

import dev.celli.enderutilities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class MolecularExciterBlock extends DirectionalBlock {
    public static final MapCodec<MolecularExciterBlock> CODEC = simpleCodec(MolecularExciterBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public MolecularExciterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            level.scheduleTick(pos, this, 1);
            if (!state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, true), 2);
            }
        } else if (!level.isClientSide && state.getValue(POWERED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(POWERED, false), 2);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.hasNeighborSignal(pos)) {
            convert(level, pos.relative(state.getValue(FACING)));
        }
    }

    public static void convert(ServerLevel level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return;
        }
        BlockState target = level.getBlockState(pos);
        if (target.isAir() || target.getDestroySpeed(level, pos) < 0.0F) {
            return;
        }
        FallingBlockEntity.fall(level, pos, target);
        level.playSound(null, pos, ModSounds.MOLECULAR_EXCITER.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
