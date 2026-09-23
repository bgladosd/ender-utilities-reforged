package dev.celli.enderutilities.item;

import dev.celli.enderutilities.util.TeleportUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemEnderArrow extends ItemEnderUtilities {
    public ItemEnderArrow(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof LivingEntity && !player.level().isClientSide) {
            TeleportUtil.teleportEntityRandomly(entity, 10.0);
            return true;
        }
        return entity instanceof LivingEntity;
    }
}
