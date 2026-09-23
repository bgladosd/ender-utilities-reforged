package dev.celli.enderutilities.client;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.blockentity.QuickStackerAdvancedBlockEntity;
import dev.celli.enderutilities.menu.QuickStackerAdvancedMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class QuickStackerAdvancedScreen extends MachineScreen<QuickStackerAdvancedMenu> {
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/widgets.png");

    public QuickStackerAdvancedScreen(QuickStackerAdvancedMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "textures/gui/quick_stacker_advanced.png", 256, menu.getStacker().getBlockPos());
        this.imageWidth = 192;
        this.titleLabelY = 5;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, this.titleLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        boolean area = menu.isAreaMode();
        int enabledTargets = menu.getEnabledTargetsMask();
        int selected = menu.getSelectedTarget();
        if (!area && selected >= 0 && selected < 9) {
            graphics.blit(WIDGETS, leftPos + 26 + selected * 18, topPos + 53, 120, 0, 10, 10);
        }
        for (int slot = 0, dx = 22, dy = 16; slot < 18; slot++) {
            if (area) {
                graphics.blit(WIDGETS, leftPos + dx, topPos + dy, 102, 0, 18, 18);
            } else if ((enabledTargets & (1 << (slot % 9))) != 0) {
                graphics.blit(WIDGETS, leftPos + dx, topPos + dy, 102, 54, 18, 18);
            }
            dx += 18;
            if (slot == 8) {
                dx = 22;
                dy += 18;
            }
        }
        long mask = menu.getEnabledSlotsMask();
        for (int c = 0; c < 9; c++) {
            if ((mask & (1L << c)) != 0) {
                graphics.blit(WIDGETS, leftPos + 22 + c * 18, topPos + 231, 102, 18, 18, 18);
            }
        }
        int bit = 9;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++, bit++) {
                if ((mask & (1L << bit)) != 0) {
                    graphics.blit(WIDGETS, leftPos + 22 + c * 18, topPos + 173 + r * 18, 102, 18, 18, 18);
                }
            }
        }
        if ((mask & (1L << 40)) != 0) {
            graphics.blit(WIDGETS, leftPos + 4, topPos + 155, 102, 18, 18, 18);
        }
        for (int i = 0; i < 9; i++) {
            if (!area) {
                graphics.blit(WIDGETS, leftPos + 27 + i * 18, topPos + 54, 0, 0, 8, 8);
            }
            graphics.blit(WIDGETS, leftPos + 24 + i * 18, topPos + 157, 60, 42, 14, 14);
        }
        graphics.blit(WIDGETS, leftPos + 6, topPos + 233, 60, 28, 14, 14);
        for (int i = 0; i < 3; i++) {
            graphics.blit(WIDGETS, leftPos + 6, topPos + 175 + i * 18, 60, 28, 14, 14);
        }
        int filters = menu.getFilterSettings();
        graphics.blit(WIDGETS, leftPos + 6, topPos + 36, 60, area ? 168 : 28, 14, 14);
        graphics.blit(WIDGETS, leftPos + 24, topPos + 66, 60, (filters & 0x01) != 0 ? 28 : 98, 14, 14);
        graphics.blit(WIDGETS, leftPos + 42, topPos + 66, 60, (filters & 0x02) != 0 ? 70 : 84, 14, 14);
        graphics.blit(WIDGETS, leftPos + 60, topPos + 66, 60, (filters & 0x04) != 0 ? 112 : 126, 14, 14);
        graphics.blit(WIDGETS, leftPos + 78, topPos + 66, 60, (filters & 0x08) != 0 ? 140 : 154, 14, 14);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        for (int i = 0; i < 9; i++) {
            if (hit(x, y, 27 + i * 18, 54, 8, 8)) {
                sendAction(button == 1
                        ? QuickStackerAdvancedBlockEntity.GUI_ACTION_TOGGLE_TARGET_ENABLED
                        : QuickStackerAdvancedBlockEntity.GUI_ACTION_SET_ACTIVE_TARGET, i);
                return true;
            }
            if (hit(x, y, 24 + i * 18, 157, 14, 14)) {
                sendAction(QuickStackerAdvancedBlockEntity.GUI_ACTION_TOGGLE_COLUMNS, i);
                return true;
            }
        }
        if (hit(x, y, 6, 233, 14, 14)) {
            sendAction(QuickStackerAdvancedBlockEntity.GUI_ACTION_TOGGLE_ROWS, 0);
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (hit(x, y, 6, 175 + i * 18, 14, 14)) {
                sendAction(QuickStackerAdvancedBlockEntity.GUI_ACTION_TOGGLE_ROWS, i + 1);
                return true;
            }
        }
        if (hit(x, y, 6, 36, 14, 14)) {
            sendAction(QuickStackerAdvancedBlockEntity.GUI_ACTION_TOGGLE_TARGET_TYPE, 0);
            return true;
        }
        int[] filterX = {24, 42, 60, 78};
        for (int i = 0; i < 4; i++) {
            if (hit(x, y, filterX[i], 66, 14, 14)) {
                sendAction(QuickStackerAdvancedBlockEntity.GUI_ACTION_TOGGLE_FILTER_SETTINGS, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean hit(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }
}
