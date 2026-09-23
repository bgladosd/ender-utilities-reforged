package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.blockentity.HandyChestBlockEntity;
import dev.celli.enderutilities.menu.HandyChestMenu;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class HandyChestScreen extends MachineScreen<HandyChestMenu> {
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.widgets.png");
    private static final String[] MOVE_TIPS = {
            "enderutilities.gui.label.moveallitemsexcepthotbar",
            "enderutilities.gui.label.movematchingitemsexcepthotbar",
            "enderutilities.gui.label.leaveonefilledstack",
            "enderutilities.gui.label.fillstacks",
            "enderutilities.gui.label.movematchingitems",
            "enderutilities.gui.label.moveallitems"
    };

    public HandyChestScreen(HandyChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "textures/gui/handy_chest_" + menu.getChest().getStorageTier() + ".png",
                switch (menu.getChest().getStorageTier()) {
                    case 0 -> 187;
                    case 1 -> 213;
                    case 2 -> 249;
                    default -> 256;
                },
                menu.getChest().getBlockPos());
        if (menu.getChest().getStorageTier() == 3) {
            this.imageWidth = 246;
        }
        this.titleLabelY = menu.getChest().getStorageTier() == 3 ? 3 : 6;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void renderSlotContents(GuiGraphics graphics, net.minecraft.world.item.ItemStack stack, Slot slot, String countString) {
        if (LargeStackSlotRenderer.isLargeStackSlot(slot, 0, menu.getChest().getInvSize()) && stack.getCount() != 1) {
            graphics.renderItem(stack, slot.x, slot.y, slot.x + slot.y * imageWidth);
            LargeStackSlotRenderer.renderOverlay(graphics, font, stack, slot.x, slot.y);
            return;
        }
        super.renderSlotContents(graphics, stack, slot, countString);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        HandyChestBlockEntity chest = menu.getChest();
        for (IconButton button : buttons()) {
            graphics.blit(WIDGETS, leftPos + button.x, topPos + button.y, button.u, button.v, button.w, button.h);
        }

        int selected = chest.getSelectedModule();
        IconButton select = selectButton(selected);
        if (select != null) {
            graphics.blit(WIDGETS, leftPos + select.x - 1, topPos + select.y - 1, 120, 0, 10, 10);
        }
        int cardSlot = chest.getInvSize() + selected;
        if (cardSlot >= 0 && cardSlot < menu.slots.size()) {
            Slot slot = menu.getSlot(cardSlot);
            graphics.blit(WIDGETS, leftPos + slot.x - 1, topPos + slot.y - 1, 102, 18, 18, 18);
        }

        int mode = chest.getQuickMode();
        IconButton quick = moveButton(mode);
        if (quick != null) {
            graphics.blit(WIDGETS, leftPos + quick.x - 1, topPos + quick.y - 1, 120, 10, 14, 14);
        }

        if (!menu.isInventoryAccessible()) {
            for (int i = 0; i < chest.getInvSize(); i++) {
                Slot slot = menu.getSlot(i);
                graphics.blit(WIDGETS, leftPos + slot.x - 1, topPos + slot.y - 1, 102, 0, 18, 18);
            }
        }

        int mask = chest.getLockMask();
        for (int i = 0; i < HandyChestBlockEntity.CARD_SLOTS; i++) {
            Slot slot = menu.getSlot(chest.getInvSize() + i);
            if (chest.getCardInventory().getStackInSlot(i).isEmpty()) {
                graphics.blit(WIDGETS, leftPos + slot.x, topPos + slot.y, 240, 80, 16, 16);
            }
            if ((mask & (1 << i)) != 0) {
                IconButton lock = selectButton(i);
                if (lock != null) {
                    graphics.blit(WIDGETS, leftPos + lock.x - 2, topPos + lock.y - 2, 120, 34, 12, 12);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        for (IconButton icon : buttons()) {
            if (!hit(x, y, icon.x, icon.y, icon.w, icon.h)) {
                continue;
            }
            if (icon.action == HandyChestBlockEntity.GUI_ACTION_SELECT_MODULE) {
                sendAction(button == 1
                        ? HandyChestBlockEntity.GUI_ACTION_LOCK_MODULE
                        : HandyChestBlockEntity.GUI_ACTION_SELECT_MODULE, icon.element);
                return true;
            }
            if (icon.action == HandyChestBlockEntity.GUI_ACTION_MOVE_ITEMS) {
                sendAction(hasShiftDown()
                        ? HandyChestBlockEntity.GUI_ACTION_SET_QUICK_ACTION
                        : HandyChestBlockEntity.GUI_ACTION_MOVE_ITEMS, icon.element);
                return true;
            }
            sendAction(icon.action, icon.element);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!drawButtonTooltip(graphics, mouseX, mouseY)) {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private boolean drawButtonTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        for (IconButton icon : buttons()) {
            if (!hit(x, y, icon.x, icon.y, icon.w, icon.h) || icon.tips.length == 0) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            for (String tip : icon.tips) {
                lines.addAll(EnderItemTooltips.splitTranslated(tip));
            }
            if (!lines.isEmpty()) {
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    private List<IconButton> buttons() {
        List<IconButton> list = new ArrayList<>();
        int tier = menu.getChest().getStorageTier();
        if (tier == 3) {
            for (int i = 0; i < 4; i++) {
                list.add(new IconButton(213, 178 + i * 18, 8, 8, 0, 0,
                        HandyChestBlockEntity.GUI_ACTION_SELECT_MODULE, i));
            }
            int[] moveX = {46, 64, 82, 154, 172, 190};
            for (int i = 0; i < 6; i++) {
                list.add(new IconButton(moveX[i], 158, 12, 12, 24, i * 12,
                        HandyChestBlockEntity.GUI_ACTION_MOVE_ITEMS, i, MOVE_TIPS[i]));
            }
            list.add(new IconButton(228, 158, 8, 8, 0, 24,
                    HandyChestBlockEntity.GUI_ACTION_SORT_ITEMS, 0, "enderutilities.gui.label.sortitems"));
            list.add(new IconButton(120, 162, 8, 8, 0, 24,
                    HandyChestBlockEntity.GUI_ACTION_SORT_ITEMS, 1, "enderutilities.gui.label.sortitems.player"));
        } else {
            for (int i = 0; i < 4; i++) {
                list.add(new IconButton(102 + i * 18, 27, 8, 8, 0, 0,
                        HandyChestBlockEntity.GUI_ACTION_SELECT_MODULE, i));
            }
            int yOff = 78 + tier * 36;
            int[] moveX = {10, 28, 46, 118, 136, 154};
            for (int i = 0; i < 6; i++) {
                list.add(new IconButton(moveX[i], yOff, 12, 12, 24, i * 12,
                        HandyChestBlockEntity.GUI_ACTION_MOVE_ITEMS, i, MOVE_TIPS[i]));
            }
            list.add(new IconButton(9, 30, 8, 8, 0, 24,
                    HandyChestBlockEntity.GUI_ACTION_SORT_ITEMS, 0, "enderutilities.gui.label.sortitems"));
            list.add(new IconButton(84, yOff + 4, 8, 8, 0, 24,
                    HandyChestBlockEntity.GUI_ACTION_SORT_ITEMS, 1, "enderutilities.gui.label.sortitems.player"));
        }
        return list;
    }

    private IconButton selectButton(int index) {
        for (IconButton button : buttons()) {
            if (button.action == HandyChestBlockEntity.GUI_ACTION_SELECT_MODULE && button.element == index) {
                return button;
            }
        }
        return null;
    }

    private IconButton moveButton(int index) {
        for (IconButton button : buttons()) {
            if (button.action == HandyChestBlockEntity.GUI_ACTION_MOVE_ITEMS && button.element == index) {
                return button;
            }
        }
        return null;
    }

    private static boolean hit(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }

    private record IconButton(int x, int y, int w, int h, int u, int v, int action, int element, String... tips) {
    }
}
