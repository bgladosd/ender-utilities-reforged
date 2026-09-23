package dev.celli.enderutilities.item;

import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.data.ItemInventoryData;
import dev.celli.enderutilities.data.SlotMaskPresets;
import dev.celli.enderutilities.inventory.SelectedModuleInventoryHandler;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.InventoryHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

public class ItemHandyBag extends ItemModular {
    public static final int TIER_1 = 0;
    public static final int TIER_2 = 1;
    public static final int INV_SIZE_TIER_1 = 27;
    public static final int INV_SIZE_TIER_2 = 55;
    public static final int GUI_ACTION_SELECT_MODULE = 0;
    public static final int GUI_ACTION_MOVE_ITEMS = 1;
    public static final int GUI_ACTION_SORT_ITEMS = 2;
    public static final int GUI_ACTION_TOGGLE_BLOCK = 3;
    public static final int GUI_ACTION_TOGGLE_UPDATE = 4;
    public static final int GUI_ACTION_TOGGLE_MODES = 5;
    public static final int GUI_ACTION_TOGGLE_SHIFTCLICK = 6;
    public static final int GUI_ACTION_TOGGLE_SHIFTCLICK_DOUBLETAP = 7;
    public static final int GUI_ACTION_OPEN_CURIOS = 100;
    public static final long[] SECTION_MASKS = {0x1FFFFFFL, 0x1FFF8000000L, 0x7FFE0000000000L};

    private final int tier;

