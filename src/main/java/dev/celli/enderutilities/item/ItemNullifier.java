package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.data.ItemInventoryData;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

public class ItemNullifier extends ItemEnderUtilities implements IKeyBound {
    public static final int NUM_SLOTS = 9;
    public static final int MAX_STACK_SIZE = 1024;
    public static final int GUI_ACTION_SELECT_SLOT = 0;
    public static final int GUI_ACTION_TOGGLE_DISABLED = 1;

    public ItemNullifier(Properties properties) {
        super(properties.stacksTo(1).component(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY));
    }

    public static ItemInventoryData data(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
    }

    public static boolean isEnabled(ItemStack stack) {
        return !data(stack).disabled();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        ItemStack useStack = extractSelected(stack, player == null || player.getAbilities().instabuild || context.getLevel().isClientSide);
        if (!useStack.isEmpty() && useStack.getItem() instanceof BlockItem && player != null) {
            ItemStack held = player.getItemInHand(context.getHand());
            player.setItemInHand(context.getHand(), useStack);
            InteractionResult result = useStack.useOn(context);
            ItemStack leftover = player.getItemInHand(context.getHand());
            player.setItemInHand(context.getHand(), held);
            if (!context.getLevel().isClientSide && !player.getAbilities().instabuild && !leftover.isEmpty()) {
                tryInsert(stack, leftover);
            }
            return result;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new dev.celli.enderutilities.menu.NullifierMenu(id, inv, hand),
                    Component.translatable("enderutilities.container.nullifier")
            ), buf -> buf.writeEnum(hand));
        }
        return ItemEnderUtilities.handledWithoutUseAnim(stack, level.isClientSide);
    }

    public static ItemStack extractSelected(ItemStack stack, boolean simulate) {
        ItemInventoryData current = data(stack);
        int slot = Math.max(0, Math.min(current.selected(), NUM_SLOTS - 1));
        ItemStack inSlot = current.stackInSlot(slot);
        if (inSlot.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = inSlot.copyWithCount(1);
        if (!simulate) {
            inSlot.shrink(1);
            stack.set(ModDataComponents.ITEM_INVENTORY, current.withSlot(slot, inSlot));
        }
        return taken;
    }

    public static ItemStack tryInsert(ItemStack nullifier, ItemStack incoming) {
        if (incoming.isEmpty() || !isEnabled(nullifier)) {
            return incoming;
        }
        ItemInventoryData current = data(nullifier);
        for (int slot = 0; slot < NUM_SLOTS && !incoming.isEmpty(); slot++) {
            ItemStack existing = current.stackInSlot(slot);
            if (existing.isEmpty()) {
                continue;
            }
            if (ItemStack.isSameItemSameComponents(existing, incoming)) {
                int space = MAX_STACK_SIZE - existing.getCount();
                if (space > 0) {
                    int move = Math.min(space, incoming.getCount());
                    existing.grow(move);
                    incoming.shrink(move);
                    current = current.withSlot(slot, existing);
                } else {
                    incoming = ItemStack.EMPTY;
                }
            }
        }
        nullifier.set(ModDataComponents.ITEM_INVENTORY, current);
        return incoming;
    }

    public static boolean handlePickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        ItemStack stack = entity.getItem();
        if (player.level().isClientSide || entity.isRemoved() || stack.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (inv.getItem() instanceof ItemNullifier && isEnabled(inv)) {
                int before = stack.getCount();
                stack = tryInsert(inv, stack);
                if (stack.getCount() != before) {
                    changed = true;
                }
                if (stack.isEmpty()) {
                    entity.discard();
                    event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
                    player.take(entity, before);
                    return true;
                }
            }
        }
        if (changed) {
            entity.setItem(stack);
        }
        return false;
    }

    public static boolean handleStacks(Player player, java.util.List<ItemStack> drops) {
        boolean changed = false;
        var it = drops.iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (stack.isEmpty()) {
                it.remove();
                continue;
            }
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack inv = player.getInventory().getItem(i);
                if (inv.getItem() instanceof ItemNullifier && isEnabled(inv)) {
                    int before = stack.getCount();
                    ItemStack leftover = tryInsert(inv, stack);
                    if (leftover.isEmpty()) {
                        it.remove();
                        changed = true;
                        break;
                    }
                    if (leftover.getCount() != before) {
                        stack.setCount(leftover.getCount());
                        changed = true;
                    }
                }
            }
        }
        return changed && drops.isEmpty();
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemInventoryData current = data(stack);
        ItemStack selected = current.stackInSlot(current.selected());
        if (!selected.isEmpty()) {
            return Component.literal(super.getName(stack).getString() + " - " + selected.getHoverName().getString());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enderutilities.tooltip.item.enabled")
                .append(": ")
                .append(Component.translatable(isEnabled(stack) ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE)) {
            use(player.level(), player, player.getUsedItemHand() == InteractionHand.OFF_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!player.level().isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new dev.celli.enderutilities.menu.NullifierMenu(id, inv, InteractionHand.MAIN_HAND),
                        Component.translatable("enderutilities.container.nullifier")
                ), buf -> buf.writeEnum(InteractionHand.MAIN_HAND));
            }
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT)) {
            ItemInventoryData current = data(stack);
            stack.set(ModDataComponents.ITEM_INVENTORY, current.withDisabled(!current.disabled()));
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL) || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL)
                || EnumKey.SCROLL.matches(key, HotKeys.MOD_NONE)) {
            ItemInventoryData current = data(stack);
            int selected = current.selected() + (EnumKey.keypressActionIsReversed(key) || EnumKey.keypressContainsShift(key) ? -1 : 1);
            if (selected < 0) {
                selected = NUM_SLOTS - 1;
            }
            if (selected >= NUM_SLOTS) {
                selected = 0;
            }
            stack.set(ModDataComponents.ITEM_INVENTORY, current.withSelected(selected));
            return true;
        }
        return false;
    }
}
