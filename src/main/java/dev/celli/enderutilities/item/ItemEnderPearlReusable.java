package dev.celli.enderutilities.item;

import dev.celli.enderutilities.entity.EntityEnderPearlReusable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemEnderPearlReusable extends ItemEnderUtilities {
    private final boolean elite;

    public ItemEnderPearlReusable(Properties properties, boolean elite) {
        super(properties.stacksTo(4));
        this.elite = elite;
    }

    public boolean isElite() {
        return elite;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_PEARL_THROW,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        player.awardStat(Stats.ITEM_USED.get(this));

        if (!level.isClientSide) {
            EntityEnderPearlReusable pearl = new EntityEnderPearlReusable(level, player, elite);
            float velocity = elite ? 2.5F : 1.9F;
            pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, 0.8F);
            level.addFreshEntity(pearl);
            if (elite) {
                Entity bottom = player.getRootVehicle();
                if (bottom instanceof EntityEnderPearlReusable && !bottom.getPassengers().isEmpty()) {
                    bottom = bottom.getPassengers().getFirst();
                }
                bottom.startRiding(pearl, true);
            }
        }

        // 1.12 creative ignored stack.shrink, so masa never returned the pearl (canPickUp=false)
        // to avoid dupes. 1.21 actually shrinks, so skip consume here to keep that behavior.
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
