package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ItemEnderUtilities extends Item {
    public ItemEnderUtilities(Properties properties) {
        super(properties);
    }

    /**
     * 1.21 treats {@code SUCCESS}/{@code CONSUME} from {@code use()} as a used-item on the client
     * ({@code ItemInHandRenderer.itemUsed}), which repeats every right-click tick unless
     * {@code startUsingItem} was called. Orig 1.12 SUCCESS did not hide the held item.
     */
    public static InteractionResultHolder<ItemStack> handledWithoutUseAnim(ItemStack stack, boolean clientSide) {
        return clientSide ? InteractionResultHolder.pass(stack) : InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EnderItemTooltips.addTranslatedTips(getDescriptionId() + ".tooltips", tooltip, ClientAccess.isShiftDown());
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
