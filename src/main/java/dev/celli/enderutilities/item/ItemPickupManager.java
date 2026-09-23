package dev.celli.enderutilities.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dev.celli.enderutilities.data.ItemInventoryData;
import dev.celli.enderutilities.data.ModularItemData;
import dev.celli.enderutilities.inventory.ItemStackBackedHandler;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.ChunkLoading;
import dev.celli.enderutilities.util.InventoryHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemPickupManager extends ItemLocationBoundModular {
    public static final int ENDER_CHARGE_COST_PER_SENT_ITEM = 2;
    public static final int NUM_PRESETS = 4;
    public static final int FILTER_SLOTS = 36;
    public static final int LINK_CRYSTALS = 3;
    public static final int TRANSMIT_SLOT = 0;
    public static final int GUI_ACTION_SELECT_MODULE = 0;
    public static final int GUI_ACTION_CHANGE_PRESET = 1;
    public static final int GUI_ACTION_TOGGLE_INVENTORY_SETTINGS = 2;
    public static final int GUI_ACTION_TOGGLE_TRANSPORT_SETTINGS = 3;

    public ItemPickupManager(Properties properties) {
        super(properties.stacksTo(1).component(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY));
    }

    public static boolean isEnabled(ItemStack stack) {
        return !stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).disabled();
    }

    public static void setEnabled(ItemStack stack, boolean enabled) {
        ItemInventoryData data = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
        stack.set(ModDataComponents.ITEM_INVENTORY, data.withDisabled(!enabled));
    }

    public static int selectedPreset(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).selected();
    }

    public static void setSelectedPreset(ItemStack stack, int preset) {
        ItemInventoryData data = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
        stack.set(ModDataComponents.ITEM_INVENTORY, data.withSelected(preset));
    }

    public static int settings(ItemStack stack) {
        int preset = selectedPreset(stack);
        return (stack.getOrDefault(ModDataComponents.ITEM_MODE, 0) >> (preset * 8)) & 0xFF;
    }

    public static void toggleSetting(ItemStack stack, int bit) {
        int preset = selectedPreset(stack);
        int packed = stack.getOrDefault(ModDataComponents.ITEM_MODE, 0);
        stack.set(ModDataComponents.ITEM_MODE, packed ^ (1 << (preset * 8 + bit)));
    }

    public static List<ItemStack> getEnabledItems(Player player) {
        List<ItemStack> enabled = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack manager = player.getInventory().getItem(i);
            if (manager.getItem() instanceof ItemPickupManager) {
                migrateLegacyCrystalSlots(manager);
                if (isEnabled(manager)) {
                    enabled.add(manager);
                }
            }
        }
        return enabled;
    }

    /**
     * The first GUI wrote Link Crystals into modular slots 0-2. Tool Workstation
     * places the capacitor at slot 0 and crystals at 1-3, so move leftover
     * crystals forward when slot 0 is still a crystal.
     */
    public static void migrateLegacyCrystalSlots(ItemStack manager) {
        ModularItemData data = ModularHelper.data(manager);
        ItemStack slot0 = data.stackInSlot(0);
        if (slot0.isEmpty() || !(slot0.getItem() instanceof IModule module)
                || module.getModuleType(slot0) != ModuleType.LINKCRYSTAL) {
            return;
        }
        ModularItemData next = data;
        for (int slot = LINK_CRYSTALS - 1; slot >= 0; slot--) {
            ItemStack crystal = next.stackInSlot(slot);
            if (crystal.isEmpty() || !next.stackInSlot(slot + 1).isEmpty()) {
                continue;
            }
            if (crystal.getItem() instanceof IModule stored
                    && stored.getModuleType(crystal) == ModuleType.LINKCRYSTAL) {
                next = next.withSlot(slot + 1, crystal).withSlot(slot, ItemStack.EMPTY);
            }
        }
        if (next != data) {
            ModularHelper.setData(manager, next);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new dev.celli.enderutilities.menu.PickupManagerMenu(id, inv, hand),
                    Component.translatable("enderutilities.container.pickupmanager")
            ), buf -> buf.writeEnum(hand));
        }
        return ItemEnderUtilities.handledWithoutUseAnim(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (be != null && InventoryHelper.inventoryAt(context.getLevel(), context.getClickedPos(), context.getClickedFace()) != null) {
                return super.useOn(context);
            }
        }
        return super.useOn(context);
    }

    public enum PickupResult {
        TRANSPORTED,
        WHITELISTED,
        NOT_WHITELISTED,
        BLACKLISTED,
        NOT_BLACKLISTED,
        NOT_HANDLED
    }

    public static boolean handlePickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        ItemStack incoming = entity.getItem();
        if (player.level().isClientSide || entity.isRemoved() || incoming.isEmpty()) {
            return false;
        }
        List<ItemStack> managers = getEnabledItems(player);
        if (managers.isEmpty()) {
            return false;
        }
        int origCount = incoming.getCount();
        boolean deny = true;
        for (ItemStack manager : managers) {
            PickupResult result = handleItems(player, manager, incoming);
            if (result == PickupResult.TRANSPORTED || result == PickupResult.BLACKLISTED) {
                deny = true;
                break;
            }
            if (result == PickupResult.WHITELISTED) {
                deny = false;
                break;
            }
            if (result == PickupResult.NOT_HANDLED || result == PickupResult.NOT_BLACKLISTED) {
                deny = false;
            }
        }
        if (incoming.getCount() != origCount || incoming.isEmpty()) {
            playPickupSound(player);
            if (incoming.isEmpty()) {
                entity.discard();
                player.take(entity, origCount);
            }
        }
        if (deny) {
            event.setCanPickup(TriState.FALSE);
        }
        return deny;
    }

    public static boolean handleStacks(Player player, List<ItemStack> drops) {
        List<ItemStack> managers = getEnabledItems(player);
        if (managers.isEmpty()) {
            return false;
        }
        boolean deny = true;
        boolean transported = false;
        Iterator<ItemStack> it = drops.iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (stack.isEmpty()) {
                it.remove();
                continue;
            }
            for (ItemStack manager : managers) {
                PickupResult result = handleItems(player, manager, stack);
                if (result == PickupResult.TRANSPORTED) {
                    if (stack.isEmpty()) {
                        it.remove();
                    }
                    transported = true;
                    deny = true;
                    break;
                }
                if (result == PickupResult.BLACKLISTED) {
                    deny = true;
                    break;
                }
                if (result == PickupResult.WHITELISTED) {
                    deny = false;
                    break;
                }
                if (result == PickupResult.NOT_HANDLED || result == PickupResult.NOT_BLACKLISTED) {
                    deny = false;
                }
            }
        }
        if (transported) {
            playPickupSound(player);
        }
        return deny;
    }

    private static void playPickupSound(Player player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 0.2F,
                ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
    }

    public static PickupResult handleItems(Player player, ItemStack manager, ItemStack incoming) {
        if ((settings(manager) & 0x1) != 0 && itemAllowed(manager, incoming, true)) {
            ItemStack leftover = tryTransport(player, manager, incoming.copy());
            if (leftover.isEmpty()) {
                incoming.setCount(0);
                return PickupResult.TRANSPORTED;
            }
            incoming.setCount(leftover.getCount());
        }
        int inv = (settings(manager) >> 4) & 0xF;
        if ((inv & 0x1) == 0) {
            return PickupResult.NOT_HANDLED;
        }
        boolean match = itemMatches(manager, incoming, false);
        boolean whitelist = (inv & 0x2) != 0;
        if (whitelist) {
            return match ? PickupResult.WHITELISTED : PickupResult.NOT_WHITELISTED;
        }
        return match ? PickupResult.BLACKLISTED : PickupResult.NOT_BLACKLISTED;
    }

    public static boolean tryTransportItemsFromTransportSlot(Player player, ItemStack manager) {
        if (player.level().isClientSide || !(manager.getItem() instanceof ItemPickupManager)) {
            return false;
        }
        ItemStackBackedHandler transmit = new ItemStackBackedHandler(
                () -> manager,
                updated -> {
                },
                1 + NUM_PRESETS * FILTER_SLOTS,
                1024
        );
        ItemStack stack = transmit.getStackInSlot(TRANSMIT_SLOT);
        if (stack.isEmpty()) {
            return false;
        }
        int sizeOrig = stack.getCount();
        int max = Math.max(1, stack.getMaxStackSize());
        stack = transmit.extractItem(TRANSMIT_SLOT, max, false);
        while (!stack.isEmpty()) {
            stack = tryTransport(player, manager, stack);
            if (!stack.isEmpty()) {
                transmit.insertItem(TRANSMIT_SLOT, stack, false);
                break;
            }
            stack = transmit.extractItem(TRANSMIT_SLOT, max, false);
        }
        ItemStack remaining = transmit.getStackInSlot(TRANSMIT_SLOT);
        return remaining.isEmpty() || remaining.getCount() != sizeOrig;
    }

    static ItemStack tryTransport(Player player, ItemStack manager, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return incoming;
        }
        IItemHandler handler = ModularHelper.getBoundInventory(manager, player, ChunkLoading.TIMEOUT_TOOLS);
        if (handler == null) {
            return incoming;
        }
        int cost = ENDER_CHARGE_COST_PER_SENT_ITEM;
        ItemStack stackToSend = incoming.copy();
        if (!ModularHelper.useEnderCharge(manager, cost * incoming.getCount(), true)) {
            int available = ModularHelper.getAvailableEnderCharge(manager);
            if (available < cost) {
                return incoming;
            }
            stackToSend.setCount(Math.min(incoming.getCount(), available / cost));
        }
        ItemStack leftover = InventoryHelper.insert(handler, stackToSend, false);
        int moved = stackToSend.getCount() - leftover.getCount();
        if (moved > 0) {
            ModularHelper.useEnderCharge(manager, moved * cost, false);
            incoming.shrink(moved);
        }
        return incoming.isEmpty() ? ItemStack.EMPTY : incoming;
    }

    public static boolean itemAllowed(ItemStack manager, ItemStack incoming, boolean transport) {
        int bits = transport ? settings(manager) & 0xF : (settings(manager) >> 4) & 0xF;
        if ((bits & 0x1) == 0) {
            return true;
        }
        boolean whitelist = (bits & 0x2) != 0;
        return whitelist == itemMatches(manager, incoming, transport);
    }

    public static boolean itemMatches(ItemStack manager, ItemStack incoming, boolean transport) {
        int bits = transport ? settings(manager) & 0xF : (settings(manager) >> 4) & 0xF;
        boolean ignoreMeta = (bits & 0x4) != 0;
        boolean matchNbt = (bits & 0x8) != 0;
        ItemInventoryData data = manager.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
        int start = 1 + selectedPreset(manager) * FILTER_SLOTS + (transport ? 0 : 18);
        for (int i = 0; i < 18; i++) {
            ItemStack filter = data.stackInSlot(start + i);
            if (filter.isEmpty()) {
                continue;
            }
            boolean same;
            if (ignoreMeta) {
                same = filter.getItem() == incoming.getItem();
            } else if (matchNbt) {
                same = ItemStack.isSameItemSameComponents(filter, incoming);
            } else {
                same = ItemStack.isSameItem(filter, incoming);
            }
            if (same) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE)) {
            setEnabled(stack, !isEnabled(stack));
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT)) {
            int next = selectedPreset(stack) + (EnumKey.keypressActionIsReversed(key) ? -1 : 1);
            if (next < 0) {
                next = NUM_PRESETS - 1;
            } else if (next >= NUM_PRESETS) {
                next = 0;
            }
            setSelectedPreset(stack, next);
            return true;
        }
        return super.doKeyBindingAction(player, stack, key);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enderutilities.tooltip.item.enabled")
                .append(": ")
                .append(Component.translatable(isEnabled(stack) ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 4;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType type) {
        return switch (type) {
            case ENDERCAPACITOR -> 1;
            case LINKCRYSTAL -> LINK_CRYSTALS;
            default -> 0;
        };
    }

    @Override
    public int getMaxModules(ItemStack container, ItemStack moduleStack) {
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IModule module)) {
            return 0;
        }
        ModuleType type = module.getModuleType(moduleStack);
        if (type == ModuleType.LINKCRYSTAL && module.getModuleTier(moduleStack) != ItemLinkCrystal.TYPE_BLOCK) {
            return 0;
        }
        return getMaxModules(container, type);
    }
}
