package dev.celli.enderutilities.event;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemBuildersWand;
import dev.celli.enderutilities.item.ItemRuler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = EnderUtilities.MOD_ID)
public final class WandClickEvents {
    private WandClickEvents() {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        if (stack.getItem() instanceof ItemBuildersWand wand) {
            wand.onLeftClickBlock(event.getEntity(), event.getLevel(), stack, event.getPos(), event.getFace());
            event.setCanceled(true);
        } else if (stack.getItem() instanceof ItemRuler ruler) {
            ruler.onLeftClickBlock(event.getEntity(), event.getLevel(), stack, event.getPos(), event.getFace());
            event.setCanceled(true);
        }
    }
}
