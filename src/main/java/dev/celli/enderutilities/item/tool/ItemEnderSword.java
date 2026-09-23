package dev.celli.enderutilities.item.tool;

import java.util.List;

import dev.celli.enderutilities.item.ItemLocationBoundModular;
import dev.celli.enderutilities.item.module.IAnvilRepairable;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.ChunkLoading;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ItemEnderSword extends ItemLocationBoundModular implements IAnvilRepairable {
    public static final int ENDER_CHARGE_COST = 50;
    public static final int DROPS_NORMAL = 0;
    public static final int DROPS_PLAYER = 1;
    public static final int DROPS_REMOTE = 2;
    public static final int DROPS_SUMMON = 3;

    public ItemEnderSword(Properties properties) {
        super(properties.stacksTo(1).durability(1561).attributes(
                net.minecraft.world.item.SwordItem.createAttributes(net.minecraft.world.item.Tiers.DIAMOND, 3, -2.4F)));
    }

    public int dropsMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TOOL_DROPS, 0);
    }

    public void handleLivingDrops(ItemStack tool, Player player, net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide || tool.getDamageValue() >= tool.getMaxDamage() - 1) {
            return;
        }
        int mode = dropsMode(tool);
        if (mode == DROPS_NORMAL || mode == DROPS_SUMMON) {
            return;
        }
        int core = ModularHelper.getMaxModuleTier(tool, ModuleType.ENDERCORE);
        if (mode == DROPS_PLAYER && core >= 0
                && dev.celli.enderutilities.event.ItemPickupEvents.handlePlayerItemPickup(player, event.getDrops())) {
            return;
        }
        var inv = linkedInventory(tool, player);
        boolean transported = ToolDropHandler.transportDrops(event.getDrops(), inv, tool, player, mode == DROPS_REMOTE);
        if (transported && mode == DROPS_REMOTE) {
            ModularHelper.useEnderCharge(tool, ENDER_CHARGE_COST, false);
        }
        if (!event.getDrops().isEmpty() && core >= 0) {
            dev.celli.enderutilities.event.ItemPickupEvents.handlePlayerItemPickup(player, event.getDrops());
        }
    }

    net.neoforged.neoforge.items.IItemHandler linkedInventory(ItemStack tool, Player player) {
        int mode = dropsMode(tool);
        int core = ModularHelper.getMaxModuleTier(tool, ModuleType.ENDERCORE);
        if (mode == DROPS_PLAYER && core >= 0) {
            return dev.celli.enderutilities.util.InventoryHelper.playerHandler(player);
        }
        if (mode == DROPS_REMOTE && core >= 1 && ModularHelper.useEnderCharge(tool, ENDER_CHARGE_COST, true)) {
            var target = ModularHelper.getSelectedTarget(tool);
            if (target == null) {
                return null;
            }
            if (!target.dimension().equals(player.level().dimension()) && core != 2) {
                return null;
            }
            return ModularHelper.getBoundInventory(tool, player, ChunkLoading.TIMEOUT_TOOL_INVENTORY);
        }
        return null;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (dropsMode(stack) == DROPS_SUMMON && !target.level().isClientSide) {
            dev.celli.enderutilities.entity.EntityEndermanFighter.summonFighters(target.level(), target, 3);
        }
        if (stack.getDamageValue() < stack.getMaxDamage() - 1) {
            stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT)) {
            stack.set(ModDataComponents.TOOL_DROPS, (dropsMode(stack) + 1) % 4);
            return true;
        }
        return super.doKeyBindingAction(player, stack, key);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int mode = dropsMode(stack);
        String modeKey = switch (mode) {
            case DROPS_PLAYER -> "enderutilities.tooltip.item.endertool.playerinv";
            case DROPS_REMOTE -> "enderutilities.tooltip.item.endertool.remote";
            case DROPS_SUMMON -> "enderutilities.tooltip.item.endersword.summon";
            default -> "enderutilities.tooltip.item.normal";
        };
        tooltip.add(Component.translatable("enderutilities.tooltip.item.mode")
                .append(": ")
                .append(Component.translatable(modeKey).withStyle(ChatFormatting.DARK_GREEN)));
        ItemEnderTool.addEnderCoreTooltip(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility itemAbility) {
        return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(itemAbility);
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 5;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType type) {
        return switch (type) {
            case ENDERCAPACITOR -> 1;
            case LINKCRYSTAL -> 3;
            case ENDERCORE -> 1;
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
        return stackMaterial.is(dev.celli.enderutilities.registry.ModItems.ENDER_ALLOY_ADVANCED.get());
    }
}
