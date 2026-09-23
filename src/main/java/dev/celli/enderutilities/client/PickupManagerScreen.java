package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemPickupManager;
import dev.celli.enderutilities.menu.PickupManagerMenu;
import dev.celli.enderutilities.network.ItemGuiActionPayload;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class PickupManagerScreen extends AbstractContainerScreen<PickupManagerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.container.pickupmanager.png");
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.widgets.png");
    private static final int INFO_X = 153;
    private static final int INFO_Y = 87;
    private static final int INFO_SIZE = 18;
    private static final int[] SET_X = {9, 27, 45, 63};
    private static final int TX_SET_Y = 29;
    private static final int INV_SET_Y = 105;

    public PickupManagerScreen(PickupManagerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(WIDGETS, leftPos + INFO_X, topPos + INFO_Y, 134, 48, INFO_SIZE, INFO_SIZE);

        ItemStack stack = menu.container();
        int module = menu.selectedModule();
        graphics.blit(WIDGETS, leftPos + 115 + module * 18, topPos + 28, 102, 18, 18, 18);
        graphics.blit(WIDGETS, leftPos + 119 + module * 18, topPos + 17, 120, 0, 10, 10);
        int preset = menu.selectedPreset();
        graphics.blit(WIDGETS, leftPos + 101 + preset * 18, topPos + 162, 120, 0, 10, 10);

        int firstCrystal = 1 + PickupManagerMenu.FILTER_SLOTS;
        for (int i = 0; i < PickupManagerMenu.LINK_CRYSTALS; i++) {
            if (!menu.getSlot(firstCrystal + i).hasItem()) {
                graphics.blit(WIDGETS, leftPos + 116 + i * 18, topPos + 29, 240, 32, 16, 16);
            }
        }

        for (int i = 0; i < PickupManagerMenu.LINK_CRYSTALS; i++) {
            blitButton(graphics, mouseX, mouseY, 120 + i * 18, 18, 8, 8, 0, 0, 8);
        }
        for (int i = 0; i < ItemPickupManager.NUM_PRESETS; i++) {
            blitButton(graphics, mouseX, mouseY, 102 + i * 18, 163, 8, 8, 0, 128 + i * 8, 8);
        }

        int tx = ItemPickupManager.settings(stack);
        blitSetting(graphics, mouseX, mouseY, SET_X[0], TX_SET_Y, (tx & 0x1) != 0 ? 28 : 98);
        blitSetting(graphics, mouseX, mouseY, SET_X[1], TX_SET_Y, (tx & 0x2) != 0 ? 84 : 70);
        blitSetting(graphics, mouseX, mouseY, SET_X[2], TX_SET_Y, (tx & 0x4) != 0 ? 126 : 112);
        blitSetting(graphics, mouseX, mouseY, SET_X[3], TX_SET_Y, (tx & 0x8) != 0 ? 140 : 154);
        int inv = tx >> 4;
        blitSetting(graphics, mouseX, mouseY, SET_X[0], INV_SET_Y, (inv & 0x1) != 0 ? 28 : 98);
        blitSetting(graphics, mouseX, mouseY, SET_X[1], INV_SET_Y, (inv & 0x2) != 0 ? 84 : 70);
        blitSetting(graphics, mouseX, mouseY, SET_X[2], INV_SET_Y, (inv & 0x4) != 0 ? 126 : 112);
        blitSetting(graphics, mouseX, mouseY, SET_X[3], INV_SET_Y, (inv & 0x8) != 0 ? 140 : 154);
    }

    private void blitSetting(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int v) {
        blitButton(graphics, mouseX, mouseY, x, y, 14, 14, 60, v, 14);
    }

    private void blitButton(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h, int u, int v, int hoverU) {
        int state = hit(mouseX - leftPos, mouseY - topPos, x, y, w, h) ? 2 : 1;
        graphics.blit(WIDGETS, leftPos + x, topPos + y, u + state * hoverU, v, w, h);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("enderutilities.container.pickupmanager"), 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.transportfilters"), 8, 19, 0x404040, false);
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.inventoryfilters"), 81, 112, 0x404040, false);
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.filterpresets").append(":"), 8, 163, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        for (int i = 0; i < PickupManagerMenu.LINK_CRYSTALS; i++) {
            if (hit(x, y, 120 + i * 18, 18, 8, 8)) {
                send(ItemPickupManager.GUI_ACTION_SELECT_MODULE, i);
                return true;
            }
        }
        for (int i = 0; i < ItemPickupManager.NUM_PRESETS; i++) {
            if (hit(x, y, 102 + i * 18, 163, 8, 8)) {
                send(ItemPickupManager.GUI_ACTION_CHANGE_PRESET, i);
                return true;
            }
        }
        for (int i = 0; i < SET_X.length; i++) {
            if (hit(x, y, SET_X[i], TX_SET_Y, 14, 14)) {
                send(ItemPickupManager.GUI_ACTION_TOGGLE_TRANSPORT_SETTINGS, i);
                return true;
            }
            if (hit(x, y, SET_X[i], INV_SET_Y, 14, 14)) {
                send(ItemPickupManager.GUI_ACTION_TOGGLE_INVENTORY_SETTINGS, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (hit(x, y, INFO_X, INFO_Y, INFO_SIZE, INFO_SIZE)) {
            List<Component> lines = new ArrayList<>();
            EnderItemTooltips.addTranslatedTips("enderutilities.gui.infoarea.pickupmanager", lines, true);
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }

        int settings = ItemPickupManager.settings(menu.container());
        for (int i = 0; i < SET_X.length; i++) {
            if (hit(x, y, SET_X[i], TX_SET_Y, 14, 14)) {
                tip(graphics, mouseX, mouseY, settingTip(settings, i));
                return;
            }
            if (hit(x, y, SET_X[i], INV_SET_Y, 14, 14)) {
                tip(graphics, mouseX, mouseY, settingTip(settings >> 4, i));
                return;
            }
        }

        Slot slot = this.hoveredSlot;
        if (slot != null && !slot.hasItem()) {
            if (slot.index == 0) {
                tip(graphics, mouseX, mouseY, "enderutilities.gui.label.transportitemsslot");
                return;
            }
            if (slot.index >= 1 && slot.index < 1 + 18) {
                tip(graphics, mouseX, mouseY, "enderutilities.gui.label.transportfilters");
                return;
            }
            if (slot.index >= 1 + 18 && slot.index < 1 + PickupManagerMenu.FILTER_SLOTS) {
                tip(graphics, mouseX, mouseY, "enderutilities.gui.label.inventoryfilters");
                return;
            }
            if (slot.index >= 1 + PickupManagerMenu.FILTER_SLOTS
                    && slot.index < 1 + PickupManagerMenu.FILTER_SLOTS + PickupManagerMenu.LINK_CRYSTALS) {
                tip(graphics, mouseX, mouseY, "item.enderutilities.linkcrystal_1");
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void tip(GuiGraphics graphics, int mouseX, int mouseY, String key) {
        graphics.renderComponentTooltip(font, List.of(Component.translatable(key)), mouseX, mouseY);
    }

    private static String settingTip(int bits, int index) {
        return switch (index) {
            case 0 -> (bits & 0x1) != 0
                    ? "enderutilities.gui.label.pickup_manager.functionality.enabled"
                    : "enderutilities.gui.label.pickup_manager.functionality.disabled";
            case 1 -> (bits & 0x2) != 0
                    ? "enderutilities.gui.label.whitelist"
                    : "enderutilities.gui.label.blacklist";
            case 2 -> (bits & 0x4) != 0
                    ? "enderutilities.gui.label.meta.ignore"
                    : "enderutilities.gui.label.meta.match";
            default -> (bits & 0x8) != 0
                    ? "enderutilities.gui.label.nbt.match"
                    : "enderutilities.gui.label.nbt.ignore";
        };
    }

    private static void send(int action, int element) {
        PacketDistributor.sendToServer(new ItemGuiActionPayload(action, element));
    }

    private static boolean hit(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }
}
