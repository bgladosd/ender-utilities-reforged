package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.data.ItemInventoryData;
import dev.celli.enderutilities.item.module.IKeyBoundUnselected;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.InventoryHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;

public class ItemInventorySwapper extends ItemModular implements IKeyBoundUnselected {
    public static final int NUM_PRESETS = 4;
    public static final int GUI_ACTION_SELECT_MODULE = 0;
    public static final int GUI_ACTION_CHANGE_PRESET = 1;
    public static final int GUI_ACTION_TOGGLE_ROWS = 2;
    public static final int GUI_ACTION_TOGGLE_COLUMNS = 3;
    public static final int GUI_ACTION_TOGGLE_LOCKED = 4;
    public static final int GUI_ACTION_TOGGLE_CYCLE_MODE = 5;
    public ItemInventorySwapper(Properties properties) {
        super(properties.stacksTo(1).component(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY));
    }

    public static boolean isEnabled(ItemStack stack) {
        return !ModularHelper.data(stack).bindLocked();
    }

    public static ItemStack getEnabledItem(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ItemInventorySwapper && isEnabled(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static long getEnabledSlotsMask(ItemStack stack) {
        int selected = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).selected();
        return stack.getOrDefault(ModDataComponents.SLOT_MASKS, dev.celli.enderutilities.data.SlotMaskPresets.EMPTY).mask(selected);
    }

    public static void setEnabledSlotsMask(ItemStack stack, long mask) {
        int selected = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).selected();
        var presets = stack.getOrDefault(ModDataComponents.SLOT_MASKS, dev.celli.enderutilities.data.SlotMaskPresets.EMPTY);
        stack.set(ModDataComponents.SLOT_MASKS, presets.withMask(selected, mask));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> new dev.celli.enderutilities.menu.InventorySwapperMenu(id, inv, hand),
                    Component.translatable("enderutilities.container.inventoryswapper")
            ), buf -> buf.writeEnum(hand));
        }
        return ItemEnderUtilities.handledWithoutUseAnim(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            IItemHandler handler = InventoryHelper.inventoryAt(context.getLevel(), context.getClickedPos(), context.getClickedFace());
            if (handler != null) {
                if (!context.getLevel().isClientSide) {
                    swapInventories(new PlayerMainInvWrapper(player.getInventory()), handler, getEnabledSlotsMask(context.getItemInHand()));
                    context.getLevel().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 0.3F, 1.6F);
                }
                return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
            }
        }
        return super.useOn(context);
    }

    public static void swapWithCard(Player player, ItemStack swapper) {
        ItemStack card = ModularHelper.getSelectedModuleStack(swapper, ModuleType.MEMORY_CARD_ITEMS);
        if (card.isEmpty()) {
            return;
        }
        ItemInventoryData stored = card.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
        ItemInventoryData playerData = ItemInventoryData.EMPTY;
        long mask = getEnabledSlotsMask(swapper);
        if (mask == 0L) {
            mask = ~0L;
        }
        for (int i = 0; i <= 40; i++) {
            if ((mask & (1L << i)) == 0) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack != swapper) {
                playerData = playerData.withSlot(i, stack);
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        for (int i = 0; i <= 40; i++) {
            if ((mask & (1L << i)) == 0) {
                continue;
            }
            ItemStack stack = stored.stackInSlot(i);
            if (!stack.isEmpty()) {
                player.getInventory().setItem(i, stack);
            }
        }
        if (dev.celli.enderutilities.compat.ModCompat.curiosLoaded()) {
            playerData = swapCurios(player, stored, playerData, mask);
        }
        card.set(ModDataComponents.ITEM_INVENTORY, playerData);
        ModularHelper.setSelectedModuleStack(swapper, ModuleType.MEMORY_CARD_ITEMS, card);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 0.3F, 1.6F);
    }

    private static ItemInventoryData swapCurios(
            Player player,
            ItemInventoryData stored,
            ItemInventoryData playerData,
            long mask
    ) {
        IItemHandler curios = dev.celli.enderutilities.compat.curios.CuriosAccess.playerCurios(player);
        int count = Math.min(dev.celli.enderutilities.compat.curios.CuriosAccess.BAUBLE_SLOT_COUNT, curios.getSlots());
        for (int i = 0; i < count; i++) {
            int swapperSlot = 41 + i;
            if ((mask & (1L << swapperSlot)) == 0) {
                continue;
            }
            ItemStack fromCard = stored.stackInSlot(swapperSlot);
            ItemStack fromPlayer = curios.extractItem(i, 64, true);
            if (fromCard.isEmpty() && fromPlayer.isEmpty()) {
                continue;
            }
            fromPlayer = fromPlayer.isEmpty() ? ItemStack.EMPTY : curios.extractItem(i, fromPlayer.getCount(), false);
            if (!fromCard.isEmpty()) {
                ItemStack leftover = curios.insertItem(i, fromCard, false);
                if (!leftover.isEmpty()) {
                    if (!fromPlayer.isEmpty()) {
                        curios.insertItem(i, fromPlayer, false);
                    }
                    continue;
                }
            }
            if (!fromPlayer.isEmpty()) {
                playerData = playerData.withSlot(swapperSlot, fromPlayer);
            }
        }
        return playerData;
    }

    public static void swapInventories(IItemHandler a, IItemHandler b) {
        swapInventories(a, b, ~0L);
    }

    public static void swapInventories(IItemHandler a, IItemHandler b, long mask) {
        int slots = Math.min(Math.min(a.getSlots(), b.getSlots()), 36);
        long used = mask == 0L ? ~0L : mask;
        for (int i = 0; i < slots; i++) {
            if ((used & (1L << i)) == 0) {
                continue;
            }
            ItemStack fromA = a.extractItem(i, 64, false);
            ItemStack fromB = b.extractItem(i, 64, false);
            a.insertItem(i, fromB, false);
            b.insertItem(i, fromA, false);
        }
    }

    @Override
    public boolean doUnselectedKeyAction(Player player, ItemStack stack, int key) {
        ItemStack enabled = getEnabledItem(player);
        return !enabled.isEmpty() && ((ItemInventorySwapper) enabled.getItem()).doKeyBindingAction(player, enabled, key);
    }

    public static boolean cycleMode(ItemStack stack) {
        return (stack.getOrDefault(ModDataComponents.ITEM_MODE, 0) & 1) != 0;
    }

    public static void cyclePreset(ItemStack stack, boolean reverse) {
        ItemInventoryData data = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
        int selected = data.selected();
        if (reverse) {
            selected--;
            if (selected < 0) {
                selected = NUM_PRESETS - 1;
            }
        } else {
            selected++;
            if (selected >= NUM_PRESETS) {
                selected = 0;
            }
        }
        stack.set(ModDataComponents.ITEM_INVENTORY, data.withSelected(selected));
    }

    private boolean cycleInventory(Player player, boolean reverse) {
        ItemStack enabled = getEnabledItem(player);
        if (enabled.isEmpty()) {
            return false;
        }
        if (ModularHelper.getInstalledModuleCount(enabled, ModuleType.MEMORY_CARD_ITEMS) <= 1) {
            return false;
        }
        swapWithCard(player, enabled);
        ModularHelper.changeSelectedModule(enabled, ModuleType.MEMORY_CARD_ITEMS, reverse);
        swapWithCard(player, enabled);
        return true;
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE)) {
            swapWithCard(player, stack);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_ALT)) {
            ModularHelper.setData(stack, ModularHelper.data(stack).withBindLocked(!ModularHelper.data(stack).bindLocked()));
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT)) {
            cyclePreset(stack, EnumKey.keypressActionIsReversed(key));
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_CTRL_ALT)) {
            stack.set(ModDataComponents.ITEM_MODE, stack.getOrDefault(ModDataComponents.ITEM_MODE, 0) ^ 1);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL)) {
            boolean reverse = EnumKey.keypressActionIsReversed(key) || EnumKey.keypressContainsShift(key);
            if (cycleMode(stack)) {
                return cycleInventory(player, reverse);
            }
            return ModularHelper.changeSelectedModule(stack, ModuleType.MEMORY_CARD_ITEMS, reverse);
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_ALT)) {
            if (cycleMode(stack)) {
                return cycleInventory(player, true);
            }
            return ModularHelper.changeSelectedModule(stack, ModuleType.MEMORY_CARD_ITEMS, true);
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enderutilities.tooltip.item.enabled")
                .append(": ")
                .append(Component.translatable(isEnabled(stack) ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")));
        tooltip.add(Component.translatable("enderutilities.tooltip.item.cyclemode")
                .append(": ")
                .append(Component.translatable(cycleMode(stack) ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")));
        int preset = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).selected();
        tooltip.add(Component.translatable("enderutilities.tooltip.item.preset")
                .append(": ")
                .append(Component.literal(String.valueOf(preset + 1))));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 5;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType type) {
        return switch (type) {
            case MEMORY_CARD_ITEMS -> 4;
            case ENDERCAPACITOR -> 1;
            default -> 0;
        };
    }

    @Override
    public int getMaxModules(ItemStack container, ItemStack moduleStack) {
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IModule module)) {
            return 0;
        }
        return getMaxModules(container, module.getModuleType(moduleStack));
    }
}
