package dev.celli.enderutilities.event.tasks;

import dev.celli.enderutilities.data.WandTemplate;
import dev.celli.enderutilities.item.ItemBuildersWand;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

public final class TaskTemplatePlaceBlocks implements IPlayerTask {
    private static final int BLOCKS_PER_TICK = 10;
    private final WandTemplate template;
    private final BlockPos dest;
    private final Rotation rotation;
    private final ItemBuildersWand.ReplaceMode replace;
    private final boolean entities;
    private final boolean mirror;
    private int index;
    private int failCount;

    public TaskTemplatePlaceBlocks(
            WandTemplate template,
            BlockPos dest,
            Rotation rotation,
            ItemBuildersWand.ReplaceMode replace,
            boolean entities,
            boolean mirror
    ) {
        this.template = template;
        this.dest = dest;
        this.rotation = rotation;
        this.replace = replace;
        this.entities = entities;
        this.mirror = mirror;
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
        int placed = 0;
        while (placed < BLOCKS_PER_TICK && index < template.blocks().size()) {
            var entry = template.blocks().get(index++);
            BlockPos pos = dest.offset(ItemBuildersWand.transformOffset(entry.offset(), rotation, mirror));
            var state = entry.state();
            if (mirror) {
                state = state.mirror(net.minecraft.world.level.block.Mirror.FRONT_BACK);
            }
            if (ItemBuildersWand.placeTemplateBlock(level, player, held, pos, state.rotate(rotation), entry.nbt(), replace)) {
                placed++;
                failCount = 0;
            }
        }
        if (index >= template.blocks().size()) {
            if (entities) {
                ItemBuildersWand.placeTemplateEntities(level, dest, template, rotation, mirror);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return true;
        }
        return failCount > 200;
    }
}
