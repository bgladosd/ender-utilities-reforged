package dev.celli.enderutilities.block;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FloorBlock extends Block {
    public static final MapCodec<FloorBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            com.mojang.serialization.Codec.BOOL.fieldOf("cracked").forGetter(block -> block.cracked)
    ).apply(instance, FloorBlock::new));
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape TOP = Block.box(0, 13, 0, 16, 16, 16);
    private final boolean cracked;

    public FloorBlock(Properties properties, boolean cracked) {
        super(properties);
        this.cracked = cracked;
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, Half.TOP));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Half half = clicked != Direction.UP && (clicked == Direction.DOWN || context.getClickLocation().y - context.getClickedPos().getY() > 0.5)
                ? Half.TOP : Half.BOTTOM;
        if (clicked == Direction.UP) {
            half = Half.BOTTOM;
        }
        return defaultBlockState().setValue(HALF, half);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == Half.BOTTOM ? BOTTOM : TOP;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (cracked && context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
                return net.minecraft.world.phys.shapes.Shapes.empty();
            }
        }
        return getShape(state, level, pos, context);
    }
}
