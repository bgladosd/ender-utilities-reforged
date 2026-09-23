package dev.celli.enderutilities.util;

import dev.celli.enderutilities.blockentity.CamouflageHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class CamouflageHelper {
    private CamouflageHelper() {
    }

    public static boolean tryApply(Level level, BlockPos pos, Player player, ItemStack stack, BlockHitResult hit) {
        return tryInteract(level, pos, player, player.getUsedItemHand(), hit);
    }

    public static boolean tryInteract(Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CamouflageHolder holder)) {
            return false;
        }
        if (!player.getMainHandItem().isEmpty()) {
            return false;
        }
        if (player.isShiftKeyDown() && holder.getCamouflage() != null) {
            if (!level.isClientSide) {
                holder.setCamouflage(null);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty() || !(offhand.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        if (blockItem.getBlock() == level.getBlockState(pos).getBlock()) {
            return false;
        }
        UseOnContext use = new UseOnContext(player, InteractionHand.OFF_HAND, hit);
        BlockState placed = blockItem.getBlock().getStateForPlacement(new BlockPlaceContext(use));
        if (placed == null) {
            placed = blockItem.getBlock().defaultBlockState();
        }
        if (placed == holder.getCamouflage()) {
            return false;
        }
        if (!level.isClientSide) {
            holder.setCamouflage(placed);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }
}
