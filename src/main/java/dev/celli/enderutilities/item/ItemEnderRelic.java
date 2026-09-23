package dev.celli.enderutilities.item;

import dev.celli.enderutilities.util.EntityUtils;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

public class ItemEnderRelic extends ItemEnderUtilities {
    public ItemEnderRelic(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!EntityUtils.spawnEnderCrystal(context.getLevel(), context.getClickedPos())) {
            return InteractionResult.PASS;
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
