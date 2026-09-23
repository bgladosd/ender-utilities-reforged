package dev.celli.enderutilities.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemSyringe extends ItemEnderUtilities {
    public static final String TAG_PASSIFIED = "enderutilities:passified";

    public enum Kind {
        EMPTY,
        PARALYZER,
        STIMULANT,
        PASSIFIER
    }

    private final Kind kind;

    public ItemSyringe(Properties properties, Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (kind == Kind.EMPTY || !(target instanceof Mob living)) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (kind == Kind.PARALYZER || kind == Kind.STIMULANT) {
            boolean noAi = kind == Kind.PARALYZER;
            if (living.isNoAi() == noAi) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                living.setNoAi(noAi);
                consume(stack, player, hand);
                level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.MASTER, 0.9F, 3.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind == Kind.PASSIFIER) {
            if (!level.isClientSide && passifyEntity(living)) {
                consume(stack, player, hand);
                level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.MASTER, 0.9F, 3.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static void consume(ItemStack stack, Player player, InteractionHand hand) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack empty = new ItemStack(dev.celli.enderutilities.registry.ModItems.SYRINGE_EMPTY.get());
            if (stack.isEmpty()) {
                player.setItemInHand(hand, empty);
            } else if (!player.addItem(empty)) {
                player.drop(empty, false);
            }
        }
    }

    public static boolean passifyEntity(Mob living) {
        living.addTag(TAG_PASSIFIED);
        living.setTarget(null);
        return true;
    }

    public static void removePassifiedState(Mob living) {
        living.removeTag(TAG_PASSIFIED);
    }

    public static boolean isPassified(LivingEntity living) {
        return living.getTags().contains(TAG_PASSIFIED);
    }
}
