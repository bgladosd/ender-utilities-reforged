package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.DrawbridgeBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class DrawbridgeMenu extends EnderMenu {
    private final DrawbridgeBlockEntity drawbridge;
    private final ContainerData data;
    private final int machineSlots;

    public DrawbridgeMenu(int id, Inventory playerInventory, DrawbridgeBlockEntity drawbridge) {
        this(id, playerInventory, drawbridge, drawbridge.getData());
    }

    public DrawbridgeMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, DrawbridgeBlockEntity.class), new SimpleContainerData(4));
    }

    private DrawbridgeMenu(int id, Inventory playerInventory, DrawbridgeBlockEntity drawbridge, ContainerData data) {
        super(ModMenus.DRAWBRIDGE.get(), id);
        this.drawbridge = drawbridge;
        this.data = data;
        if (drawbridge.isAdvanced()) {
            this.machineSlots = DrawbridgeBlockEntity.MAX_LENGTH_ADVANCED;
            int posX = 17;
            int posY = 42;
            for (int slot = 0; slot < machineSlots; slot++) {
                int index = slot;
                addSlot(new SlotItemHandler(drawbridge.getInventory(), slot, posX, posY) {
                    @Override
                    public boolean isActive() {
                        return index < drawbridge.getSlotCount();
                    }
                });
                posX += 18;
                if (slot % 8 == 7) {
                    posX = 17;
                    posY += 18;
                }
            }
            addPlayerSlots(playerInventory, 8, 128);
        } else {
            this.machineSlots = 1;
            addSlot(new SlotItemHandler(drawbridge.getInventory(), 0, 8, 21));
            addPlayerSlots(playerInventory, 8, 54);
        }
        addDataSlots(data);
    }

    public DrawbridgeBlockEntity getDrawbridge() {
        return drawbridge;
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, machineSlots);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(drawbridge, player);
    }
}
