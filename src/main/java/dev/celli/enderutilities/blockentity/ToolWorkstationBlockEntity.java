package dev.celli.enderutilities.blockentity;

import dev.celli.enderutilities.inventory.InstalledToolModulesHandler;
import dev.celli.enderutilities.item.module.IModular;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.menu.ToolWorkstationMenu;
import dev.celli.enderutilities.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ToolWorkstationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_TOOL = 0;
    public static final int MODULE_SLOTS = InstalledToolModulesHandler.SLOTS;
    public static final int STORAGE_SLOTS = 9;
    public static final int SLOT_RENAME = 20;
    public static final int SIZE = 21;

    private final ItemStackHandler inventory = new ItemStackHandler(SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final InstalledToolModulesHandler installedModules =
            new InstalledToolModulesHandler(this::getTool, this::setChanged);

    public ToolWorkstationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOOL_WORKSTATION.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public InstalledToolModulesHandler getInstalledModules() {
        return installedModules;
    }

    public ItemStack getTool() {
        return inventory.getStackInSlot(SLOT_TOOL);
    }

    /**
     * Older ports kept installed modules only in BE slots 1-10. Copy them onto the
     * tool once, then drop those leftover copies so they cannot desync from the icon.
     */
    public void migrateLegacyModuleSlots() {
        ItemStack tool = getTool();
        if (!(tool.getItem() instanceof IModular)) {
            clearLegacyModuleCopies();
            return;
        }
        var data = ModularHelper.data(tool);
        if (data.modules().isEmpty()) {
            boolean any = false;
            for (int i = 0; i < MODULE_SLOTS; i++) {
                ItemStack module = inventory.getStackInSlot(1 + i);
                if (!module.isEmpty()) {
                    data = data.withSlot(i, module);
                    any = true;
                }
            }
            if (any) {
                ModularHelper.setData(tool, data);
            }
        }
        clearLegacyModuleCopies();
    }

    private void clearLegacyModuleCopies() {
        for (int i = 0; i < MODULE_SLOTS; i++) {
            inventory.setStackInSlot(1 + i, ItemStack.EMPTY);
        }
    }

    public void dropContents() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (i >= 1 && i <= MODULE_SLOTS) {
                continue;
            }
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        migrateLegacyModuleSlots();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("enderutilities.container.tool_workstation");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ToolWorkstationMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }
}
