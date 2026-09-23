package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.InserterBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class InserterMenu extends EnderMenu {
    private final InserterBlockEntity inserter;
    private final ContainerData data;
    private final int machineSlots;

    public InserterMenu(int id, Inventory playerInventory, InserterBlockEntity inserter) {
        this(id, playerInventory, inserter, inserter.getData());
    }

    public InserterMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, InserterBlockEntity.class), new SimpleContainerData(4));
    }

    private InserterMenu(int id, Inventory playerInventory, InserterBlockEntity inserter, ContainerData data) {
        super(ModMenus.INSERTER.get(), id);
        this.inserter = inserter;
        this.data = data;
        addSlot(new SlotItemHandler(inserter.getInventory(), 0, 80, 27));
        int extra = 0;
        if (inserter.isFiltered()) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    addSlot(new SlotItemHandler(inserter.getFilters(), row * 9 + column, 8 + column * 18, 47 + row * 18));
                }
            }
            extra = InserterBlockEntity.FILTER_SLOTS;
        }
        this.machineSlots = 1 + extra;
        addPlayerSlots(playerInventory, 8, inserter.isFiltered() ? 115 : 59);
        addDataSlots(data);
    }

    public InserterBlockEntity getInserter() {
        return inserter;
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
        return stillValid(inserter, player);
    }
}
