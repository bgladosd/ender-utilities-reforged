package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.util.EnderItemTooltips;
import dev.celli.enderutilities.util.StorageContentsTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class ItemBlockEnderUtilities extends BlockItem {
    public ItemBlockEnderUtilities(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EnderItemTooltips.addTranslatedTips(getDescriptionId() + ".tooltips", tooltip, ClientAccess.isShiftDown());
        if (context.registries() != null) {
            StorageContentsTooltip.add(stack, context.registries(), tooltip, 9);
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
