package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.JsuBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class JsuMenu extends LargeStacksMenu {
    private final JsuBlockEntity jsu;
    private final ContainerData data;

    public JsuMenu(int id, Inventory playerInventory, JsuBlockEntity jsu) {
        this(id, playerInventory, jsu, createData());
    }

    public JsuMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, JsuBlockEntity.class), new SimpleContainerData(1));
    }

    private JsuMenu(int id, Inventory playerInventory, JsuBlockEntity jsu, ContainerData data) {
        super(ModMenus.JSU.get(), id, JsuBlockEntity.VISIBLE_SLOTS);
        this.jsu = jsu;
        this.data = data;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int visible = row * 9 + col;
                addSlot(new LargeStackSlot(jsu.getInventory(), visible, 8 + col * 18, 17 + row * 18) {
                    @Override
                    public int getSlotIndex() {
                        return visible + getRowOffset() * 9;
                    }

                    @Override
                    public ItemStack getItem() {
                        return jsu.getInventory().getStackInSlot(visible + getRowOffset() * 9);
                    }

                    @Override
                    public void set(ItemStack stack) {
                        jsu.getInventory().setStackInSlot(visible + getRowOffset() * 9, stack);
                        setChanged();
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getMaxStackSize() == 1;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return JsuBlockEntity.MAX_STACK_SIZE;
                    }

                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        return JsuBlockEntity.MAX_STACK_SIZE;
                    }
                });
            }
        }
        addPlayerSlots(playerInventory, 8, 139);
        addDataSlots(data);
    }

    private static ContainerData createData() {
        return new SimpleContainerData(1);
    }

    public int getRowOffset() {
        return data.get(0);
    }

    public void setRowOffset(int row) {
        data.set(0, Mth.clamp(row, 0, (JsuBlockEntity.INV_SIZE / 9) - 6));
    }

    public JsuBlockEntity getJsu() {
        return jsu;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, JsuBlockEntity.VISIBLE_SLOTS);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(jsu, player);
    }
}
