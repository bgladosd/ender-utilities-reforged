package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.menu.DrawbridgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Orig {@code GuiDrawbridge}: delay/length labels, take/redstone widgets, advanced slot chrome.
 */
public class DrawbridgeScreen extends MachineScreen<DrawbridgeMenu> {
    public DrawbridgeScreen(DrawbridgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title,
                menu.getDrawbridge().isAdvanced() ? "textures/gui/drawbridge_advanced.png" : "textures/gui/drawbridge_normal.png",
                menu.getDrawbridge().isAdvanced() ? 210 : 136, menu.getDrawbridge().getBlockPos());
        this.titleLabelY = 5;
        this.inventoryLabelY = menu.getDrawbridge().isAdvanced() ? 117 : 43;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        boolean advanced = menu.getDrawbridge().isAdvanced();
        int labelX = advanced ? 70 : 76;
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.drawbridge.delay_num",
                menu.getData().get(0)), labelX, 19, 0x404040, false);
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.drawbridge.length_num",
                menu.getData().get(1)), labelX, advanced ? 31 : 31, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        if (menu.getDrawbridge().isAdvanced()) {
            int slots = menu.getDrawbridge().getSlotCount();
            for (int i = 0; i < slots && i < menu.slots.size(); i++) {
                Slot slot = menu.getSlot(i);
                if (slot.isActive()) {
                    graphics.blit(guiTexture(), leftPos + slot.x - 1, topPos + slot.y - 1, 7, 127, 18, 18);
                }
            }
        }
        for (GuiWidgets.IconButton button : buttons()) {
            GuiWidgets.render(graphics, button, leftPos, topPos, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        int amount = clickAmount(button);
        if (hasShiftDown()) {
            amount *= 8;
        }
        if (hasControlDown()) {
            amount *= 4;
        }
        List<GuiWidgets.IconButton> icons = buttons();
        for (int i = 0; i < icons.size(); i++) {
            if (GuiWidgets.hit(x, y, icons.get(i))) {
                sendAction(i, amount);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (GuiWidgets.IconButton button : buttons()) {
            if (GuiWidgets.tooltip(graphics, font, button, leftPos, topPos, mouseX, mouseY)) {
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private List<GuiWidgets.IconButton> buttons() {
        List<GuiWidgets.IconButton> list = new ArrayList<>();
        boolean advanced = menu.getDrawbridge().isAdvanced();
        int redstone = menu.getData().get(2);
        int takeX = advanced ? 18 : 27;
        int takeY = advanced ? 24 : 22;
        int delayX = advanced ? 57 : 63;
        int lengthY = advanced ? 30 : 31;
        int redstoneX = advanced ? 36 : 45;
        int redstoneY = advanced ? 24 : 22;
        int redstoneV = switch (redstone) {
            case 1 -> 196;
            case 2 -> 238;
            default -> 210;
        };
        String redstoneTip = switch (redstone) {
            case 1 -> "enderutilities.gui.label.drawbridge.redstone_mode.retract_when_powered";
            case 2 -> "enderutilities.gui.label.drawbridge.redstone_mode.toggle_on_pulse";
            default -> "enderutilities.gui.label.drawbridge.redstone_mode.extend_when_powered";
        };
        list.add(GuiWidgets.IconButton.widgets(takeX, takeY, 14, 14, 60, 42,
                "enderutilities.gui.label.drawbridge.take_blocks"));
        list.add(GuiWidgets.IconButton.widgets(delayX, 19, 8, 8, 0, 120,
                "enderutilities.gui.label.drawbridge.delay"));
        list.add(GuiWidgets.IconButton.widgets(delayX, lengthY, 8, 8, 0, 120,
                "enderutilities.gui.label.drawbridge.block_count"));
        list.add(GuiWidgets.IconButton.widgets(redstoneX, redstoneY, 14, 14, 60, redstoneV, redstoneTip));
        return list;
    }

    private static int clickAmount(int button) {
        if (button == 0 || button == 11) {
            return 1;
        }
        if (button == 1 || button == 9) {
            return -1;
        }
        return 0;
    }
}
