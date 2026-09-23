package dev.celli.enderutilities.block;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HorizontalMachineBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<HorizontalMachineBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            Kind.CODEC.fieldOf("kind").forGetter(block -> block.kind)
    ).apply(instance, HorizontalMachineBlock::new));

    public enum Kind {
        ASU, BARREL, ENDER_FURNACE, INSERTER, INSERTER_FILTERED, CREATION_STATION,
        QUICK_STACKER_ADVANCED, SOUND_BLOCK, MEMORY_CHEST_0, MEMORY_CHEST_1, MEMORY_CHEST_2,
        HANDY_CHEST_0, HANDY_CHEST_1, HANDY_CHEST_2, HANDY_CHEST_3, JSU;

        public static final com.mojang.serialization.Codec<Kind> CODEC =
                com.mojang.serialization.Codec.STRING.xmap(Kind::valueOf, Kind::name);
    }

    private final Kind kind;

    public HorizontalMachineBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        BlockState state = this.stateDefinition.any().setValue(FACING, Direction.NORTH);
        this.registerDefaultState(state);
    }

    public Kind kind() {
        return kind;
    }

    private static final VoxelShape CHEST = Block.box(1, 0, 1, 15, 14, 15);
    private static final VoxelShape INSERTER_NS = Block.box(4, 4, 0, 12, 12, 16);
    private static final VoxelShape INSERTER_WE = Block.box(0, 4, 4, 16, 12, 12);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (kind) {
            case MEMORY_CHEST_0, MEMORY_CHEST_1, MEMORY_CHEST_2,
                 HANDY_CHEST_0, HANDY_CHEST_1, HANDY_CHEST_2, HANDY_CHEST_3 -> CHEST;
            case INSERTER, INSERTER_FILTERED -> {
                Direction facing = state.getValue(FACING);
                yield facing.getAxis() == Direction.Axis.Z ? INSERTER_NS : INSERTER_WE;
            }
            default -> super.getShape(state, level, pos, context);
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (kind == Kind.INSERTER || kind == Kind.INSERTER_FILTERED) {
            if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()
                    && level.getBlockEntity(pos) instanceof dev.celli.enderutilities.blockentity.InserterBlockEntity inserter) {
                inserter.toggleOutputSide(hit.getDirection());
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (kind == Kind.BARREL && !player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof dev.celli.enderutilities.blockentity.BarrelBlockEntity barrel) {
                barrel.onRightClick(player, hit.getDirection());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind == Kind.MEMORY_CHEST_0 || kind == Kind.MEMORY_CHEST_1 || kind == Kind.MEMORY_CHEST_2) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof dev.celli.enderutilities.blockentity.MemoryChestBlockEntity chest
                    && !chest.canAccess(player)) {
                return InteractionResult.FAIL;
            }
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            serverPlayer.openMenu(provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case ASU -> new dev.celli.enderutilities.blockentity.AsuBlockEntity(pos, state);
            case BARREL -> new dev.celli.enderutilities.blockentity.BarrelBlockEntity(pos, state);
            case ENDER_FURNACE -> new dev.celli.enderutilities.blockentity.EnderFurnaceBlockEntity(pos, state);
            case INSERTER -> new dev.celli.enderutilities.blockentity.InserterBlockEntity(pos, state, false);
            case INSERTER_FILTERED -> new dev.celli.enderutilities.blockentity.InserterBlockEntity(pos, state, true);
            case CREATION_STATION -> new dev.celli.enderutilities.blockentity.CreationStationBlockEntity(pos, state);
            case QUICK_STACKER_ADVANCED -> new dev.celli.enderutilities.blockentity.QuickStackerAdvancedBlockEntity(pos, state);
            case SOUND_BLOCK -> new dev.celli.enderutilities.blockentity.SoundBlockEntity(pos, state);
            case MEMORY_CHEST_0 -> new dev.celli.enderutilities.blockentity.MemoryChestBlockEntity(pos, state, 0);
            case MEMORY_CHEST_1 -> new dev.celli.enderutilities.blockentity.MemoryChestBlockEntity(pos, state, 1);
            case MEMORY_CHEST_2 -> new dev.celli.enderutilities.blockentity.MemoryChestBlockEntity(pos, state, 2);
            case HANDY_CHEST_0 -> new dev.celli.enderutilities.blockentity.HandyChestBlockEntity(pos, state, 0);
            case HANDY_CHEST_1 -> new dev.celli.enderutilities.blockentity.HandyChestBlockEntity(pos, state, 1);
            case HANDY_CHEST_2 -> new dev.celli.enderutilities.blockentity.HandyChestBlockEntity(pos, state, 2);
            case HANDY_CHEST_3 -> new dev.celli.enderutilities.blockentity.HandyChestBlockEntity(pos, state, 3);
            case JSU -> new dev.celli.enderutilities.blockentity.JsuBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof ServerTickingBlockEntity ticking) {
                ticking.serverTick();
            }
        };
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (kind == Kind.JSU) {
            BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (be instanceof dev.celli.enderutilities.blockentity.JsuBlockEntity jsu) {
                ItemStack stack = new ItemStack(this);
                jsu.saveToItem(stack, params.getLevel().registryAccess());
                return List.of(stack);
            }
        }
        return super.getDrops(state, params);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide
                && (kind == Kind.HANDY_CHEST_0 || kind == Kind.HANDY_CHEST_1
                        || kind == Kind.HANDY_CHEST_2 || kind == Kind.HANDY_CHEST_3)
                && level.getBlockEntity(pos) instanceof dev.celli.enderutilities.blockentity.HandyChestBlockEntity chest) {
            chest.onLeftClickBlock(player);
        }
        super.attack(state, level, pos, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DroppingInventory be) {
            be.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AnalogInventory analog) {
            return analog.getAnalogSignal();
        }
        return 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof NeighborAwareBlockEntity aware) {
            aware.onNeighborChanged();
        }
        super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
    }

    public interface ServerTickingBlockEntity {
        void serverTick();
    }

    public interface DroppingInventory {
        void dropContents();
    }

    public interface AnalogInventory {
        int getAnalogSignal();
    }

    public interface NeighborAwareBlockEntity {
        void onNeighborChanged();
    }
}
