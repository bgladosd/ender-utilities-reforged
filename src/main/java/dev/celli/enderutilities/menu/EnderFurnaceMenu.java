package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.EnderFurnaceBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EnderFurnaceMenu extends EnderMenu {
    private final EnderFurnaceBlockEntity furnace;
    private final ContainerData data;
    private final String ownerName;

    public EnderFurnaceMenu(int id, Inventory playerInventory, EnderFurnaceBlockEntity furnace) {
        this(id, playerInventory, furnace, furnace.getData(), furnace.getOwnerName());
    }

    public EnderFurnaceMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, EnderFurnaceBlockEntity.class),
                new SimpleContainerData(4), readMenuUtf(buf));
    }

    private EnderFurnaceMenu(int id, Inventory playerInventory, EnderFurnaceBlockEntity furnace, ContainerData data, String ownerName) {
        super(ModMenus.ENDER_FURNACE.get(), id);
        this.furnace = furnace;
        this.data = data;
        this.ownerName = ownerName == null ? "" : ownerName;
        addSlot(new SlotItemHandler(furnace.getInventory(), EnderFurnaceBlockEntity.SLOT_INPUT, 34, 17));
        addSlot(new SlotItemHandler(furnace.getInventory(), EnderFurnaceBlockEntity.SLOT_FUEL, 34, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnderFurnaceBlockEntity.isItemFuel(stack);
            }
        });
        addSlot(new SlotItemHandler(furnace.getInventory(), EnderFurnaceBlockEntity.SLOT_OUTPUT, 88, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerSlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    public EnderFurnaceBlockEntity getFurnace() {
        return furnace;
    }

    public ContainerData getData() {
        return data;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getFuelProgress() {
        int fresh = data.get(2);
        if (fresh <= 0) {
            return 0;
        }
        return 100 * data.get(1) / fresh;
    }

    public int getSmeltingProgress() {
        return 100 * data.get(0) / EnderFurnaceBlockEntity.COOKTIME_DEFAULT;
    }

    public boolean isBurning() {
        return data.get(1) > 0;
    }

    public boolean isFastMode() {
        return (data.get(3) & 1) != 0;
    }

    public boolean isOutputToEnderChest() {
        return (data.get(3) & 2) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, 3);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(furnace, player);
    }
}
