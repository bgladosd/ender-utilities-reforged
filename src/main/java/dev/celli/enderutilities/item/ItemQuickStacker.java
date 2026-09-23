package dev.celli.enderutilities.item;

import java.util.List;
import java.util.function.Predicate;

import dev.celli.enderutilities.data.ItemInventoryData;
import dev.celli.enderutilities.data.SlotMaskPresets;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.item.module.IKeyBoundUnselected;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemQuickStacker extends ItemEnderUtilities implements IKeyBound, IKeyBoundUnselected {
    public static final int NUM_PRESETS = 4;
    public static final int MAX_RANGE_HORIZONTAL = 4;
    public static final int MAX_RANGE_VERTICAL = 3;
    public static final int GUI_ACTION_CHANGE_PRESET = 0;
    public static final int GUI_ACTION_TOGGLE_ROWS = 1;
    public static final int GUI_ACTION_TOGGLE_COLUMNS = 2;

    public ItemQuickStacker(Properties properties) {
        super(properties.stacksTo(1).component(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY));
    }

    public static boolean isEnabled(ItemStack stack) {
        return !stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).disabled();
    }

    public static ItemStack getEnabledItem(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ItemQuickStacker && isEnabled(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new dev.celli.enderutilities.menu.QuickStackerMenu(id, inv, hand),
                    Component.translatable("enderutilities.container.quickstacker")
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
                    quickStackItems(player, context.getItemInHand(), handler, !player.isShiftKeyDown());
                }
                return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
            }
        }
        return super.useOn(context);
    }

    public static long getEnabledSlotsMask(ItemStack stack) {
        int selected = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).selected();
        return stack.getOrDefault(ModDataComponents.SLOT_MASKS, SlotMaskPresets.EMPTY).mask(selected);
    }

    public static void setEnabledSlotsMask(ItemStack stack, long mask) {
        int selected = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).selected();
        SlotMaskPresets presets = stack.getOrDefault(ModDataComponents.SLOT_MASKS, SlotMaskPresets.EMPTY);
        stack.set(ModDataComponents.SLOT_MASKS, presets.withMask(selected, mask));
    }

    public static void quickStackItems(Player player, ItemStack stacker, IItemHandler inventory, boolean matchingOnly) {
        quickStackMasked(player, inventory, getEnabledSlotsMask(stacker), matchingOnly, stack -> true);
    }

    public static void quickStackMasked(Player player, IItemHandler inventory, long slotMask, boolean matchingOnly,
            Predicate<ItemStack> filter) {
        Inventory inv = player.getInventory();
        boolean moved = false;
        for (int slot = 0; slot <= Inventory.SLOT_OFFHAND; slot++) {
            if ((slotMask & (1L << slot)) == 0) {
                continue;
            }
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !filter.test(stack)) {
                continue;
            }
            ItemStack leftover = InventoryHelper.insertMatchingOrAny(inventory, stack, matchingOnly, true);
            int insert = stack.getCount() - leftover.getCount();
            if (insert > 0) {
                ItemStack taken = stack.split(insert);
                inv.setItem(slot, stack);
                InventoryHelper.insert(inventory, taken, false);
                moved = true;
            }
        }
        if (moved) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 0.2F, 1.8F);
        }
    }

    public static void quickStackItems(Player player) {
        ItemStack stack = getEnabledItem(player);
        if (stack.isEmpty()) {
            return;
        }
        BlockPos origin = player.blockPosition();
        for (int dy = -MAX_RANGE_VERTICAL; dy <= MAX_RANGE_VERTICAL; dy++) {
            for (int dx = -MAX_RANGE_HORIZONTAL; dx <= MAX_RANGE_HORIZONTAL; dx++) {
                for (int dz = -MAX_RANGE_HORIZONTAL; dz <= MAX_RANGE_HORIZONTAL; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    IItemHandler handler = InventoryHelper.inventoryAt(player.level(), pos, null);
                    if (handler != null) {
                        quickStackItems(player, stack, handler, !player.isShiftKeyDown());
                    }
                }
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        int selected = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).selected() + 1;
        return Component.literal(super.getName(stack).getString() + " P: " + selected);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enderutilities.tooltip.item.enabled")
                .append(": ")
                .append(Component.translatable(isEnabled(stack) ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")));
        tooltip.add(Component.translatable("enderutilities.tooltip.item.preset")
                .append(": ")
                .append(Component.literal(String.valueOf(dataSelected(stack) + 1))));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private static int dataSelected(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).selected();
    }

    @Override
    public boolean doUnselectedKeyAction(Player player, ItemStack stack, int key) {
        ItemStack enabled = getEnabledItem(player);
        return !enabled.isEmpty() && ((ItemQuickStacker) enabled.getItem()).doKeyBindingAction(player, enabled, key);
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE, HotKeys.MOD_SHIFT)) {
            quickStackItems(player);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_ALT)) {
            ItemInventoryData data = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
            stack.set(ModDataComponents.ITEM_INVENTORY, data.withDisabled(!data.disabled()));
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL)) {
            ItemInventoryData data = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
            int selected = data.selected() + (EnumKey.keypressActionIsReversed(key) || EnumKey.keypressContainsShift(key) ? -1 : 1);
            if (selected < 0) {
                selected = NUM_PRESETS - 1;
            }
            if (selected >= NUM_PRESETS) {
                selected = 0;
            }
            stack.set(ModDataComponents.ITEM_INVENTORY, data.withSelected(selected));
            return true;
        }
        return false;
    }

    public static void performGuiAction(Player player, ItemStack stack, int action, int element) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemQuickStacker)) {
            return;
        }
        if (action == GUI_ACTION_CHANGE_PRESET && element >= 0 && element < NUM_PRESETS) {
            ItemInventoryData data = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
            stack.set(ModDataComponents.ITEM_INVENTORY, data.withSelected(element));
        } else if (action == GUI_ACTION_TOGGLE_ROWS && element >= 0 && element < 4) {
            setEnabledSlotsMask(stack, getEnabledSlotsMask(stack) ^ (0x1FFL << (element * 9)));
        } else if (action == GUI_ACTION_TOGGLE_COLUMNS && element >= 0 && element < 9) {
            setEnabledSlotsMask(stack, getEnabledSlotsMask(stack) ^ (0x08040201L << element));
        }
    }
}
