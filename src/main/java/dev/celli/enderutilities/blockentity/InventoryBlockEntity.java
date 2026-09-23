package dev.celli.enderutilities.blockentity;

import org.jetbrains.annotations.Nullable;

import dev.celli.enderutilities.block.HorizontalMachineBlock;
import dev.celli.enderutilities.inventory.LimitedItemStackHandler;
import dev.celli.enderutilities.menu.EnderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public abstract class InventoryBlockEntity extends BlockEntity implements MenuProvider,
        HorizontalMachineBlock.DroppingInventory, HorizontalMachineBlock.AnalogInventory {
    protected final LimitedItemStackHandler inventory;

    protected InventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, LimitedItemStackHandler inventory) {
        super(type, pos, state);
        this.inventory = inventory;
    }

    public LimitedItemStackHandler getInventory() {
        return inventory;
    }

    public IItemHandler getHopperHandler() {
        return inventory;
    }

    @Override
    public void dropContents() {
        EnderMenu.dropContents(inventory, level, worldPosition);
    }

    @Override
    public int getAnalogSignal() {
        return ItemHandlerHelper.calcRedstoneFromInventory(inventory);
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
    }

    public boolean stillValid(Player player) {
        return EnderMenu.stillValid(this, player);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    public abstract net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player);

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }
}
