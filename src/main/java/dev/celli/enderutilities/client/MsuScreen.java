package dev.celli.enderutilities.client;

import dev.celli.enderutilities.menu.MsuMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Orig {@code GuiMSU}: 176x139, creative widget at (162,6), highlight at (161,5), bundle slot row.
 */
public class MsuScreen extends MachineScreen<MsuMenu> {
    public MsuScreen(MsuMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "textures/gui/msu.png", 139, menu.getMsu().getBlockPos());
        this.titleLabelY = 5;
        this.inventoryLabelY = 46;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        if (menu.getMsu().getStorageTier() == 1) {
            graphics.blit(guiTexture(), leftPos + 7, topPos + 22, 7, 56, 9 * 18, 18);
        }
        if (menu.getMsu().isCreative()) {
            graphics.blit(GuiWidgets.TEXTURE, leftPos + 161, topPos + 5, 120, 24, 10, 10);
        }
        GuiWidgets.render(graphics, creativeButton(), leftPos, topPos, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (GuiWidgets.hit((int) mouseX - leftPos, (int) mouseY - topPos, creativeButton())) {
            sendAction(1, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (GuiWidgets.tooltip(graphics, font, creativeButton(), leftPos, topPos, mouseX, mouseY)) {
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private GuiWidgets.IconButton creativeButton() {
        return GuiWidgets.IconButton.widgets(162, 6, 8, 8, 0, menu.getMsu().isCreative() ? 112 : 40,
                menu.getMsu().isCreative()
                        ? "enderutilities.gui.label.msu.creative.enabled"
                        : "enderutilities.gui.label.msu.creative.disabled");
    }
}
