package dev.celli.enderutilities.block;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import dev.celli.enderutilities.blockentity.PortalPanelBlockEntity;
import dev.celli.enderutilities.menu.EnderMenu;
import dev.celli.enderutilities.util.PositionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PortalPanelBlock extends DirectionalBlock implements EntityBlock {
    public static final MapCodec<PortalPanelBlock> CODEC = simpleCodec(PortalPanelBlock::new);
    private static final VoxelShape NORTH = Block.box(0, 0, 11, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 5);
    private static final VoxelShape WEST = Block.box(11, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST = Block.box(0, 0, 0, 5, 16, 16);
    private static final VoxelShape UP = Block.box(0, 0, 0, 16, 5, 16);
    private static final VoxelShape DOWN = Block.box(0, 11, 0, 16, 16, 16);

    private static final AABB PANEL_BOUNDS_BASE = new AABB(1.0, 0.0, 1.0, 0.0, 1.0, 0.75);
    private static final float BTN_X = 15.5f / 16f;
    private static final float BTN_Y1 = 12f / 16f;
    private static final float BTN_Y2 = 1f / 16f;
    private static final float BTN_W = 3f / 16f;
    private static final float BTN_D = 4f / 16f;
    private static final float BTN_ZS = 12f / 16f;
    private static final float BTN_ZE = 11f / 16f;
    private static final AABB BUTTON_1 = new AABB(BTN_X - 0 * BTN_D, BTN_Y1, BTN_ZS, BTN_X - 0 * BTN_D - BTN_W, BTN_Y1 + BTN_W, BTN_ZE);
    private static final AABB BUTTON_2 = new AABB(BTN_X - 1 * BTN_D, BTN_Y1, BTN_ZS, BTN_X - 1 * BTN_D - BTN_W, BTN_Y1 + BTN_W, BTN_ZE);
    private static final AABB BUTTON_3 = new AABB(BTN_X - 2 * BTN_D, BTN_Y1, BTN_ZS, BTN_X - 2 * BTN_D - BTN_W, BTN_Y1 + BTN_W, BTN_ZE);
    private static final AABB BUTTON_4 = new AABB(BTN_X - 3 * BTN_D, BTN_Y1, BTN_ZS, BTN_X - 3 * BTN_D - BTN_W, BTN_Y1 + BTN_W, BTN_ZE);
    private static final AABB BUTTON_5 = new AABB(BTN_X - 0 * BTN_D, BTN_Y2, BTN_ZS, BTN_X - 0 * BTN_D - BTN_W, BTN_Y2 + BTN_W, BTN_ZE);
    private static final AABB BUTTON_6 = new AABB(BTN_X - 1 * BTN_D, BTN_Y2, BTN_ZS, BTN_X - 1 * BTN_D - BTN_W, BTN_Y2 + BTN_W, BTN_ZE);
    private static final AABB BUTTON_7 = new AABB(BTN_X - 2 * BTN_D, BTN_Y2, BTN_ZS, BTN_X - 2 * BTN_D - BTN_W, BTN_Y2 + BTN_W, BTN_ZE);
    private static final AABB BUTTON_8 = new AABB(BTN_X - 3 * BTN_D, BTN_Y2, BTN_ZS, BTN_X - 3 * BTN_D - BTN_W, BTN_Y2 + BTN_W, BTN_ZE);
    private static final AABB BUTTON_M = new AABB(4f / 16f, 5.5f / 16f, BTN_ZS, 12f / 16f, 10.5f / 16f, 10.5f / 16f);

    public PortalPanelBlock(Properties properties) {
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
        return defaultBlockState().setValue(FACING, context.getClickedFace());
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
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortalPanelBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PortalPanelBlockEntity panel) {
            EnderMenu.dropContents(panel.getInventory(), level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Integer id = getPointedElementId(pos, state.getValue(FACING), player);
        if (id != null && id >= 0 && id <= 8) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof PortalPanelBlockEntity te) {
                if (id == 8) {
                    te.tryTogglePortal();
                } else {
                    te.setActiveTargetId(id);
                    level.sendBlockUpdated(pos, state, state, 3);
                }
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.MASTER, 0.5f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof PortalPanelBlockEntity panel) {
            serverPlayer.openMenu(panel, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    public Integer getPointedElementId(BlockPos pos, Direction facing, Player player) {
        Map<Integer, AABB> boxMap = new HashMap<>();
        Vec3 reference = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        boxMap.put(0, PositionUtils.rotateBoxAroundPoint(BUTTON_1.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(1, PositionUtils.rotateBoxAroundPoint(BUTTON_2.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(2, PositionUtils.rotateBoxAroundPoint(BUTTON_3.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(3, PositionUtils.rotateBoxAroundPoint(BUTTON_4.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(4, PositionUtils.rotateBoxAroundPoint(BUTTON_5.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(5, PositionUtils.rotateBoxAroundPoint(BUTTON_6.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(6, PositionUtils.rotateBoxAroundPoint(BUTTON_7.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(7, PositionUtils.rotateBoxAroundPoint(BUTTON_8.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(8, PositionUtils.rotateBoxAroundPoint(BUTTON_M.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(9, PositionUtils.rotateBoxAroundPoint(PANEL_BOUNDS_BASE.move(pos), reference, Direction.NORTH, facing));
        return PositionUtils.getPointedBox(player, 6.0, boxMap);
    }

    @Nullable
    public AABB getPointedHighlightBox(BlockPos pos, Direction facing, Player player) {
        Map<Integer, AABB> boxMap = new HashMap<>();
        Vec3 reference = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        boxMap.put(0, PositionUtils.rotateBoxAroundPoint(BUTTON_1.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(1, PositionUtils.rotateBoxAroundPoint(BUTTON_2.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(2, PositionUtils.rotateBoxAroundPoint(BUTTON_3.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(3, PositionUtils.rotateBoxAroundPoint(BUTTON_4.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(4, PositionUtils.rotateBoxAroundPoint(BUTTON_5.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(5, PositionUtils.rotateBoxAroundPoint(BUTTON_6.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(6, PositionUtils.rotateBoxAroundPoint(BUTTON_7.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(7, PositionUtils.rotateBoxAroundPoint(BUTTON_8.move(pos), reference, Direction.NORTH, facing));
        boxMap.put(8, PositionUtils.rotateBoxAroundPoint(BUTTON_M.move(pos), reference, Direction.NORTH, facing));
        Integer id = PositionUtils.getPointedBox(player, 6.0, boxMap);
        return id == null ? null : boxMap.get(id);
    }
}
