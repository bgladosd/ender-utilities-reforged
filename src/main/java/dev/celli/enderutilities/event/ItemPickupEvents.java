package dev.celli.enderutilities.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemHandyBag;
import dev.celli.enderutilities.item.ItemNullifier;
import dev.celli.enderutilities.item.ItemPickupManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = EnderUtilities.MOD_ID)
public final class ItemPickupEvents {
    private ItemPickupEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        // Orig order: Pickup Manager, then Handy Bag, then Nullifier. A deny/transport
        // from the manager must skip the later interceptors.
        if (ItemPickupManager.handlePickup(event)) {
            event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
            return;
        }
        if (ItemHandyBag.handlePickup(event) || ItemNullifier.handlePickup(event)) {
            event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
        }
    }

    public static boolean handlePlayerItemPickup(Player player, Collection<ItemEntity> drops) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEntity entity : drops) {
            if (!entity.getItem().isEmpty()) {
                stacks.add(entity.getItem());
            }
        }
        boolean stop = ItemPickupManager.handleStacks(player, stacks);
        if (!stop) {
            ItemHandyBag.handleStacks(player, stacks);
            ItemNullifier.handleStacks(player, stacks);
        }
        drops.removeIf(entity -> entity.getItem().isEmpty());
        return drops.isEmpty();
    }
}
