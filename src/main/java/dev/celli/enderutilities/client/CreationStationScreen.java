package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.menu.CreationStationMenu;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class CreationStationScreen extends MachineScreen<CreationStationMenu> {
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/widgets.png");
    private static final String[] ACTION_TIPS = {
            "enderutilities.gui.label.moveallitemsexcepthotbar",
            "enderutilities.gui.label.movematchingitemsexcepthotbar",
            "enderutilities.gui.label.leaveonefilledstack",
            "enderutilities.gui.label.fillstacks",
            "enderutilities.gui.label.movematchingitems",
            "enderutilities.gui.label.moveallitems"
    };

    public CreationStationScreen(CreationStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "textures/gui/creation_station.png", 256, menu.getStation().getBlockPos());
        this.imageWidth = 240;
        this.titleLabelX = 80;
        this.titleLabelY = 6;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 80, 6, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int x = this.leftPos;
        int y = this.topPos;
        int modeMask = this.menu.getModeMask();
        int selectedModule = this.menu.getSelectedModuleSlot();

        graphics.blit(WIDGETS, x + 204, y + 105 + selectedModule * 18, 120, 0, 10, 10);
        graphics.blit(WIDGETS, x + 215, y + 101 + selectedModule * 18, 102, 18, 18, 18);

        int quick = this.menu.getQuickMode();
        int[] actionX = CreationStationMenu.actionButtonX();
        int markerX = quick >= 0 && quick < actionX.length ? actionX[quick] : 9;
        graphics.blit(WIDGETS, x + markerX, y + 156, 120, 10, 14, 14);

        int[] craftModeX = CreationStationMenu.craftModeX();
        int mode = modeMask & 0xFF;
        for (int i = 0, bit = 0x1; i < 6; i++, bit <<= 1) {
            if ((mode & bit) != 0) {
                graphics.blit(WIDGETS, x + craftModeX[i], y + 88, 120, 0, 10, 10);
            }
        }

        graphics.blit(WIDGETS, x + 27, y + 32 + ((modeMask >> 8) & 0x7) * 11, 120, 0, 10, 10);
        graphics.blit(WIDGETS, x + 203, y + 32 + ((modeMask >> 11) & 0x7) * 11, 120, 0, 10, 10);

        int gridX = this.menu.getLastInteractedCraftingGrid() == 1 ? 146 : 38;
        graphics.blit(WIDGETS, x + gridX, y + 31, 184, 200, 56, 56);

        for (int i = 0; i < 4; i++) {
            Slot slot = this.menu.getSlot(27 + i);
            if (slot != null && !slot.hasItem()) {
                graphics.blit(WIDGETS, x + 216, y + 102 + i * 18, 240, 80, 16, 16);
            }
            blitIcon(graphics, mouseX, mouseY, x + 205, y + 106 + i * 18, 8, 8, 0, 0, 8);
        }

        for (int i = 0; i < 6; i++) {
            blitIcon(graphics, mouseX, mouseY, x + actionX[i] + 1, y + 157, 12, 12, 24, i * 12, 12);
        }

        blitIcon(graphics, mouseX, mouseY, x + 84, y + 89, 8, 8, 0, 8, 8);
        blitIcon(graphics, mouseX, mouseY, x + 148, y + 89, 8, 8, 0, 8, 8);
        blitIcon(graphics, mouseX, mouseY, x + 45, y + 89, 8, 8, 0, 32, 8);
        blitIcon(graphics, mouseX, mouseY, x + 58, y + 89, 8, 8, 0, 24, 8);
        blitIcon(graphics, mouseX, mouseY, x + 71, y + 89, 8, 8, 0, 16, 8);
        blitIcon(graphics, mouseX, mouseY, x + 161, y + 89, 8, 8, 0, 16, 8);
        blitIcon(graphics, mouseX, mouseY, x + 174, y + 89, 8, 8, 0, 24, 8);
        blitIcon(graphics, mouseX, mouseY, x + 187, y + 89, 8, 8, 0, 32, 8);
        blitIcon(graphics, mouseX, mouseY, x + 116, y + 90, 8, 8, 0, 24, 8);
        blitIcon(graphics, mouseX, mouseY, x + 116, y + 162, 8, 8, 0, 24, 8);

        boolean leftFast = (modeMask & 0x0040) != 0;
        boolean rightFast = (modeMask & 0x0080) != 0;
        blitIcon(graphics, mouseX, mouseY, x + 9, y + 71, 14, 14, 60, leftFast ? 14 : 0, 14);
        blitIcon(graphics, mouseX, mouseY, x + 217, y + 71, 14, 14, 60, rightFast ? 14 : 0, 14);

        for (int i = 0; i < 5; i++) {
            blitIcon(graphics, mouseX, mouseY, x + 28, y + 33 + i * 11, 8, 8, 0, 128 + i * 8, 8);
            blitIcon(graphics, mouseX, mouseY, x + 204, y + 33 + i * 11, 8, 8, 0, 128 + i * 8, 8);
        }

        graphics.blit(WIDGETS, x + 223, y + 88, 134, 66, 11, 11);

        int vOff = leftFast ? 34 : 20;
        int h = this.menu.getFuelProgress(0) * 13 / 100;
        graphics.blit(WIDGETS, x + 9, y + 30 + 12 - h, 134, vOff + 13 - h, 14, h + 1);
        if (this.menu.getSmeltProgress(0) > 0) {
            int w = this.menu.getSmeltProgress(0) * 11 / 100;
            graphics.blit(WIDGETS, x + 27, y + 11, 134, leftFast ? 10 : 0, w, 10);
        }

        vOff = rightFast ? 34 : 20;
        h = this.menu.getFuelProgress(1) * 13 / 100;
        graphics.blit(WIDGETS, x + 217, y + 30 + 12 - h, 134, vOff + 13 - h, 14, h + 1);
        if (this.menu.getSmeltProgress(1) > 0) {
            int w = this.menu.getSmeltProgress(1) * 11 / 100;
            graphics.blit(WIDGETS, x + 203 + 10 - w, y + 11, 144 + 10 - w, rightFast ? 10 : 0, w, 10);
        }
    }

    private static void blitIcon(GuiGraphics graphics, int mouseX, int mouseY, int bx, int by, int w, int h, int u, int v, int hoverOff) {
        if (mouseX >= bx && mouseX < bx + w && mouseY >= by && mouseY < by + h) {
            u += hoverOff;
        }
        graphics.blit(WIDGETS, bx, by, u, v, w, h);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - this.leftPos;
        int y = (int) mouseY - this.topPos;
        for (int i = 0; i < 4; i++) {
            if (hit(x, y, 205, 106 + i * 18, 8, 8)) {
                sendAction(0, i);
                return true;
            }
        }
        int[] moveX = CreationStationMenu.actionButtonX();
        for (int i = 0; i < moveX.length; i++) {
            if (hit(x, y, moveX[i] + 1, 157, 12, 12)) {
                sendAction(hasShiftDown() || button != 0 ? 2 : 1, i);
                return true;
            }
        }
        if (hit(x, y, 84, 89, 8, 8)) {
            sendAction(3, 0);
            return true;
        }
        if (hit(x, y, 148, 89, 8, 8)) {
            sendAction(3, 1);
            return true;
        }
        if (hit(x, y, 116, 90, 8, 8)) {
            sendAction(8, 0);
            return true;
        }
        if (hit(x, y, 116, 162, 8, 8)) {
            sendAction(8, 1);
            return true;
        }
        int[] leftModes = {45, 58, 71};
        int[] rightModes = {161, 174, 187};
        for (int i = 0; i < 3; i++) {
            if (hit(x, y, leftModes[i], 89, 8, 8)) {
                sendAction(7, i);
                return true;
            }
            if (hit(x, y, rightModes[i], 89, 8, 8)) {
                sendAction(7, 3 + i);
                return true;
            }
        }
        if (hit(x, y, 9, 71, 14, 14)) {
            sendAction(7, 6);
            return true;
        }
        if (hit(x, y, 217, 71, 14, 14)) {
            sendAction(7, 7);
            return true;
        }
        for (int i = 0; i < 5; i++) {
            if (hit(x, y, 28, 33 + i * 11, 8, 8) || hit(x, y, 204, 33 + i * 11, 8, 8)) {
                int element = hit(x, y, 204, 33 + i * 11, 8, 8) ? 5 + i : i;
                if (button == 2) {
                    sendAction(6, element);
                } else if (button == 1) {
                    sendAction(3, element / 5);
                } else {
                    sendAction(hasShiftDown() ? 5 : 4, element);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos;
        if (x >= 223 && x < 234 && y >= 88 && y < 99) {
            List<Component> lines = new ArrayList<>();
            EnderItemTooltips.addTranslatedTips("enderutilities.gui.infoarea.creation_station", lines, true);
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        int[] moveX = CreationStationMenu.actionButtonX();
        for (int i = 0; i < moveX.length; i++) {
            if (hit(x, y, moveX[i] + 1, 157, 12, 12)) {
                tip(graphics, mouseX, mouseY, ACTION_TIPS[i]);
                return;
            }
        }
        if (hit(x, y, 84, 89, 8, 8) || hit(x, y, 148, 89, 8, 8)) {
            tip(graphics, mouseX, mouseY, "enderutilities.gui.label.clearcraftinggrid");
            return;
        }
        if (hit(x, y, 45, 89, 8, 8) || hit(x, y, 187, 89, 8, 8)) {
            boolean on = (this.menu.getModeMask() & (hit(x, y, 45, 89, 8, 8) ? 0x0001 : 0x0008)) != 0;
            tip(graphics, mouseX, mouseY, on
                    ? "enderutilities.gui.label.useoredictionary.enabled"
                    : "enderutilities.gui.label.useoredictionary.disabled");
            return;
        }
        if (hit(x, y, 58, 89, 8, 8) || hit(x, y, 174, 89, 8, 8)) {
            boolean on = (this.menu.getModeMask() & (hit(x, y, 58, 89, 8, 8) ? 0x0002 : 0x0010)) != 0;
            tip(graphics, mouseX, mouseY, on
                    ? "enderutilities.gui.label.leaveoneitemongrid.enabled"
                    : "enderutilities.gui.label.leaveoneitemongrid.disabled");
            return;
        }
        if (hit(x, y, 71, 89, 8, 8) || hit(x, y, 161, 89, 8, 8)) {
            boolean on = (this.menu.getModeMask() & (hit(x, y, 71, 89, 8, 8) ? 0x0004 : 0x0020)) != 0;
            tip(graphics, mouseX, mouseY, on
                    ? "enderutilities.gui.label.useitemsfrominventory.enabled"
                    : "enderutilities.gui.label.useitemsfrominventory.disabled");
            return;
        }
        if (hit(x, y, 116, 90, 8, 8)) {
            tip(graphics, mouseX, mouseY, "enderutilities.gui.label.sortitems");
            return;
        }
        if (hit(x, y, 116, 162, 8, 8)) {
            tip(graphics, mouseX, mouseY, "enderutilities.gui.label.sortitems.player");
            return;
        }
        if (hit(x, y, 9, 71, 14, 14) || hit(x, y, 217, 71, 14, 14)) {
            boolean fast = (this.menu.getModeMask() & (hit(x, y, 9, 71, 14, 14) ? 0x0040 : 0x0080)) != 0;
            tip(graphics, mouseX, mouseY, fast
                    ? "enderutilities.gui.label.furnace.fast"
                    : "enderutilities.gui.label.furnace.slow");
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void tip(GuiGraphics graphics, int mouseX, int mouseY, String key) {
        graphics.renderComponentTooltip(this.font, List.of(Component.translatable(key)), mouseX, mouseY);
    }

    private static boolean hit(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }
}
