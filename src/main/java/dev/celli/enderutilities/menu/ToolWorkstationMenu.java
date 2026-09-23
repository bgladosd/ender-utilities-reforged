package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.ToolWorkstationBlockEntity;
import dev.celli.enderutilities.inventory.InstalledToolModulesHandler;
import dev.celli.enderutilities.item.module.IModular;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ToolWorkstationMenu extends AbstractContainerMenu {
    private final ToolWorkstationBlockEntity workstation;

    public ToolWorkstationMenu(int containerId, Inventory playerInventory, ToolWorkstationBlockEntity workstation) {
        super(ModMenus.TOOL_WORKSTATION.get(), containerId);
        this.workstation = workstation;
        IItemHandler inv = workstation.getInventory();
        InstalledToolModulesHandler modules = workstation.getInstalledModules();

        this.addSlot(new SlotItemHandler(inv, ToolWorkstationBlockEntity.SLOT_TOOL, 8, 19) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof IModular;
            }
        });

        int x = 80;
        int y = 19;
        for (int i = 0; i < ToolWorkstationBlockEntity.MODULE_SLOTS; i++) {
            final int moduleIndex = i;
            this.addSlot(new SlotItemHandler(modules, i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isModuleAllowed(stack, moduleIndex);
                }
            });
            x += 18;
            if (i == 4) {
                y += 18;
                x = 80;
            }
        }

        x = 8;
        y = 66;
        for (int i = 0; i < ToolWorkstationBlockEntity.STORAGE_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(inv, 11 + i, x, y));
            x += 18;
        }

        this.addSlot(new SlotItemHandler(inv, ToolWorkstationBlockEntity.SLOT_RENAME, 8, 97));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 135 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 193));
        }
    }

    public ToolWorkstationMenu(int containerId, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf buf) {
        this(containerId, playerInventory, getWorkstation(playerInventory, EnderMenu.readMenuBlockPos(playerInventory, buf)));
    }

    private static ToolWorkstationBlockEntity getWorkstation(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof ToolWorkstationBlockEntity workstation) {
            return workstation;
        }
        throw new IllegalStateException("Tool Workstation missing at " + pos);
    }

    public ToolWorkstationBlockEntity getWorkstation() {
        return workstation;
    }

    public ModuleType getModuleSlotType(int moduleIndex) {
        ItemStack tool = workstation.getTool();
        if (!(tool.getItem() instanceof IModular modular)) {
            return ModuleType.INVALID;
        }
        return InstalledToolModulesHandler.slotType(modular, tool, moduleIndex);
    }

    public ItemStack getRenameStack() {
        return workstation.getInventory().getStackInSlot(ToolWorkstationBlockEntity.SLOT_RENAME);
    }

    private boolean isModuleAllowed(ItemStack stack, int moduleIndex) {
        return workstation.getInstalledModules().isItemValid(moduleIndex, stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack extracted = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            extracted = current.copy();
            int customEnd = 21;
            if (index < customEnd) {
                if (!this.moveItemStackTo(current, customEnd, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, customEnd, false)) {
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
        return workstation.getLevel() != null
                && workstation.getLevel().getBlockEntity(workstation.getBlockPos()) == workstation
                && player.distanceToSqr(workstation.getBlockPos().getCenter()) <= 64.0;
    }

}
