package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.data.ModularItemData;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.item.module.IModular;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public abstract class ItemModular extends Item implements IModular, IKeyBound {
    public ItemModular(Properties properties) {
        super(properties.component(ModDataComponents.MODULAR, ModularItemData.EMPTY));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide
                && dev.celli.enderutilities.util.EnergyBridgeTracker.dimensionHasEnergyBridge(level)
                && (dev.celli.enderutilities.util.WorldUtils.isEndDimension(level)
                        || dev.celli.enderutilities.util.EnergyBridgeTracker.endHasEnergyBridges(level))) {
            ModularHelper.addEnderCharge(stack, ItemEnderCapacitor.CHARGE_RATE_FROM_ENERGY_BRIDGE, true);
        }
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean verbose = ClientAccess.isShiftDown();
        if (ModularHelper.data(stack).modules().isEmpty()) {
            EnderItemTooltips.addTranslatedTips(getDescriptionId() + ".tooltips", tooltip, verbose);
        }
        EnderItemTooltips.addModularTooltips(this, stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public int getMaxModules(ItemStack container, ItemStack moduleStack) {
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IModule module)) {
            return 0;
        }
        return getMaxModules(container, module.getModuleType(moduleStack));
    }
}
