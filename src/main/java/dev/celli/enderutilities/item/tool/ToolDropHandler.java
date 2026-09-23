package dev.celli.enderutilities.item.tool;

import java.util.Collection;
import java.util.Iterator;

import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.item.ItemLinkCrystal;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.util.ChunkLoading;
import dev.celli.enderutilities.util.InventoryHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

final class ToolDropHandler {
    private ToolDropHandler() {
    }

    static boolean transportDrops(
            Collection<ItemEntity> drops,
            IItemHandler inv,
            ItemStack tool,
            Player player,
            boolean remote
    ) {
        boolean transported = false;
        Iterator<ItemEntity> it = drops.iterator();
        while (it.hasNext()) {
            ItemEntity entity = it.next();
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) {
                it.remove();
                continue;
            }
            ItemStack leftover = stack.copy();
            if (inv != null) {
                leftover = InventoryHelper.insert(inv, leftover, false);
            } else if (remote && ModularHelper.getSelectedModuleTier(tool, ModuleType.LINKCRYSTAL) == ItemLinkCrystal.TYPE_LOCATION) {
                leftover = spawnAtTarget(leftover, tool, player);
            }
            if (leftover.isEmpty()) {
                it.remove();
                transported = true;
            } else if (leftover.getCount() != stack.getCount()) {
                entity.setItem(leftover);
                transported = true;
            }
        }
        return transported;
    }

    private static ItemStack spawnAtTarget(ItemStack stack, ItemStack tool, Player player) {
        TargetLocation target = ModularHelper.getSelectedTarget(tool);
        if (target == null || !ModularHelper.canAccessSelectedModule(tool, ModuleType.LINKCRYSTAL, player)) {
            return stack;
        }
        int core = ModularHelper.getMaxModuleTier(tool, ModuleType.ENDERCORE);
        if (!target.dimension().equals(player.level().dimension()) && core != 2) {
            return stack;
        }
        if (!(player.level() instanceof ServerLevel server)) {
            return stack;
        }
        ServerLevel dest = server.getServer().getLevel(target.dimension());
        if (dest == null) {
            return stack;
        }
        ChunkLoading.getInstance().forceChunk(dest, target.pos(), ChunkLoading.TIMEOUT_TOOLS, player.getUUID());
        ItemEntity spawned = new ItemEntity(dest, target.exact().x, target.exact().y + 0.125, target.exact().z, stack.copy());
        spawned.setDeltaMovement(0, 0.15, 0);
        if (dest.addFreshEntity(spawned)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }
}
