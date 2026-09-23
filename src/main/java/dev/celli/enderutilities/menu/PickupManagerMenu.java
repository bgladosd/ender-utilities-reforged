package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.inventory.InstalledToolModulesHandler;
import dev.celli.enderutilities.inventory.ItemStackBackedHandler;
import dev.celli.enderutilities.inventory.ModularModuleHandler;
import dev.celli.enderutilities.item.ItemPickupManager;
import dev.celli.enderutilities.item.module.IModular;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

public class PickupManagerMenu extends AbstractContainerMenu implements ItemGuiActionMenu {
    public static final int FILTER_SLOTS = 36;
    public static final int LINK_CRYSTALS = 3;
    private final InteractionHand hand;
    private final Player player;

    public PickupManagerMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.PICKUP_MANAGER.get(), id);
        this.hand = hand;
        this.player = inventory.player;
        ItemStackBackedHandler transmit = new ItemStackBackedHandler(
                this::container, this::write, 1, 1024);
        addSlot(new SlotItemHandler(transmit, 0, 89, 29));
        IItemHandlerModifiable filters = new FilterHandler();
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(filters, row * 9 + col, 8 + col * 18, 47 + row * 18));
            }
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(filters, 18 + row * 9 + col, 8 + col * 18, 123 + row * 18));
            }
        }
        ItemStack manager = inventory.player.getItemInHand(hand);
        if (manager.getItem() instanceof ItemPickupManager) {
            ItemPickupManager.migrateLegacyCrystalSlots(manager);
        }
        int firstCrystal = manager.getItem() instanceof IModular modular
                ? InstalledToolModulesHandler.firstSlotOfType(modular, manager, ModuleType.LINKCRYSTAL)
                : 1;
        ModularModuleHandler modules = new ModularModuleHandler(
                this::container, this::write, ModuleType.LINKCRYSTAL, firstCrystal, LINK_CRYSTALS);
        for (int i = 0; i < LINK_CRYSTALS; i++) {
            addSlot(new SlotItemHandler(modules, i, 116 + i * 18, 29));
        }
        addPlayerSlots(inventory, 8, 174);
    }

    public PickupManagerMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, EnderMenu.readMenuHand(buf));
    }

    public ItemStack container() {
        return player.getItemInHand(hand);
    }

    private void write(ItemStack stack) {
        player.setItemInHand(hand, stack);
    }

    public int selectedPreset() {
        return ItemPickupManager.selectedPreset(container());
    }

    public int selectedModule() {
        return ModularHelper.data(container()).selectedLinkCrystal();
    }

    private void addPlayerSlots(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, x + col * 18, y + 58));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot.container == player.getInventory() && slot.getItem() == player.getItemInHand(hand)) {
                return;
            }
        }
        if (slotId >= 1 && slotId < 1 + FILTER_SLOTS && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
            ItemStack carried = getCarried();
            slots.get(slotId).set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            return;
        }
        super.clicked(slotId, button, clickType, player);
        if (!player.level().isClientSide) {
            ItemPickupManager.tryTransportItemsFromTransportSlot(player, container());
        }
    }

    @Override
    public void performItemGuiAction(Player player, int action, int element) {
        ItemStack stack = container();
        if (!(stack.getItem() instanceof ItemPickupManager)) {
            return;
        }
        if (action == ItemPickupManager.GUI_ACTION_SELECT_MODULE && element >= 0 && element < LINK_CRYSTALS) {
            ModularHelper.setData(stack, ModularHelper.data(stack).withSelectedLinkCrystal(element));
            ItemPickupManager.tryTransportItemsFromTransportSlot(player, stack);
        } else if (action == ItemPickupManager.GUI_ACTION_CHANGE_PRESET && element >= 0 && element < ItemPickupManager.NUM_PRESETS) {
            ItemPickupManager.setSelectedPreset(stack, element);
        } else if (action == ItemPickupManager.GUI_ACTION_TOGGLE_INVENTORY_SETTINGS && element >= 0 && element < 4) {
            ItemPickupManager.toggleSetting(stack, 4 + element);
        } else if (action == ItemPickupManager.GUI_ACTION_TOGGLE_TRANSPORT_SETTINGS && element >= 0 && element < 4) {
            ItemPickupManager.toggleSetting(stack, element);
        }
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= 1 && index < 1 + FILTER_SLOTS) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int customEnd = 1 + FILTER_SLOTS + LINK_CRYSTALS;
            if (index < customEnd) {
                if (!moveItemStackTo(stack, customEnd, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 1, false) && !moveItemStackTo(stack, 1 + FILTER_SLOTS, customEnd, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof ItemPickupManager;
    }

    private final class FilterHandler implements IItemHandlerModifiable {
        private int offset() {
            return 1 + selectedPreset() * FILTER_SLOTS;
        }

        private ItemStackBackedHandler backing() {
            return new ItemStackBackedHandler(PickupManagerMenu.this::container, PickupManagerMenu.this::write,
                    1 + ItemPickupManager.NUM_PRESETS * FILTER_SLOTS, 1);
        }

        @Override
        public int getSlots() {
            return FILTER_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return backing().getStackInSlot(offset() + slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return backing().insertItem(offset() + slot, stack.copyWithCount(1), simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return backing().extractItem(offset() + slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            backing().setStackInSlot(offset() + slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
    }
}
