package dev.celli.enderutilities.block;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import dev.celli.enderutilities.blockentity.EnergyBridgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class EnergyBridgeBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final EnergyBridgeBlockEntity.Type bridgeType;
    private final MapCodec<EnergyBridgeBlock> codec;

    public EnergyBridgeBlock(Properties properties, EnergyBridgeBlockEntity.Type bridgeType) {
        super(properties);
        this.bridgeType = bridgeType;
        this.codec = simpleCodec(props -> new EnergyBridgeBlock(props, bridgeType));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    public EnergyBridgeBlockEntity.Type getBridgeType() {
        return bridgeType;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = bridgeType == EnergyBridgeBlockEntity.Type.RESONATOR
                ? context.getHorizontalDirection().getOpposite()
                : Direction.NORTH;
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyBridgeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof EnergyBridgeBlockEntity bridge) {
                bridge.serverTick();
            }
        };
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())
                && level.getBlockEntity(pos) instanceof EnergyBridgeBlockEntity bridge) {
            bridge.tryAssembleMultiBlock();
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof EnergyBridgeBlockEntity bridge) {
            bridge.disassembleMultiblock();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
