package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemEnderCapacitor extends Item implements IModule {
    public static final int TIER_BASIC = 0;
    public static final int TIER_ENHANCED = 1;
    public static final int TIER_ADVANCED = 2;
    public static final int TIER_CREATIVE = 3;
    public static final int CHARGE_FROM_END_CRYSTAL = Integer.MAX_VALUE / 4;
    public static final int CHARGE_RATE_FROM_ENERGY_BRIDGE = 100;

    private final int tier;

    public ItemEnderCapacitor(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int getCapacity(ItemStack stack) {
        return switch (tier) {
            case TIER_ENHANCED -> 100000;
            case TIER_ADVANCED -> 500000;
            case TIER_CREATIVE -> 1_000_000_000;
            default -> 10000;
        };
    }

    public int getCharge(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENDER_CHARGE, 0);
    }

    public void setCharge(ItemStack stack, int value) {
        stack.set(ModDataComponents.ENDER_CHARGE, Mth.clamp(value, 0, getCapacity(stack)));
    }

    public int addCharge(ItemStack stack, int amount, boolean doCharge) {
        if (tier == TIER_CREATIVE) {
            return amount;
        }
        int charge = getCharge(stack);
        int capacity = getCapacity(stack);
        int accepted = Math.min(amount, capacity - charge);
        if (doCharge) {
            setCharge(stack, charge + accepted);
        }
        return accepted;
    }

    public int useCharge(ItemStack stack, int amount, boolean doUse) {
        if (amount < 0) {
            return 0;
        }
        if (tier == TIER_CREATIVE && getCharge(stack) > 0) {
            return amount;
        }
        int charge = getCharge(stack);
        int used = Math.min(amount, charge);
        if (doUse) {
            setCharge(stack, charge - used);
        }
        return used;
    }

    public boolean isCreative() {
        return tier == TIER_CREATIVE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (dev.celli.enderutilities.util.EndCrystalInteractions.tryUseHeldItem(player, hand)) {
            return ItemEnderUtilities.handledWithoutUseAnim(player.getItemInHand(hand), level.isClientSide);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && dev.celli.enderutilities.util.EndCrystalInteractions.tryUseHeldItem(player, context.getHand())) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public ModuleType getModuleType(ItemStack stack) {
        return ModuleType.ENDERCAPACITOR;
    }

    @Override
    public int getModuleTier(ItemStack stack) {
        return tier;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    public ItemStack createChargedStack() {
        ItemStack stack = new ItemStack(this);
        setCharge(stack, getCapacity(stack));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!stack.has(ModDataComponents.ENDER_CHARGE)) {
            EnderItemTooltips.addTranslatedTips(getDescriptionId() + ".tooltips", tooltip, ClientAccess.isShiftDown());
        }
        EnderItemTooltips.addChargeLine(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
