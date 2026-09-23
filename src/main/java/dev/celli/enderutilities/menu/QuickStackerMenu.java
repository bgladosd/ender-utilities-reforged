package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.item.ItemQuickStacker;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class QuickStackerMenu extends AbstractContainerMenu implements ItemGuiActionMenu {
    private final InteractionHand hand;

    public QuickStackerMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.QUICK_STACKER.get(), id);
        this.hand = hand;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 25 + col * 18, 45 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 25 + col * 18, 103));
        }
        addSlot(new Slot(inventory, Inventory.SLOT_OFFHAND, 7, 27));
    }

    public QuickStackerMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, EnderMenu.readMenuHand(buf));
    }

    public ItemStack getStackerStack(Player player) {
        return ItemQuickStacker.getEnabledItem(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.CLONE && slotId >= 0 && slotId < slots.size()) {
            ItemStack stacker = getStackerStack(player);
            if (!stacker.isEmpty()) {
                int invSlot = slots.get(slotId).getContainerSlot();
                if (invSlot >= 0 && invSlot < 36 || invSlot == Inventory.SLOT_OFFHAND) {
                    ItemQuickStacker.setEnabledSlotsMask(stacker, ItemQuickStacker.getEnabledSlotsMask(stacker) ^ (1L << invSlot));
                }
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void performItemGuiAction(Player player, int action, int element) {
        ItemQuickStacker.performGuiAction(player, getStackerStack(player), action, element);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof ItemQuickStacker
                || !ItemQuickStacker.getEnabledItem(player).isEmpty();
    }
}
