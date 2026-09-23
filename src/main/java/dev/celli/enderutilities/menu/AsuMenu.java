package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.AsuBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class AsuMenu extends LargeStacksMenu {
    private final AsuBlockEntity asu;
    private final ContainerData data;

    public AsuMenu(int id, Inventory playerInventory, AsuBlockEntity asu) {
        this(id, playerInventory, asu, asu.getData());
    }

    public AsuMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, AsuBlockEntity.class), new SimpleContainerData(2));
    }

    private AsuMenu(int id, Inventory playerInventory, AsuBlockEntity asu, ContainerData data) {
        super(ModMenus.ASU.get(), id, AsuBlockEntity.MAX_INV_SIZE);
        this.asu = asu;
        this.data = data;
        for (int i = 0; i < AsuBlockEntity.MAX_INV_SIZE; i++) {
            int col = i % 9;
            int row = i / 9;
            addSlot(new LargeStackSlot(asu.getInventory(), i, 8 + col * 18, 43 + row * 18) {
                @Override
                public boolean isActive() {
                    return this.getSlotIndex() < AsuMenu.this.asu.getInventory().getVisibleSlots();
                }
            });
        }
        addPlayerSlots(playerInventory, 8, 112);
        addDataSlots(data);
    }

    public AsuBlockEntity getAsu() {
        return asu;
    }

    public int getInvSize() {
        return data.get(0);
    }

    public int getStackLimit() {
        return data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, AsuBlockEntity.MAX_INV_SIZE);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(asu, player);
    }
}
