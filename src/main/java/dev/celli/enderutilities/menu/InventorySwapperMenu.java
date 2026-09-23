package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.compat.ModCompat;
import dev.celli.enderutilities.compat.curios.CuriosAccess;
import dev.celli.enderutilities.data.ItemInventoryData;
import dev.celli.enderutilities.inventory.ItemStackBackedHandler;
import dev.celli.enderutilities.inventory.ModularModuleHandler;
import dev.celli.enderutilities.item.ItemInventorySwapper;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

public class InventorySwapperMenu extends AbstractContainerMenu implements ItemGuiActionMenu {
    public static final int BASE_CARD_SLOTS = 41;
    public static final int CURIOS_SLOT_COUNT = 7;
    public static final int MODULE_SLOTS = 4;
    /** Orig non-Baubles layout. Prefer {@link #cardSlots()} when Curios may be present. */
    public static final int CARD_SLOTS = BASE_CARD_SLOTS;
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final InteractionHand hand;
    private final Player player;
    private final boolean curios;
    private final int xOffset;
    private final int cardSlots;
    private final int moduleSlotStart;
    private final int playerInvStart;
    private final int playerArmorStart;
    private final int playerCuriosStart;
    private final int swapperCuriosStart;

    public InventorySwapperMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.INVENTORY_SWAPPER.get(), id);
        this.hand = hand;
        this.player = inventory.player;
        this.curios = ModCompat.curiosLoaded();
        this.xOffset = curios ? 20 : 0;
        this.cardSlots = curios ? BASE_CARD_SLOTS + CURIOS_SLOT_COUNT : BASE_CARD_SLOTS;
        ItemStackBackedHandler card = new ItemStackBackedHandler(this::selectedCard, this::writeSelectedCard, cardSlots, 64);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(card, row * 9 + col + 9, 31 + xOffset + col * 18, 57 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new SlotItemHandler(card, col, 31 + xOffset + col * 18, 115));
        }
        addHorizontalArmorSlots(card, 31 + xOffset, 37);
        addOffhandSlot(new SlotItemHandler(card, 40, 103 + xOffset, 37));
        this.swapperCuriosStart = slots.size();
        if (curios) {
            for (int i = 0; i < CURIOS_SLOT_COUNT; i++) {
                addSlot(new SlotItemHandler(card, BASE_CARD_SLOTS + i, 218, 57 + i * 18));
            }
        }
        this.moduleSlotStart = slots.size();
        ModularModuleHandler modules = new ModularModuleHandler(
                this::container, this::write, ModuleType.MEMORY_CARD_ITEMS, MODULE_SLOTS);
        for (int i = 0; i < MODULE_SLOTS; i++) {
            addSlot(new SlotItemHandler(modules, i, 121 + xOffset + i * 18, 15));
        }
        this.playerInvStart = slots.size();
        addPlayerSlots(inventory, 31 + xOffset, 167);
        addOffhandSlot(new Slot(inventory, Inventory.SLOT_OFFHAND, 8 + xOffset, 129));
        this.playerArmorStart = slots.size();
        addVerticalPlayerArmorSlots(inventory, 8 + xOffset, 57);
        this.playerCuriosStart = slots.size();
        if (curios) {
            IItemHandlerModifiable curiosInv = CuriosAccess.playerCurios(player);
            for (int i = 0; i < CURIOS_SLOT_COUNT; i++) {
                final int curiosSlot = i;
                addSlot(new SlotItemHandler(curiosInv, i, 8, 57 + i * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return CuriosAccess.mayPlace(InventorySwapperMenu.this.player, curiosSlot, stack);
                    }
                });
            }
        }
    }

    public InventorySwapperMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, EnderMenu.readMenuHand(buf));
    }

    private void addHorizontalArmorSlots(IItemHandler card, int x, int y) {
        for (int i = 0; i < 4; i++) {
            addArmorItemHandlerSlot(card, 39 - i, ARMOR[i], x + i * 18, y);
        }
    }

    private void addVerticalPlayerArmorSlots(Inventory inventory, int x, int y) {
        for (int i = 0; i < 4; i++) {
            addArmorPlayerSlot(inventory, 39 - i, ARMOR[i], x, y + i * 18);
        }
    }

    private void addArmorItemHandlerSlot(IItemHandler handler, int index, EquipmentSlot type, int x, int y) {
        addSlot(new SlotItemHandler(handler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.canEquip(type, InventorySwapperMenu.this.player);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }).setBackground(InventoryMenu.BLOCK_ATLAS, emptyArmorIcon(type));
    }

    private void addArmorPlayerSlot(Inventory inventory, int index, EquipmentSlot type, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.canEquip(type, InventorySwapperMenu.this.player);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }).setBackground(InventoryMenu.BLOCK_ATLAS, emptyArmorIcon(type));
    }

    private void addOffhandSlot(Slot slot) {
        addSlot(slot).setBackground(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
    }

    private static ResourceLocation emptyArmorIcon(EquipmentSlot type) {
        return switch (type) {
            case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
            case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
            case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
            case FEET -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
            default -> InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
        };
    }

    public boolean curiosLoaded() {
        return curios;
    }

    public int xOffset() {
        return xOffset;
    }

    public int cardSlots() {
        return cardSlots;
    }

    public int moduleSlotStart() {
        return moduleSlotStart;
    }

    public int playerInvStart() {
        return playerInvStart;
    }

    public int playerArmorStart() {
        return playerArmorStart;
    }

    public int playerCuriosStart() {
        return playerCuriosStart;
    }

    public int swapperCuriosStart() {
        return swapperCuriosStart;
    }

    public ItemStack container() {
        return player.getItemInHand(hand);
    }

    private void write(ItemStack stack) {
        player.setItemInHand(hand, stack);
    }

    private ItemStack selectedCard() {
        return ModularHelper.getSelectedModuleStack(container(), ModuleType.MEMORY_CARD_ITEMS);
    }

    private void writeSelectedCard(ItemStack card) {
        ItemStack swapper = container();
        ModularHelper.setSelectedModuleStack(swapper, ModuleType.MEMORY_CARD_ITEMS, card);
        write(swapper);
    }

    public int selectedModule() {
        if (container().isEmpty()) {
            return -1;
        }
        return ModularHelper.data(container()).selectedLinkCrystal();
    }

    public int selectedPreset() {
        return container().getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY).selected();
    }

    public long enabledMask() {
        return ItemInventorySwapper.getEnabledSlotsMask(container());
    }

    public boolean cardAccessible() {
        return !selectedCard().isEmpty();
    }

    public boolean moduleAccessible() {
        return !container().isEmpty();
    }

    public boolean moduleSlotEmpty(int index) {
        return getSlot(moduleSlotStart + index).getItem().isEmpty();
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
            if (clickType == ClickType.CLONE) {
                int invSlot = -1;
                if (slot.container == player.getInventory()) {
                    invSlot = slot.getContainerSlot();
                } else if (curios && slotId >= playerCuriosStart && slotId < playerCuriosStart + CURIOS_SLOT_COUNT) {
                    invSlot = BASE_CARD_SLOTS + (slotId - playerCuriosStart);
                }
                if (invSlot >= 0) {
                    ItemStack swapper = container();
                    ItemInventorySwapper.setEnabledSlotsMask(swapper,
                            ItemInventorySwapper.getEnabledSlotsMask(swapper) ^ (1L << invSlot));
                    return;
                }
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void performItemGuiAction(Player player, int action, int element) {
        ItemStack stack = container();
        if (!(stack.getItem() instanceof ItemInventorySwapper)) {
            return;
        }
        if (action == ItemInventorySwapper.GUI_ACTION_SELECT_MODULE && element >= 0 && element < MODULE_SLOTS) {
            ModularHelper.setData(stack, ModularHelper.data(stack).withSelectedLinkCrystal(element));
        } else if (action == ItemInventorySwapper.GUI_ACTION_CHANGE_PRESET && element >= 0 && element < ItemInventorySwapper.NUM_PRESETS) {
            ItemInventoryData data = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
            stack.set(ModDataComponents.ITEM_INVENTORY, data.withSelected(element));
        } else if (action == ItemInventorySwapper.GUI_ACTION_TOGGLE_ROWS && element >= 0 && element < 4) {
            ItemInventorySwapper.setEnabledSlotsMask(stack, ItemInventorySwapper.getEnabledSlotsMask(stack) ^ (0x1FFL << (element * 9)));
        } else if (action == ItemInventorySwapper.GUI_ACTION_TOGGLE_COLUMNS && element >= 0 && element <= 10) {
            long mask = ItemInventorySwapper.getEnabledSlotsMask(stack);
            if (element < 9) {
                mask ^= 0x08040201L << element;
            } else if (element == 9) {
                mask ^= 0x1FL << 36;
            } else if (curios) {
                mask ^= 0x7FL << BASE_CARD_SLOTS;
            }
            ItemInventorySwapper.setEnabledSlotsMask(stack, mask);
        } else if (action == ItemInventorySwapper.GUI_ACTION_TOGGLE_LOCKED) {
            ModularHelper.setData(stack, ModularHelper.data(stack).withBindLocked(!ModularHelper.data(stack).bindLocked()));
        } else if (action == ItemInventorySwapper.GUI_ACTION_TOGGLE_CYCLE_MODE) {
            stack.set(ModDataComponents.ITEM_MODE, stack.getOrDefault(ModDataComponents.ITEM_MODE, 0) ^ 1);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int customEnd = cardSlots + MODULE_SLOTS;
            if (index < customEnd) {
                if (!moveItemStackTo(stack, customEnd, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, cardSlots, false)) {
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
        return player.getItemInHand(hand).getItem() instanceof ItemInventorySwapper;
    }
}
