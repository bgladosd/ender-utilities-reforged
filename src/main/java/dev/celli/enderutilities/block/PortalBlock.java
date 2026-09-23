package dev.celli.enderutilities.block;

import com.mojang.serialization.MapCodec;

import dev.celli.enderutilities.blockentity.PortalBlockEntity;
import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.event.PortalTickHandler;
import dev.celli.enderutilities.registry.ModBlocks;
import dev.celli.enderutilities.util.PositionUtils;
import dev.celli.enderutilities.util.TeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PortalBlock extends DirectionalBlock implements EntityBlock {
    public static final MapCodec<PortalBlock> CODEC = simpleCodec(PortalBlock::new);
    private static final String COOLDOWN_TAG = "EUPortalTime";
    private static final VoxelShape X = Block.box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape Z = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Y = Block.box(0, 6, 0, 16, 10, 16);

    public PortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalBlockEntity(pos, state);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        Direction facing = state.getValue(FACING);
        return side != facing && side != facing.getOpposite();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.PORTAL, x, y, z,
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.5);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            PortalTickHandler.addPlayerToTeleport(player);
        } else {
            teleportEntity(level, pos, state, entity);
        }
    }

    public void teleportEntity(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (world.isClientSide) {
            return;
        }
        if (!(world.getBlockEntity(pos) instanceof PortalBlockEntity te) || te.getDestination() == null) {
            return;
        }
        if (!entity.getBoundingBox().intersects(state.getShape(world, pos).bounds().move(pos))) {
            return;
        }
        long currentTime = world.getGameTime();
        if (!isEntityUnderPortalCooldown(entity, currentTime, 10)) {
            OwnerInfo ownerData = te.getOwner();
            if (ownerData == null || ownerData.canAccess(entity)) {
                TargetLocation target = te.getDestination();
                if (te.targetIsPortal()) {
                    var destWorld = entity.getServer() == null ? null : entity.getServer().getLevel(target.dimension());
                    if (destWorld == null || !destWorld.getBlockState(target.pos()).is(ModBlocks.PORTAL.get())) {
                        return;
                    }
                }
                entity = TeleportUtil.teleportUsingTarget(entity, target, false, true, true);
            }
        }
        if (entity != null) {
            updatePortalCooldown(entity, currentTime);
        }
    }

    private boolean isEntityUnderPortalCooldown(Entity entity, long currentTime, int cooldown) {
        long last = entity.getPersistentData().getLong(COOLDOWN_TAG);
        return (currentTime - last) < cooldown;
    }

    private void updatePortalCooldown(Entity entity, long currentTime) {
        entity.getPersistentData().putLong(COOLDOWN_TAG, currentTime);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!checkCanStayAndScheduleBreaking(level, pos, state)) {
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.2f, 0.8f);
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && !checkCanStayAndScheduleBreaking(level, pos, state)) {
            level.scheduleTick(pos, this, 0);
        }
    }

    private boolean checkCanStayAndScheduleBreaking(Level world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos[] positions = PositionUtils.getAdjacentPositions(pos, facing, false);
        boolean canStay = true;
        for (BlockPos posTmp : positions) {
            Block block = world.getBlockState(posTmp).getBlock();
            if (block != this && block != ModBlocks.PORTAL_FRAME.get()) {
                for (BlockPos posTmp2 : positions) {
                    Block adjacent = world.getBlockState(posTmp2).getBlock();
                    if (adjacent == this) {
                        world.scheduleTick(posTmp2, adjacent, 0);
                    }
                }
                canStay = false;
                break;
            }
        }
        return canStay;
    }
}
