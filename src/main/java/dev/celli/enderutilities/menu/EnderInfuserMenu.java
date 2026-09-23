package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.EnderInfuserBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EnderInfuserMenu extends AbstractContainerMenu {
    private final EnderInfuserBlockEntity infuser;
    private final ContainerData data;

    public EnderInfuserMenu(int containerId, Inventory playerInventory, EnderInfuserBlockEntity infuser) {
        this(containerId, playerInventory, infuser, infuser.getData());
    }

    public EnderInfuserMenu(int containerId, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf buf) {
        this(containerId, playerInventory, getInfuser(playerInventory, EnderMenu.readMenuBlockPos(playerInventory, buf)), new SimpleContainerData(3));
    }

    private EnderInfuserMenu(int containerId, Inventory playerInventory, EnderInfuserBlockEntity infuser, ContainerData data) {
        super(ModMenus.ENDER_INFUSER.get(), containerId);
        this.infuser = infuser;
        this.data = data;
        IItemHandler inv = infuser.getInventory();

        this.addSlot(new SlotItemHandler(inv, EnderInfuserBlockEntity.SLOT_MATERIAL, 44, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnderInfuserBlockEntity.isItemValidForSlot(EnderInfuserBlockEntity.SLOT_MATERIAL, stack);
            }
        });
        this.addSlot(new SlotItemHandler(inv, EnderInfuserBlockEntity.SLOT_CAP_IN, 134, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnderInfuserBlockEntity.isItemValidForSlot(EnderInfuserBlockEntity.SLOT_CAP_IN, stack);
            }
        });
        this.addSlot(new SlotItemHandler(inv, EnderInfuserBlockEntity.SLOT_CAP_OUT, 134, 66) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 152));
        }

        this.addDataSlots(data);
    }

    private static EnderInfuserBlockEntity getInfuser(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof EnderInfuserBlockEntity infuser) {
            return infuser;
        }
        throw new IllegalStateException("Ender Infuser missing at " + pos);
    }

    public int getAmountStored() {
        return data.get(0);
    }

    public int getMeltingProgress() {
        return data.get(1);
    }

    public int getChargeProgress() {
        return data.get(2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack extracted = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            extracted = current.copy();
            if (index < 3) {
                if (!this.moveItemStackTo(current, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return extracted;
    }

    @Override
    public boolean stillValid(Player player) {
        return infuser.getLevel() != null
                && infuser.getLevel().getBlockEntity(infuser.getBlockPos()) == infuser
                && player.distanceToSqr(infuser.getBlockPos().getCenter()) <= 64.0;
    }
}
