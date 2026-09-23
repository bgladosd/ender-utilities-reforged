package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.MemoryChestBlockEntity;
import dev.celli.enderutilities.inventory.LockableItemStackHandler;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MemoryChestMenu extends EnderMenu {
    private final MemoryChestBlockEntity chest;
    private final int machineSlots;
    private final ContainerData data;

    public MemoryChestMenu(int id, Inventory playerInventory, MemoryChestBlockEntity chest) {
        this(id, playerInventory, chest, chest.getData());
    }

    public MemoryChestMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, MemoryChestBlockEntity.class),
                new SimpleContainerData(1));
    }

    private MemoryChestMenu(int id, Inventory playerInventory, MemoryChestBlockEntity chest, ContainerData data) {
        super(ModMenus.MEMORY_CHEST.get(), id);
        this.chest = chest;
        this.data = data;
        this.machineSlots = MemoryChestBlockEntity.INV_SIZES[chest.getStorageTier()];
        chest.getLockable().ensureSize(machineSlots);
        int x = 8;
        int y = 26;
        for (int slot = 0; slot < machineSlots; slot++) {
            addSlot(new SlotItemHandler(chest.getLockable(), slot, x, y));
            x += 18;
            if (slot % 9 == 8) {
                x = 8;
                y += 18;
            }
        }
        int lastY = slots.get(slots.size() - 1).y;
        addPlayerSlots(playerInventory, 8, lastY + 32);
        addDataSlots(data);
    }

    public MemoryChestBlockEntity getChest() {
        return chest;
    }

    public int getMachineSlots() {
        return machineSlots;
    }

    public boolean isPublic() {
        return data.get(0) != 0;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean middleDrag = clickType == ClickType.QUICK_CRAFT && button == 9;
        if ((clickType == ClickType.CLONE || middleDrag) && slotId >= 0 && slotId < machineSlots) {
            Slot slot = slots.get(slotId);
            chest.getLockable().toggleLockFromClick(slot.getContainerSlot(), getCarried(), slot.getItem());
            chest.syncToClient();
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= machineSlots) {
            ItemStack current = slots.get(index).getItem();
            if (!current.isEmpty()) {
                LockableItemStackHandler inv = chest.getLockable();
                for (int i = 0; i < machineSlots; i++) {
                    if (inv.isSlotLocked(i) && moveItemStackTo(current, i, i + 1, false) && current.isEmpty()) {
                        slots.get(index).setByPlayer(ItemStack.EMPTY);
                        slots.get(index).setChanged();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        return quickMoveMachine(player, index, machineSlots);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(chest, player);
    }
}
