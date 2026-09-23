package dev.celli.enderutilities.client;

import java.util.List;

import dev.celli.enderutilities.blockentity.AsuBlockEntity;
import dev.celli.enderutilities.menu.AsuMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Orig {@code GuiASU}: 176x195, slot/stack widgets at (8,18)/(8,30), labels, slot-row overlay.
 */
public class AsuScreen extends MachineScreen<AsuMenu> {
    public AsuScreen(AsuMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "textures/gui/asu.png", 195, menu.getAsu().getBlockPos());
        this.titleLabelY = 5;
        this.inventoryLabelY = 102;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.slot.amount.num", menu.getInvSize()),
                20, 18, 0x404040, false);
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.stack.limit.num", menu.getStackLimit()),
                20, 30, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int invSize = menu.getInvSize();
        int rows = Math.max((int) Math.ceil(invSize / 9.0), 1);
        for (int row = 0; row < rows; row++) {
            int rowLen = Mth.clamp(invSize - row * 9, 1, 9);
            graphics.blit(guiTexture(), leftPos + 7, topPos + 42 + row * 18, 7, 111, rowLen * 18, 18);
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
        if (GuiWidgets.hit(x, y, buttons().get(0))) {
            if (hasShiftDown()) {
                amount *= 9;
            }
            if (hasControlDown()) {
                amount *= 3;
            }
            sendAction(0, amount);
            return true;
        }
        if (GuiWidgets.hit(x, y, buttons().get(1))) {
            if (button == 2) {
                amount = hasShiftDown() ? AsuBlockEntity.MAX_STACK_SIZE : -AsuBlockEntity.MAX_STACK_SIZE;
            }
            if (hasShiftDown()) {
                amount *= 16;
            }
            if (hasControlDown()) {
                amount *= 64;
            }
            sendAction(1, amount);
            return true;
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

    private static List<GuiWidgets.IconButton> buttons() {
        return List.of(
                GuiWidgets.IconButton.widgets(8, 18, 8, 8, 0, 120, "enderutilities.gui.label.slot.amount"),
                GuiWidgets.IconButton.widgets(8, 30, 8, 8, 0, 120, "enderutilities.gui.label.stacklimit")
        );
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
