package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.inventory.ItemStackBackedHandler;
import dev.celli.enderutilities.item.ItemNullifier;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class NullifierMenu extends AbstractContainerMenu implements ItemGuiActionMenu {
    private final InteractionHand hand;
    private final Player player;

    public NullifierMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.NULLIFIER.get(), id);
        this.hand = hand;
        this.player = inventory.player;
        ItemStackBackedHandler handler = new ItemStackBackedHandler(
                () -> inventory.player.getItemInHand(hand),
                stack -> inventory.player.setItemInHand(hand, stack),
                ItemNullifier.NUM_SLOTS,
                ItemNullifier.MAX_STACK_SIZE
        );
        for (int i = 0; i < ItemNullifier.NUM_SLOTS; i++) {
            addSlot(new SlotItemHandler(handler, i, 8 + i * 18, 24));
        }
        addPlayerSlots(inventory, 8, 69);
    }

    public NullifierMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, EnderMenu.readMenuHand(buf));
    }

    public ItemStack container() {
        return player.getItemInHand(hand);
    }

    @Override
    public void performItemGuiAction(Player player, int action, int element) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ItemNullifier)) {
            return;
        }
        if (action == ItemNullifier.GUI_ACTION_SELECT_SLOT && element >= 0 && element < ItemNullifier.NUM_SLOTS) {
            stack.set(dev.celli.enderutilities.registry.ModDataComponents.ITEM_INVENTORY,
                    ItemNullifier.data(stack).withSelected(element));
        } else if (action == ItemNullifier.GUI_ACTION_TOGGLE_DISABLED) {
            var data = ItemNullifier.data(stack);
            stack.set(dev.celli.enderutilities.registry.ModDataComponents.ITEM_INVENTORY, data.withDisabled(!data.disabled()));
        }
    }

    private void addPlayerSlots(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, x + col * 18, y + 58));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot.container == player.getInventory() && slot.getItem() == player.getItemInHand(hand)) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < ItemNullifier.NUM_SLOTS) {
                if (!moveItemStackTo(stack, ItemNullifier.NUM_SLOTS, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, ItemNullifier.NUM_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof ItemNullifier;
    }
}
