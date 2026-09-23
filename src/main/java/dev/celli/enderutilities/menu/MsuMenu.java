package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.MsuBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MsuMenu extends LargeStacksMenu {
    private final MsuBlockEntity msu;

    public MsuMenu(int id, Inventory playerInventory, MsuBlockEntity msu) {
        super(ModMenus.MSU.get(), id, msu.getInventory().getSlots());
        this.msu = msu;
        if (machineSlots == 1) {
            addSlot(new LargeStackSlot(msu.getInventory(), 0, 80, 23));
        } else {
            for (int i = 0; i < machineSlots; i++) {
                addSlot(new LargeStackSlot(msu.getInventory(), i, 8 + i * 18, 23));
            }
        }
        addPlayerSlots(playerInventory, 8, 57);
    }

    public MsuMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, MsuBlockEntity.class));
    }

    public MsuBlockEntity getMsu() {
        return msu;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, machineSlots);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(msu, player);
    }
}
