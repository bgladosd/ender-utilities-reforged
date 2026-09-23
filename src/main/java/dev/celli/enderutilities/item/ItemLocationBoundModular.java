package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.data.ModularItemData;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.item.module.IModular;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public abstract class ItemLocationBoundModular extends Item implements IModular, IKeyBound {
    public ItemLocationBoundModular(Properties properties) {
        super(properties.component(ModDataComponents.MODULAR, ModularItemData.EMPTY));
    }

    public boolean useBindLocking(ItemStack stack) {
        return false;
    }

    public boolean isBindLocked(ItemStack stack) {
        return ModularHelper.data(stack).bindLocked();
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
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (!level.isClientSide) {
            if (!useBindLocking(stack) || !isBindLocked(stack)) {
                boolean adjust = ModularHelper.getSelectedModuleTier(stack, ModuleType.LINKCRYSTAL) == ItemLinkCrystal.TYPE_LOCATION;
                BlockPos pos = context.getClickedPos();
                ModularHelper.setTarget(
                        stack,
                        player,
                        pos,
                        context.getClickedFace(),
                        context.getClickLocation().x - pos.getX(),
                        context.getClickLocation().y - pos.getY(),
                        context.getClickLocation().z - pos.getZ(),
                        adjust,
                        false
                );
                player.displayClientMessage(Component.translatable("enderutilities.chat.message.itemboundtolocation"), true);
            } else {
                player.displayClientMessage(Component.translatable("enderutilities.chat.message.itembindlocked"), true);
            }
        }
        return level.isClientSide ? InteractionResult.PASS : InteractionResult.CONSUME;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                ModularHelper.setTargetFromPlayer(stack, player, true);
            }
            return ItemEnderUtilities.handledWithoutUseAnim(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL)) {
            return ModularHelper.changeSelectedModule(
                    stack,
                    ModuleType.LINKCRYSTAL,
                    EnumKey.keypressActionIsReversed(key) || EnumKey.keypressContainsShift(key)
            );
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT) && useBindLocking(stack)) {
            ModularItemData data = ModularHelper.data(stack);
            ModularHelper.setData(stack, data.withBindLocked(!data.bindLocked()));
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_ALT)) {
            ModularHelper.setTargetFromPlayer(stack, player, true);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_ALT)) {
            ItemStack crystal = ModularHelper.getSelectedModuleStack(stack, ModuleType.LINKCRYSTAL);
            var owner = crystal.get(ModDataComponents.OWNER);
            if (!crystal.isEmpty() && owner != null && owner.isOwner(player)) {
                crystal.set(ModDataComponents.OWNER, owner.withPublic(!owner.isPublic()));
                ModularHelper.setSelectedModuleStack(stack, ModuleType.LINKCRYSTAL, crystal);
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        return EnderItemTooltips.namedWithTarget(super.getName(stack), EnderItemTooltips.getTargetDisplayName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean verbose = ClientAccess.isShiftDown();
        if (ModularHelper.data(stack).modules().isEmpty()) {
            EnderItemTooltips.addTranslatedTips(getDescriptionId() + ".tooltips", tooltip, verbose);
            if (commonTooltipKey() != null) {
                EnderItemTooltips.addTranslatedTips(commonTooltipKey(), tooltip, verbose);
            }
        }
        addConfiguredTooltips(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }

    protected String commonTooltipKey() {
        return null;
    }

    protected void addConfiguredTooltips(ItemStack stack, List<Component> tooltip) {
        EnderItemTooltips.addModularTooltips(this, stack, tooltip);
    }

    @Override
    public int getMaxModules(ItemStack container, ItemStack moduleStack) {
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IModule module)) {
            return 0;
        }
        return getMaxModules(container, module.getModuleType(moduleStack));
    }
}
