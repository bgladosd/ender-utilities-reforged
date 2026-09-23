package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.data.CarriedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

public class ItemDolly extends ItemEnderUtilities {
    public ItemDolly(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            player.swing(context.getHand());
        }
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }
        if (isCarryingBlock(stack)) {
            tryPlaceDownBlock(stack, player, level, context.getClickedPos(), context.getClickedFace());
        } else {
            tryPickUpBlock(stack, player, level, context.getClickedPos(), context.getClickedFace());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        CarriedBlock carried = stack.get(dev.celli.enderutilities.registry.ModDataComponents.CARRIED_BLOCK);
        if (carried != null && !carried.displayName().isEmpty()) {
            return Component.literal(super.getName(stack).getString() + " - " + carried.displayName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CarriedBlock carried = stack.get(dev.celli.enderutilities.registry.ModDataComponents.CARRIED_BLOCK);
        if (carried != null) {
            String name = carried.displayName().isEmpty() ? carried.blockId().toString() : carried.displayName();
            tooltip.add(Component.translatable("enderutilities.tooltip.item.carrying", name));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public static boolean isCarryingBlock(ItemStack stack) {
        return stack.has(dev.celli.enderutilities.registry.ModDataComponents.CARRIED_BLOCK);
    }

    private boolean shouldTryToPickUpBlock(Level level, BlockPos pos, Direction side, Player player) {
        if (player == null || !level.mayInteract(player, pos)) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) != null
                || be instanceof net.minecraft.world.Container);
    }

    private boolean tryPickUpBlock(ItemStack stack, Player player, Level level, BlockPos pos, Direction side) {
        if (isCarryingBlock(stack) || !shouldTryToPickUpBlock(level, pos, side, player)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        CompoundTag beTag = null;
        if (be != null) {
            beTag = be.saveWithFullMetadata(level.registryAccess());
        }
        ItemStack pick = state.getBlock().asItem().getDefaultInstance();
        String name = pick.isEmpty() ? BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString() : pick.getHoverName().getString();
        stack.set(dev.celli.enderutilities.registry.ModDataComponents.CARRIED_BLOCK, new CarriedBlock(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                CarriedBlock.encodeState(state),
                java.util.Optional.ofNullable(beTag),
                dev.celli.enderutilities.util.EntityUtils.getHorizontalLookingDirection(player),
                name
        ));
        level.removeBlockEntity(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    private boolean tryPlaceDownBlock(ItemStack stack, Player player, Level level, BlockPos pos, Direction side) {
        pos = pos.relative(side);
        CarriedBlock carried = stack.get(dev.celli.enderutilities.registry.ModDataComponents.CARRIED_BLOCK);
        if (carried == null || player == null || !level.mayInteract(player, pos)) {
            return false;
        }
        Block block = BuiltInRegistries.BLOCK.get(carried.blockId());
        if (block == Blocks.AIR) {
            return false;
        }
        BlockState state = carried.decodeState(level.registryAccess());
        Direction pickup = carried.pickupFacing();
        Direction current = dev.celli.enderutilities.util.EntityUtils.getHorizontalLookingDirection(player);
        int turns = (current.get2DDataValue() - pickup.get2DDataValue() + 4) % 4;
        for (int i = 0; i < turns; i++) {
            state = state.rotate(level, pos, net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
        }
        if (!state.canSurvive(level, pos) && !level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        if (level.setBlock(pos, state, Block.UPDATE_ALL)) {
            if (carried.blockEntity().isPresent()) {
                CompoundTag tag = carried.blockEntity().get().copy();
                tag.putInt("x", pos.getX());
                tag.putInt("y", pos.getY());
                tag.putInt("z", pos.getZ());
                BlockEntity created = BlockEntity.loadStatic(pos, state, tag, level.registryAccess());
                if (created != null) {
                    level.setBlockEntity(created);
                }
            }
            stack.remove(dev.celli.enderutilities.registry.ModDataComponents.CARRIED_BLOCK);
            return true;
        }
        return false;
    }
}
