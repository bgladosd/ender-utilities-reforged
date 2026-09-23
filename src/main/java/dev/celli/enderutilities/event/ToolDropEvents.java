package dev.celli.enderutilities.event;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.module.IAnvilRepairable;
import dev.celli.enderutilities.item.tool.ItemEnderSword;
import dev.celli.enderutilities.item.tool.ItemEnderTool;
import dev.celli.enderutilities.item.tool.ItemVoidPickaxe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = EnderUtilities.MOD_ID)
public final class ToolDropEvents {
    private static final int ANVIL_REPAIR_XP = 15;

    private ToolDropEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        ItemStack tool = event.getPlayer().getMainHandItem();
        if (tool.getItem() instanceof ItemVoidPickaxe pick) {
            pick.handleBreakEvent(event);
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.getItem() instanceof ItemVoidPickaxe
                || (event.getBreaker() instanceof Player player && player.getMainHandItem().getItem() instanceof ItemVoidPickaxe)) {
            event.setCanceled(true);
            return;
        }
        if (tool.getItem() instanceof ItemEnderTool ender) {
            ender.handleHarvestDrops(tool, event);
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!(left.getItem() instanceof IAnvilRepairable repairable) || right.isEmpty()) {
            return;
        }
        if (repairable.isRepairItem(left, right)) {
            ItemStack repaired = left.copy();
            if (!repairable.repairItem(repaired, -1)) {
                event.setCanceled(true);
                return;
            }
            event.setMaterialCost(1);
            event.setCost(ANVIL_REPAIR_XP);
            applyAnvilName(event, repaired);
            event.setOutput(repaired);
            return;
        }
        if (right.is(Items.ENCHANTED_BOOK)) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (tool.getItem() instanceof ItemEnderSword sword) {
            sword.handleLivingDrops(tool, player, event);
        }
    }

    private static void applyAnvilName(AnvilUpdateEvent event, ItemStack output) {
        String name = event.getName();
        if (name == null) {
            return;
        }
        if (!name.isBlank()) {
            output.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            event.setCost(event.getCost() + 1);
        } else if (output.has(DataComponents.CUSTOM_NAME)) {
            output.remove(DataComponents.CUSTOM_NAME);
            event.setCost(event.getCost() + 1);
        }
    }
}
