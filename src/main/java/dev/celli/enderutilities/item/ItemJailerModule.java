package dev.celli.enderutilities.item;

import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.util.EntityUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemJailerModule extends ItemEnderUtilities implements IModule {
    public ItemJailerModule(Properties properties) {
        super(properties);
    }

    @Override
    public ModuleType getModuleType(ItemStack stack) {
        return ModuleType.MOBPERSISTENCE;
    }

    @Override
    public int getModuleTier(ItemStack stack) {
        return 0;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Mob mob && EntityUtils.applyMobPersistence(mob)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }
}
