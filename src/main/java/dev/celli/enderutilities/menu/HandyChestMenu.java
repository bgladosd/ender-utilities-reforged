package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.blockentity.HandyChestBlockEntity;
import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import dev.celli.enderutilities.inventory.CardBackedSlot;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

public class HandyChestMenu extends EnderMenu {
    private static final int[] PLAYER_INV_Y = {95, 131, 167, 174};
    private final HandyChestBlockEntity chest;
    private final int itemSlots;
    private final ContainerData data;

    public HandyChestMenu(int id, Inventory playerInventory, HandyChestBlockEntity chest) {
        super(ModMenus.HANDY_CHEST.get(), id);
        this.chest = chest;
        int tier = Mth.clamp(chest.getStorageTier(), 0, 3);
        this.itemSlots = chest.getInvSize();
        int posX = 8;
        int posY = tier <= 2 ? 41 : 13;
        int rows = tier <= 2 ? (tier + 1) * 2 : 8;
        int columns = tier == 3 ? 13 : 9;
        IItemHandlerModifiable items = chest.getItemHandler();
        Player player = playerInventory.player;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                addSlot(new CardBackedSlot(items, row * columns + col, posX + col * 18, posY + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return chest.isInventoryAccessible(player) && super.mayPlace(stack);
                    }

                    @Override
                    public boolean mayPickup(Player query) {
                        return chest.isInventoryAccessible(query) && super.mayPickup(query);
                    }
                });
            }
        }
        int moduleX = tier <= 2 ? 98 : 224;
        int moduleY = tier <= 2 ? 8 : 174;
        int modX = tier == 3 ? 0 : 18;
        int modY = tier == 3 ? 18 : 0;
        for (int i = 0; i < HandyChestBlockEntity.CARD_SLOTS; i++) {
            addSlot(new SlotItemHandler(chest.getCardInventory(), i, moduleX + i * modX, moduleY + i * modY));
        }
        addPlayerSlots(playerInventory, tier == 3 ? 44 : 8, PLAYER_INV_Y[tier]);
        this.data = new SimpleContainerData(4) {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> chest.getSelectedModule();
                    case 1 -> chest.getQuickMode();
                    case 2 -> chest.getLockMask();
                    case 3 -> chest.isInventoryAccessible(player) ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> chest.setSelectedModule(value);
                    case 1 -> chest.setQuickMode(value);
                    case 2 -> chest.setLockMask(value);
                    default -> {
                    }
                }
            }
        };
        addDataSlots(data);
    }

    public HandyChestMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf, HandyChestBlockEntity.class));
    }

    public HandyChestBlockEntity getChest() {
        return chest;
    }

    public boolean isInventoryAccessible() {
        return data.get(3) != 0;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        int cardStart = itemSlots;
        int cardEnd = itemSlots + HandyChestBlockEntity.CARD_SLOTS;
        if (slotId >= cardStart && slotId < cardEnd) {
            int cardIndex = slotId - cardStart;
            if ((chest.getLockMask() & (1 << cardIndex)) != 0 && slots.get(slotId).hasItem()) {
                ItemStack card = slots.get(slotId).getItem();
                OwnerInfo owner = card.get(ModDataComponents.OWNER);
                if (owner != null && !owner.isOwner(player)) {
                    return;
                }
                super.clicked(slotId, button, clickType, player);
                if (!slots.get(slotId).hasItem()) {
                    chest.setLockMask(chest.getLockMask() & ~(1 << cardIndex));
                }
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, itemSlots + HandyChestBlockEntity.CARD_SLOTS);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(chest, player);
    }
}
