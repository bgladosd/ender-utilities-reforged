package dev.celli.enderutilities.item;

import dev.celli.enderutilities.entity.EntityChair;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class ItemChairWand extends ItemEnderUtilities implements IKeyBound {
    public ItemChairWand(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            ItemStack stack = context.getItemInHand();
            Player player = context.getPlayer();
            float yOffset = player != null && player.isShiftKeyDown()
                    ? (float) (-context.getClickLocation().y + context.getClickedPos().getY())
                    : 0;
            EntityChair chair = new EntityChair(context.getLevel());
            chair.setChairSize(getEntityWidth(stack), getEntityHeight(stack));
            chair.setPos(
                    context.getClickLocation().x,
                    context.getClickLocation().y + yOffset,
                    context.getClickLocation().z
            );
            context.getLevel().addFreshEntity(chair);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide && entity instanceof EntityChair) {
            entity.discard();
            return true;
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    private float getEntityWidth(ItemStack stack) {
        int value = stack.getOrDefault(dev.celli.enderutilities.registry.ModDataComponents.CHAIR_WIDTH, 0);
        return value == 0 ? 0.5F : value / 16.0F;
    }

    private float getEntityHeight(ItemStack stack) {
        int value = stack.getOrDefault(dev.celli.enderutilities.registry.ModDataComponents.CHAIR_HEIGHT, 0);
        return value == 0 ? 0.75F : value / 16.0F;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.has(dev.celli.enderutilities.registry.ModDataComponents.CHAIR_WIDTH)
                || stack.has(dev.celli.enderutilities.registry.ModDataComponents.CHAIR_HEIGHT)) {
            return Component.literal(String.format("%s - w: %.3f, h: %.3f",
                    super.getName(stack).getString(), getEntityWidth(stack), getEntityHeight(stack)));
        }
        return super.getName(stack);
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT)) {
            cycle(stack, true, !EnumKey.keypressActionIsReversed(key));
            return true;
        }
        if (EnumKey.SCROLL.matches(key, HotKeys.MOD_ALT)) {
            cycle(stack, false, !EnumKey.keypressActionIsReversed(key));
            return true;
        }
        return false;
    }

    private static void cycle(ItemStack stack, boolean width, boolean forward) {
        var component = width
                ? dev.celli.enderutilities.registry.ModDataComponents.CHAIR_WIDTH
                : dev.celli.enderutilities.registry.ModDataComponents.CHAIR_HEIGHT;
        int value = stack.getOrDefault(component, 8);
        if (value == 0) {
            value = 8;
        }
        value += forward ? 1 : -1;
        if (value < 1) {
            value = 64;
        }
        if (value > 64) {
            value = 1;
        }
        stack.set(component, Mth.clamp(value, 1, 64));
    }
}
