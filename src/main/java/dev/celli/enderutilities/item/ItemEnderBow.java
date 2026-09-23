package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.entity.EntityEnderArrow;
import dev.celli.enderutilities.item.module.IAnvilRepairable;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class ItemEnderBow extends ItemLocationBoundModular implements IAnvilRepairable {
    public static final int ENDER_CHARGE_COST_MOB_TP = 1000;
    public static final byte BOW_MODE_TP_TARGET = 0;
    public static final byte BOW_MODE_TP_SELF = 1;

    public ItemEnderBow(Properties properties) {
        super(properties.stacksTo(1).durability(384));
    }

    @Override
    public boolean useBindLocking(ItemStack stack) {
        return true;
    }

    public byte getBowMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_MODE, 0).byteValue();
    }

    public boolean isBroken(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (isBroken(stack) || level.isClientSide) {
            return;
        }
        Player player = living instanceof Player p ? p : null;
        byte mode = getBowMode(stack);
        if (mode == BOW_MODE_TP_TARGET && player != null && !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)) {
            return;
        }
        if (mode == BOW_MODE_TP_SELF && !ModCommonConfig.ENDER_BOW_ALLOW_SELF_TP.getAsBoolean()) {
            return;
        }
        if (player != null && !player.getAbilities().instabuild && !player.getInventory().contains(new ItemStack(ModItems.ENDER_ARROW.get()))) {
            return;
        }
        float f = (getUseDuration(stack, living) - timeLeft) / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f < 0.1F) {
            return;
        }
        if (f > 1.0F) {
            f = 1.0F;
        }
        EntityEnderArrow arrow = new EntityEnderArrow(level, living, f * 2.0F);
        arrow.setTpMode(mode);
        if (mode == BOW_MODE_TP_TARGET) {
            TargetLocation target = ModularHelper.getSelectedTarget(stack);
            if (target == null) {
                return;
            }
            arrow.setTpTarget(target);
            if (ModularHelper.getInstalledModuleCount(stack, ModuleType.MOBPERSISTENCE) > 0) {
                arrow.setPersistence(true);
            }
        }
        if (player != null && !player.getAbilities().instabuild) {
            if (mode == BOW_MODE_TP_TARGET && !ModularHelper.useEnderCharge(stack, ENDER_CHARGE_COST_MOB_TP, false)) {
                return;
            }
            consumeArrow(player);
            stack.hurtAndBreak(1, player, living.getEquipmentSlotForItem(stack));
        }
        if (f == 1.0F) {
            arrow.setCritArrow(true);
        }
        level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL,
                1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);
        level.addFreshEntity(arrow);
    }

    private static void consumeArrow(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.ENDER_ARROW.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isBroken(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        if (getBowMode(stack) == BOW_MODE_TP_TARGET) {
            if ((!player.getAbilities().instabuild && !ModularHelper.useEnderCharge(stack, ENDER_CHARGE_COST_MOB_TP, true))
                    || !ModularHelper.canAccessSelectedModule(stack, ModuleType.LINKCRYSTAL, player)
                    || ModularHelper.getSelectedTarget(stack) == null) {
                return InteractionResultHolder.pass(stack);
            }
        }
        if (player.getAbilities().instabuild || player.getInventory().contains(new ItemStack(ModItems.ENDER_ARROW.get()))) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (key == HotKeys.KEYBIND_ID_TOGGLE_MODE) {
            if (!ModCommonConfig.ENDER_BOW_ALLOW_SELF_TP.getAsBoolean()) {
                stack.set(ModDataComponents.ITEM_MODE, (int) BOW_MODE_TP_TARGET);
            } else {
                stack.set(ModDataComponents.ITEM_MODE, getBowMode(stack) == BOW_MODE_TP_TARGET ? (int) BOW_MODE_TP_SELF : (int) BOW_MODE_TP_TARGET);
            }
            return true;
        }
        return super.doKeyBindingAction(player, stack, key);
    }

    @Override
    protected String commonTooltipKey() {
        return "enderutilities.tooltips.itemlocationboundmodular";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (getBowMode(stack) == BOW_MODE_TP_SELF) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.mode")
                    .append(": ")
                    .append(Component.translatable("enderutilities.tooltip.item.tpself")));
        } else {
            super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(Component.translatable("enderutilities.tooltip.item.mode")
                    .append(": ")
                    .append(Component.translatable("enderutilities.tooltip.item.tptarget")));
            return;
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 10;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType type) {
        return switch (type) {
            case ENDERCAPACITOR -> 1;
            case LINKCRYSTAL -> 8;
            case MOBPERSISTENCE -> 1;
            default -> 0;
        };
    }

    @Override
    public boolean repairItem(ItemStack stack, int amount) {
        if (amount == -1) {
            amount = stack.getMaxDamage();
        }
        int damage = Math.max(stack.getDamageValue() - amount, 0);
        boolean repaired = damage != stack.getDamageValue();
        stack.setDamageValue(damage);
        return repaired;
    }

    @Override
    public boolean isRepairItem(ItemStack stackTool, ItemStack stackMaterial) {
        return stackMaterial.is(ModItems.ENDER_ALLOY_ADVANCED.get());
    }
}
