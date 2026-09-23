package dev.celli.enderutilities.menu;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.blockentity.CreationStationBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CreationStationMenu extends EnderMenu {
    private static final int[] ACTION_BUTTON_X = {41, 59, 77, 149, 167, 185};
    private static final int[] CRAFT_MODE_X = {44, 57, 70, 186, 173, 160};

    private final CreationStationBlockEntity station;
    private final ContainerData data;

    public CreationStationMenu(int id, Inventory playerInventory, CreationStationBlockEntity station) {
        this(id, playerInventory, station, station.getData());
    }

    public CreationStationMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, CreationStationBlockEntity.class),
                new SimpleContainerData(5));
    }

    private CreationStationMenu(int id, Inventory playerInventory, CreationStationBlockEntity station, ContainerData data) {
        super(ModMenus.CREATION_STATION.get(), id);
        this.station = station;
        this.data = data;
        station.refreshCraftResults();
        for (int i = 0; i < CreationStationBlockEntity.STORAGE_SLOTS; i++) {
            addSlot(new TransferableItemHandlerSlot(station.getInventory(), CreationStationBlockEntity.SLOT_STORAGE + i,
                    40 + (i % 9) * 18, 102 + (i / 9) * 18));
        }
        for (int i = 0; i < CreationStationBlockEntity.MODULE_SLOTS; i++) {
            addSlot(new SlotItemHandler(station.getInventory(), CreationStationBlockEntity.SLOT_MODULES + i, 216, 102 + i * 18));
        }
        for (int i = 0; i < CreationStationBlockEntity.CRAFT_SLOTS; i++) {
            addSlot(new TransferableItemHandlerSlot(station.getInventory(), CreationStationBlockEntity.SLOT_CRAFT_LEFT + i,
                    40 + (i % 3) * 18, 33 + (i / 3) * 18));
        }
        addSlot(new CraftResultSlot(station, 0, 112, 33));
        for (int i = 0; i < CreationStationBlockEntity.CRAFT_SLOTS; i++) {
            addSlot(new TransferableItemHandlerSlot(station.getInventory(), CreationStationBlockEntity.SLOT_CRAFT_RIGHT + i,
                    148 + (i % 3) * 18, 33 + (i / 3) * 18));
        }
        addSlot(new CraftResultSlot(station, 1, 112, 69));
        addSlot(new SlotItemHandler(station.getInventory(), CreationStationBlockEntity.SLOT_FURNACE, 8, 8));
        addSlot(new SlotItemHandler(station.getInventory(), CreationStationBlockEntity.SLOT_FURNACE + 1, 8, 51));
        addSlot(new SlotItemHandler(station.getInventory(), CreationStationBlockEntity.SLOT_FURNACE + 2, 40, 8));
        addSlot(new SlotItemHandler(station.getInventory(), CreationStationBlockEntity.SLOT_FURNACE + 3, 216, 8));
        addSlot(new SlotItemHandler(station.getInventory(), CreationStationBlockEntity.SLOT_FURNACE + 4, 216, 51));
        addSlot(new SlotItemHandler(station.getInventory(), CreationStationBlockEntity.SLOT_FURNACE + 5, 184, 8));
        addPlayerSlots(playerInventory, 40, 174);
        addDataSlots(data);
    }

    public CreationStationBlockEntity getStation() {
        return station;
    }

    public int getModeMask() {
        return data.get(0);
    }

    public int getSelectedModuleSlot() {
        return data.get(1) & 0x3;
    }

    public int getQuickMode() {
        return (data.get(1) >> 2) & 0x7;
    }

    public int getFuelProgress(int furnace) {
        int packed = data.get(2);
        return furnace == 1 ? (packed >> 8) & 0x7F : packed & 0x7F;
    }

    public int getSmeltProgress(int furnace) {
        int packed = data.get(3);
        return furnace == 1 ? (packed >> 8) & 0x7F : packed & 0xFF;
    }

    public int getLastInteractedCraftingGrid() {
        return data.get(4);
    }

    public int getCraftingGridStart() {
        return getLastInteractedCraftingGrid() == 1 ? 41 : 31;
    }

    public int getCraftingResultIndex() {
        return getLastInteractedCraftingGrid() == 1 ? 50 : 40;
    }

    public int getPlayerInventoryStart() {
        return 57;
    }

    public List<Slot> recipeTransferCraftingSlots() {
        int start = getCraftingGridStart();
        List<Slot> slots = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            slots.add(getSlot(start + i));
        }
        return slots;
    }

    public List<Slot> recipeTransferInventorySlots() {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            slots.add(getSlot(i));
        }
        int playerStart = getPlayerInventoryStart();
        for (int i = playerStart; i < this.slots.size(); i++) {
            slots.add(getSlot(i));
        }
        return slots;
    }

    public static int[] actionButtonX() {
        return ACTION_BUTTON_X;
    }

    public static int[] craftModeX() {
        return CRAFT_MODE_X;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 31 && slotId <= 40) {
            station.lastInteractedCraftingGrid = 0;
            data.set(4, 0);
            station.setChanged();
        } else if (slotId >= 41 && slotId <= 50) {
            station.lastInteractedCraftingGrid = 1;
            data.set(4, 1);
            station.setChanged();
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, CreationStationBlockEntity.TOTAL);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(station, player);
    }

    /** Empty {@link SlotItemHandler}s fail vanilla {@code allowModification}; REI requires it. */
    private static final class TransferableItemHandlerSlot extends SlotItemHandler {
        private TransferableItemHandlerSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.isEmpty() || super.mayPlace(stack);
        }

        @Override
        public boolean allowModification(Player player) {
            return getItem().isEmpty() || super.allowModification(player);
        }
    }

    private static final class CraftResultSlot extends SlotItemHandler {
        private final CreationStationBlockEntity station;
        private final int grid;

        private CraftResultSlot(CreationStationBlockEntity station, int grid, int x, int y) {
            super(station.getInventory(), station.resultSlot(grid), x, y);
            this.station = station;
            this.grid = grid;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            station.lastInteractedCraftingGrid = grid;
            station.consumeCraft(grid);
            super.onTake(player, stack);
        }
    }
}
