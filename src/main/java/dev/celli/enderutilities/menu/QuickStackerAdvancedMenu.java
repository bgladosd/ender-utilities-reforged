package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.QuickStackerAdvancedBlockEntity;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class QuickStackerAdvancedMenu extends EnderMenu {
    private static final int FILTER_MENU_START = QuickStackerAdvancedBlockEntity.FILTER_START;
    private final QuickStackerAdvancedBlockEntity stacker;
    private final ContainerData data;

    public QuickStackerAdvancedMenu(int id, Inventory playerInventory, QuickStackerAdvancedBlockEntity stacker) {
        this(id, playerInventory, stacker, stacker.getData());
    }

    public QuickStackerAdvancedMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, QuickStackerAdvancedBlockEntity.class),
                new SimpleContainerData(6));
    }

    private QuickStackerAdvancedMenu(int id, Inventory playerInventory, QuickStackerAdvancedBlockEntity stacker, ContainerData data) {
        super(ModMenus.QUICK_STACKER_ADVANCED.get(), id);
        this.stacker = stacker;
        this.data = data;
        for (int i = 0; i < QuickStackerAdvancedBlockEntity.TARGETS; i++) {
            addSlot(new SlotItemHandler(stacker.getInventory(), i, 23 + i * 18, 17));
        }
        for (int i = 0; i < QuickStackerAdvancedBlockEntity.CARDS; i++) {
            addSlot(new SlotItemHandler(stacker.getInventory(), QuickStackerAdvancedBlockEntity.TARGETS + i, 23 + i * 18, 35));
        }
        for (int i = 0; i < QuickStackerAdvancedBlockEntity.FILTERS; i++) {
            addSlot(new SlotItemHandler(stacker.getFilterHandler(), i,
                    23 + (i % 9) * 18, 83 + (i / 9) * 18));
        }
        addPlayerSlots(playerInventory, 23, 174);
        addOffhandSlot(playerInventory, 5, 156);
        addDataSlots(data);
    }

    public QuickStackerAdvancedBlockEntity getStacker() {
        return stacker;
    }

    public boolean isAreaMode() {
        return data.get(0) != 0;
    }

    public int getFilterSettings() {
        return data.get(1);
    }

    public int getEnabledTargetsMask() {
        return data.get(2);
    }

    public int getSelectedTarget() {
        return data.get(3);
    }

    public long getEnabledSlotsMask() {
        return ((long) data.get(4) << 32) | (data.get(5) & 0xFFFFFFFFL);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= FILTER_MENU_START && slotId < FILTER_MENU_START + QuickStackerAdvancedBlockEntity.FILTERS
                && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
            ItemStack carried = getCarried();
            Slot slot = slots.get(slotId);
            slot.set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            return;
        }
        if (clickType == ClickType.CLONE && slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot.container == player.getInventory()) {
                int invSlot = slot.getContainerSlot();
                if (invSlot >= 0 && invSlot < 36 || invSlot == Inventory.SLOT_OFFHAND) {
                    stacker.setEnabledSlotsMask(stacker.getEnabledSlotsMask() ^ (1L << invSlot));
                }
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= FILTER_MENU_START && index < FILTER_MENU_START + QuickStackerAdvancedBlockEntity.FILTERS) {
            return ItemStack.EMPTY;
        }
        return quickMoveMachine(player, index, QuickStackerAdvancedBlockEntity.FILTER_START);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(stacker, player);
    }
}