    public ItemHandyBag(Properties properties, int tier) {
        super(properties.stacksTo(1).component(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY));
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    public int inventorySize() {
        return tier == TIER_2 ? INV_SIZE_TIER_2 : INV_SIZE_TIER_1;
    }

    public static boolean isLocked(ItemStack stack) {
        return ModularHelper.data(stack).bindLocked();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!bagIsOpenable(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            int slot = hand == InteractionHand.OFF_HAND ? 40 : player.getInventory().selected;
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new dev.celli.enderutilities.menu.HandyBagMenu(id, inv, slot),
                    Component.translatable(getDescriptionId(stack))
            ), buf -> buf.writeVarInt(slot));
        }
        return ItemEnderUtilities.handledWithoutUseAnim(stack, level.isClientSide);
    }

    public static boolean bagIsOpenable(ItemStack stack) {
        return stack.getItem() instanceof ItemHandyBag && !isLocked(stack);
    }

    public static int findOpenableBagSlot(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (bagIsOpenable(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static ItemStack getOpenableBag(Player player) {
        int slot = findOpenableBagSlot(player);
        return slot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(slot);
    }

    public static ItemStack handleIncoming(Player player, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return incoming;
        }
        var playerInv = InventoryHelper.playerHandler(player);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack bag = player.getInventory().getItem(i);
            if (!(bag.getItem() instanceof ItemHandyBag handy) || isLocked(bag) || handy.getPickupMode(bag) == 0) {
                continue;
            }
            incoming = InventoryHelper.insertIntoExisting(playerInv, incoming);
            if (incoming.isEmpty()) {
                return incoming;
            }
            if (handy.getPickupMode(bag) == 1 && !containsMatching(bag, incoming)) {
                continue;
            }
            incoming = insert(bag, incoming);
            if (incoming.isEmpty()) {
                return incoming;
            }
        }
        return incoming;
    }

    public static boolean handleStacks(Player player, java.util.List<ItemStack> drops) {
        boolean picked = false;
        var it = drops.iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (stack.isEmpty()) {
                it.remove();
                continue;
            }
            int before = stack.getCount();
            ItemStack leftover = handleIncoming(player, stack);
            if (leftover.isEmpty()) {
                it.remove();
                picked = true;
            } else if (leftover.getCount() != before) {
                stack.setCount(leftover.getCount());
                picked = true;
            }
        }
        if (picked) {
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.2F, 1.0F);
        }
        return drops.isEmpty();
    }

    public static boolean handlePickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        var entity = event.getItemEntity();
        ItemStack incoming = entity.getItem();
        if (player.level().isClientSide || entity.isRemoved() || incoming.isEmpty()) {
            return false;
        }
        int before = incoming.getCount();
        ItemStack leftover = handleIncoming(player, incoming);
        if (leftover.getCount() == before && !leftover.isEmpty()) {
            return false;
        }
        if (leftover.isEmpty()) {
            entity.discard();
            event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
            return true;
        }
        entity.setItem(leftover);
        return false;
    }

    public static ItemStack selectedCard(ItemStack bag) {
        return ModularHelper.getSelectedModuleStack(bag, ModuleType.MEMORY_CARD_ITEMS);
    }

    public static void writeSelectedCard(ItemStack bag, ItemStack card) {
        ModularHelper.setSelectedModuleStack(bag, ModuleType.MEMORY_CARD_ITEMS, card);
    }

    public static SelectedModuleInventoryHandler cardInventory(ItemStack bag) {
        ItemHandyBag handy = bag.getItem() instanceof ItemHandyBag item ? item : null;
        int size = handy == null ? INV_SIZE_TIER_1 : handy.inventorySize();
        int max = handy == null ? 64 : handy.maxStackSize(bag);
        return new SelectedModuleInventoryHandler(() -> bag, updated -> {
        }, ModuleType.MEMORY_CARD_ITEMS, size, max);
    }

    public static net.neoforged.neoforge.items.IItemHandlerModifiable unlockedCardInventory(ItemStack bag) {
        SelectedModuleInventoryHandler inner = cardInventory(bag);
        return new net.neoforged.neoforge.items.IItemHandlerModifiable() {
            @Override
            public int getSlots() {
                return inner.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return inner.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return isSlotLocked(bag, slot) ? stack : inner.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return isSlotLocked(bag, slot) ? ItemStack.EMPTY : inner.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return inner.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !isSlotLocked(bag, slot) && inner.isItemValid(slot, stack);
            }

            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                if (!isSlotLocked(bag, slot)) {
                    inner.setStackInSlot(slot, stack);
                }
            }
        };
    }

    public static boolean isSlotLocked(ItemStack bag, int slot) {
        if (!(bag.getItem() instanceof ItemHandyBag handy) || handy.tier() != TIER_2) {
            return false;
        }
        ItemStack card = selectedCard(bag);
        if (card.isEmpty()) {
            return true;
        }
        long lock = card.getOrDefault(ModDataComponents.SLOT_MASKS, SlotMaskPresets.EMPTY).mask(0);
        return (lock & (1L << slot)) != 0;
    }

    public static void toggleSectionMask(ItemStack bag, int section, int maskIndex) {
        if (section < 0 || section > 2) {
            return;
        }
        ItemStack card = selectedCard(bag);
        if (card.isEmpty()) {
            return;
        }
        SlotMaskPresets presets = card.getOrDefault(ModDataComponents.SLOT_MASKS, SlotMaskPresets.EMPTY);
        card.set(ModDataComponents.SLOT_MASKS, presets.withMask(maskIndex, presets.mask(maskIndex) ^ SECTION_MASKS[section]));
        writeSelectedCard(bag, card);
    }

    public static boolean isSectionMasked(ItemStack bag, int section, int maskIndex) {
        ItemStack card = selectedCard(bag);
        if (card.isEmpty() || section < 0 || section > 2) {
            return false;
        }
        long mask = card.getOrDefault(ModDataComponents.SLOT_MASKS, SlotMaskPresets.EMPTY).mask(maskIndex);
        return (mask & SECTION_MASKS[section]) == SECTION_MASKS[section];
    }

    public static int[] sectionRange(int section) {
        return switch (section) {
            case 1 -> new int[] {27, 41};
            case 2 -> new int[] {41, 55};
            default -> new int[] {0, 27};
        };
    }

    public static boolean containsMatching(ItemStack bag, ItemStack match) {
        if (match.isEmpty() || selectedCard(bag).isEmpty()) {
            return false;
        }
        var inv = cardInventory(bag);
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            if (!isSlotLocked(bag, slot) && ItemStack.isSameItemSameComponents(inv.getStackInSlot(slot), match)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack insert(ItemStack bag, ItemStack incoming) {
        if (!(bag.getItem() instanceof ItemHandyBag) || incoming.isEmpty() || selectedCard(bag).isEmpty()) {
            return incoming;
        }
        var inv = cardInventory(bag);
        for (int slot = 0; slot < inv.getSlots() && !incoming.isEmpty(); slot++) {
            if (isSlotLocked(bag, slot)) {
                continue;
            }
            incoming = inv.insertItem(slot, incoming, false);
        }
        return incoming;
    }

    public static ItemStack extractMatching(ItemStack bag, ItemStack match, int amount) {
        if (match.isEmpty() || amount <= 0 || selectedCard(bag).isEmpty()) {
            return ItemStack.EMPTY;
        }
        var inv = cardInventory(bag);
        ItemStack taken = ItemStack.EMPTY;
        for (int slot = 0; slot < inv.getSlots() && amount > 0; slot++) {
            if (isSlotLocked(bag, slot) || !ItemStack.isSameItemSameComponents(inv.getStackInSlot(slot), match)) {
                continue;
            }
            ItemStack extracted = inv.extractItem(slot, amount, false);
            amount -= extracted.getCount();
            if (taken.isEmpty()) {
                taken = extracted;
            } else {
                taken.grow(extracted.getCount());
            }
        }
        return taken;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        updateMaskedItems(stack, level, player, slotId);
        if (getRestockMode(stack) == 0 || (player.tickCount + slotId) % 20 != 0) {
            return;
        }
        restockPlayer(stack, player);
    }

    private void updateMaskedItems(ItemStack bag, Level level, Player player, int bagSlot) {
        ItemStack card = selectedCard(bag);
        if (card.isEmpty()) {
            return;
        }
        long mask = card.getOrDefault(ModDataComponents.SLOT_MASKS, SlotMaskPresets.EMPTY).mask(1);
        if (mask == 0) {
            return;
        }
        int tmpSlot = freeNonHeldSlot(player, bagSlot);
        if (tmpSlot < 0) {
            return;
        }
        ItemStack saved = player.getInventory().getItem(tmpSlot);
        var inv = cardInventory(bag);
        int sections = tier == TIER_2 ? 3 : 1;
        for (int section = 0; section < sections; section++) {
            if ((mask & SECTION_MASKS[section]) == 0) {
                continue;
            }
            int[] range = tier == TIER_2 ? sectionRange(section) : new int[] {0, inventorySize()};
            for (int slot = range[0]; slot < range[1] && slot < inv.getSlots(); slot++) {
                if ((mask & (1L << slot)) == 0) {
                    continue;
                }
                ItemStack current = inv.getStackInSlot(slot);
                if (current.isEmpty()) {
                    continue;
                }
                ItemStack original = current.copy();
                player.getInventory().setItem(tmpSlot, current);
                try {
                    current.inventoryTick(level, player, tmpSlot, false);
                } catch (Throwable t) {
                    EnderUtilities.LOGGER.warn("Exception while updating items inside a Handy Bag", t);
                }
                if (!ItemStack.matches(current, original)) {
                    inv.setStackInSlot(slot, current);
                }
            }
        }
        player.getInventory().setItem(tmpSlot, saved);
    }

    private static int freeNonHeldSlot(Player player, int bagSlot) {
        int selected = player.getInventory().selected;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (i != selected && player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }
        return bagSlot != selected ? bagSlot : -1;
    }

    private void restockPlayer(ItemStack bag, Player player) {
        int end = getRestockMode(bag) == 1 ? 9 : 36;
        for (int i = 0; i < end; i++) {
            ItemStack current = player.getInventory().getItem(i);
            if (current.isEmpty() || current.getCount() >= current.getMaxStackSize()) {
                continue;
            }
            int need = current.getMaxStackSize() - current.getCount();
            ItemStack taken = extractMatching(bag, current, need);
            if (!taken.isEmpty()) {
                current.grow(taken.getCount());
                player.getInventory().setItem(i, current);
            }
        }
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && off.getCount() < off.getMaxStackSize()) {
            ItemStack taken = extractMatching(bag, off, off.getMaxStackSize() - off.getCount());
            if (!taken.isEmpty()) {
                off.grow(taken.getCount());
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack bag = context.getItemInHand();
        if (player == null || getPickupMode(bag) == 0) {
            return InteractionResult.PASS;
        }
        var handler = dev.celli.enderutilities.util.InventoryHelper.inventoryAt(
                context.getLevel(), context.getClickedPos(), context.getClickedFace());
        if (handler == null) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            var bagInv = unlockedCardInventory(bag);
            if (getRestockMode(bag) != 0) {
                if (getRestockMode(bag) == 1) {
                    InventoryHelper.moveMatching(bagInv, handler, 0, bagInv.getSlots());
                } else {
                    InventoryHelper.moveAll(bagInv, handler, 0, bagInv.getSlots());
                }
            } else if (player.isShiftKeyDown() || getPickupMode(bag) == 2) {
                InventoryHelper.moveAll(handler, bagInv, 0, handler.getSlots());
            } else {
                InventoryHelper.moveMatching(handler, bagInv, 0, handler.getSlots());
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    public int maxStackSize(ItemStack bag) {
        int tier = ModularHelper.getSelectedModuleTier(bag, ModuleType.MEMORY_CARD_ITEMS);
        return switch (tier) {
            case 1 -> 256;
            case 2 -> 1024;
            case 3 -> 4096;
            default -> 64;
        };
    }

    public int getPickupMode(ItemStack stack) {
        ItemStack card = selectedCard(stack);
        return card.isEmpty() ? 0 : card.getOrDefault(ModDataComponents.ITEM_MODE, 0) & 0x3;
    }

    public static int getRestockMode(ItemStack stack) {
        ItemStack card = selectedCard(stack);
        return card.isEmpty() ? 0 : (card.getOrDefault(ModDataComponents.ITEM_MODE, 0) >> 2) & 0x3;
    }

    public static int getShiftMode(ItemStack stack) {
        return packedMode(stack) & 0x3;
    }

    public static int getEffectiveShiftMode(ItemStack stack) {
        int raw = packedMode(stack);
        int mode = raw & 0x3;
        if (mode == 2) {
            return (raw & 0x80) != 0 ? 1 : 0;
        }
        return mode;
    }

    public static int packedMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_MODE, 0);
    }

    public static void setPackedMode(ItemStack stack, int mode) {
        stack.set(ModDataComponents.ITEM_MODE, mode);
    }

    public static void cycleCardField(ItemStack bag, int shift, boolean reverse) {
        ItemStack card = selectedCard(bag);
        if (card.isEmpty()) {
            return;
        }
        int packed = card.getOrDefault(ModDataComponents.ITEM_MODE, 0);
        int value = (packed >> shift) & 0x3;
        value = reverse ? (value + 2) % 3 : (value + 1) % 3;
        card.set(ModDataComponents.ITEM_MODE, (packed & ~(0x3 << shift)) | (value << shift));
        writeSelectedCard(bag, card);
    }

    public static int cycleField(int packed, int shift, boolean reverse) {
        int value = (packed >> shift) & 0x3;
        value = reverse ? (value + 2) % 3 : (value + 1) % 3;
        return (packed & ~(0x3 << shift)) | (value << shift);
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE)) {
            cycleCardField(stack, 0, false);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT)) {
            ModularHelper.setData(stack, ModularHelper.data(stack).withBindLocked(!isLocked(stack)));
            return true;
        }
        return super.doKeyBindingAction(player, stack, key);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(isLocked(stack)
                ? "enderutilities.tooltip.item.bindlocked"
                : "enderutilities.tooltip.item.enabled"));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 6;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType type) {
        return switch (type) {
            case ENDERCAPACITOR -> 1;
            case MEMORY_CARD_ITEMS -> 4;
            case MOBPERSISTENCE -> 0;
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
