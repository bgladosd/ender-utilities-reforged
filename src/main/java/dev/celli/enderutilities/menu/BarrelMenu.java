package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.BarrelBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BarrelMenu extends LargeStacksMenu {
    private final BarrelBlockEntity barrel;
    private final ContainerData data;

    public BarrelMenu(int id, Inventory playerInventory, BarrelBlockEntity barrel) {
        super(ModMenus.BARREL.get(), id, 1);
        this.barrel = barrel;
        addSlot(new LargeStackSlot(barrel.getInventory(), 0, 80, 23));
        for (int i = 0; i < 4; i++) {
            addSlot(new SlotItemHandler(barrel.getUpgrades(), i, 44 + i * 18, 59));
        }
        addPlayerSlots(playerInventory, 8, 93);
        this.data = new SimpleContainerData(1) {
            @Override
            public int get(int index) {
                return barrel.isCreative() ? 1 : 0;
            }

            @Override
            public void set(int index, int value) {
                barrel.setCreative(value != 0);
            }
        };
        addDataSlots(data);
    }

    public BarrelMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, BarrelBlockEntity.class));
    }

    public BarrelBlockEntity getBarrel() {
        return barrel;
    }

    public boolean isCreative() {
        return data.get(0) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, 5);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(barrel, player);
    }
}
