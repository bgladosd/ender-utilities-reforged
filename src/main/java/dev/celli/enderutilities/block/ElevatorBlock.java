package dev.celli.enderutilities.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.celli.enderutilities.blockentity.ElevatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ElevatorBlock extends Block implements EntityBlock {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final MapCodec<ElevatorBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            Kind.CODEC.fieldOf("kind").forGetter(block -> block.kind)
    ).apply(instance, ElevatorBlock::new));

    public enum Kind {
        FULL,
        SLAB,
        LAYER;

        public static final com.mojang.serialization.Codec<Kind> CODEC =
                com.mojang.serialization.Codec.STRING.xmap(Kind::valueOf, Kind::name);
    }

    private static final VoxelShape SLAB_BOTTOM = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape SLAB_TOP = Block.box(0, 8, 0, 16, 16, 16);
    private static final VoxelShape LAYER_BOTTOM = Block.box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape LAYER_TOP = Block.box(0, 14, 0, 16, 16, 16);

    private final Kind kind;

    public ElevatorBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        BlockState state = this.stateDefinition.any().setValue(COLOR, DyeColor.WHITE);
        if (kind != Kind.FULL) {
            state = state.setValue(BlockStateProperties.HALF, Half.BOTTOM);
        }
        this.registerDefaultState(state);
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
        if (kind != Kind.FULL) {
            builder.add(BlockStateProperties.HALF);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        ItemStack stack = context.getItemInHand();
        var blockState = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        if (blockState != null) {
            state = blockState.apply(state);
        }
        DyeColor fromComponent = stack.get(dev.celli.enderutilities.registry.ModDataComponents.ELEVATOR_COLOR);
        if (fromComponent != null) {
            state = state.setValue(COLOR, fromComponent);
        }
        if (kind != Kind.FULL) {
            Direction clicked = context.getClickedFace();
            Half half = clicked == Direction.DOWN || (clicked != Direction.UP && context.getClickLocation().y - context.getClickedPos().getY() > 0.5)
                    ? Half.TOP : Half.BOTTOM;
            if (clicked == Direction.UP) {
                half = Half.BOTTOM;
            }
            if (clicked == Direction.DOWN) {
                half = Half.TOP;
            }
            state = state.setValue(BlockStateProperties.HALF, half);
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (kind) {
            case FULL -> Shapes.block();
            case SLAB -> state.getValue(BlockStateProperties.HALF) == Half.TOP ? SLAB_TOP : SLAB_BOTTOM;
            case LAYER -> state.getValue(BlockStateProperties.HALF) == Half.TOP ? LAYER_TOP : LAYER_BOTTOM;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (dev.celli.enderutilities.util.CamouflageHelper.tryApply(level, pos, player, stack, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof DyeItem dye && state.getValue(COLOR) != dye.getDyeColor()) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(COLOR, dye.getDyeColor()), 3);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (dev.celli.enderutilities.util.CamouflageHelper.tryInteract(level, pos, player, InteractionHand.MAIN_HAND, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElevatorBlockEntity(pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof ElevatorBlockEntity elevator) {
            elevator.neighborChanged();
        }
    }
}
