package dev.celli.enderutilities.event.tasks;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.item.ItemBuildersWand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class TaskBuildersWand implements IPlayerTask {
    public static final int BLOCKS_PER_TICK = 10;

    private final List<BlockPos> positions;
    private final boolean replace;
    private final BlockState match;
    private final boolean moveStart;
    private final Direction moveFace;
    private int index;
    private int failCount;

    public TaskBuildersWand(
            List<BlockPos> positions,
            boolean replace,
            BlockState match,
            boolean moveStart,
            Direction moveFace
    ) {
        this.positions = new ArrayList<>(positions);
        this.replace = replace;
        this.match = match;
        this.moveStart = moveStart;
        this.moveFace = moveFace;
    }

    @Override
    public boolean canExecute(Level level, Player player) {
        return !level.isClientSide;
    }

    @Override
    public boolean execute(Level level, Player player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemBuildersWand)) {
            failCount++;
            return failCount > 200;
        }
        ItemStack placeStack = ItemBuildersWand.placeStack(player, held, level, player.blockPosition());
        int placed = 0;
        while (placed < BLOCKS_PER_TICK && index < positions.size()) {
            BlockPos pos = positions.get(index++);
            if (ItemBuildersWand.tryPlace(level, player, held, placeStack, pos, moveFace, replace, match)) {
                placed++;
                failCount = 0;
            }
        }
        if (index >= positions.size()) {
            if (moveStart) {
                ItemBuildersWand.advanceStart(held, moveFace);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.4F, 1.0F);
            return true;
        }
        return failCount > 200;
    }
}
