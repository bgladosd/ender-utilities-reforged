package dev.celli.enderutilities.client;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemQuickStacker;
import dev.celli.enderutilities.menu.QuickStackerMenu;
import dev.celli.enderutilities.network.ItemGuiActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class QuickStackerScreen extends AbstractContainerScreen<QuickStackerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.container.quickstacker.png");
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.widgets.png");

    public QuickStackerScreen(QuickStackerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 192;
        this.imageHeight = 126;
        this.titleLabelY = 6;
        this.titleLabelX = 6;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        var player = minecraft != null ? minecraft.player : null;
        if (player == null) {
            return;
        }
        var stack = menu.getStackerStack(player);
        if (stack.isEmpty()) {
            return;
        }
        int selected = stack.getOrDefault(dev.celli.enderutilities.registry.ModDataComponents.ITEM_INVENTORY,
                dev.celli.enderutilities.data.ItemInventoryData.EMPTY).selected();
        graphics.blit(WIDGETS, leftPos + 118 + selected * 18, topPos + 16, 120, 0, 10, 10);
        long mask = ItemQuickStacker.getEnabledSlotsMask(stack);
        for (int c = 0; c < 9; c++) {
            if ((mask & (1L << c)) != 0) {
                graphics.blit(WIDGETS, leftPos + 24 + c * 18, topPos + 102, 102, 18, 18, 18);
            }
        }
        int bit = 9;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++, bit++) {
                if ((mask & (1L << bit)) != 0) {
                    graphics.blit(WIDGETS, leftPos + 24 + c * 18, topPos + 44 + r * 18, 102, 18, 18, 18);
                }
            }
        }
        if ((mask & (1L << 40)) != 0) {
            graphics.blit(WIDGETS, leftPos + 6, topPos + 26, 102, 18, 18, 18);
        }
        for (int i = 0; i < ItemQuickStacker.NUM_PRESETS; i++) {
            graphics.blit(WIDGETS, leftPos + 119 + i * 18, topPos + 17, 0, 128 + i * 8, 8, 8);
        }
        graphics.blit(WIDGETS, leftPos + 8, topPos + 104, 60, 28, 14, 14);
        for (int i = 0; i < 3; i++) {
            graphics.blit(WIDGETS, leftPos + 8, topPos + 46 + i * 18, 60, 28, 14, 14);
        }
        for (int i = 0; i < 9; i++) {
            graphics.blit(WIDGETS, leftPos + 26 + i * 18, topPos + 28, 60, 42, 14, 14);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        for (int i = 0; i < ItemQuickStacker.NUM_PRESETS; i++) {
            if (hit(x, y, 119 + i * 18, 17, 8, 8)) {
                PacketDistributor.sendToServer(new ItemGuiActionPayload(ItemQuickStacker.GUI_ACTION_CHANGE_PRESET, i));
                return true;
            }
        }
        if (hit(x, y, 8, 104, 14, 14)) {
            PacketDistributor.sendToServer(new ItemGuiActionPayload(ItemQuickStacker.GUI_ACTION_TOGGLE_ROWS, 0));
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (hit(x, y, 8, 46 + i * 18, 14, 14)) {
                PacketDistributor.sendToServer(new ItemGuiActionPayload(ItemQuickStacker.GUI_ACTION_TOGGLE_ROWS, i + 1));
                return true;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (hit(x, y, 26 + i * 18, 28, 14, 14)) {
                PacketDistributor.sendToServer(new ItemGuiActionPayload(ItemQuickStacker.GUI_ACTION_TOGGLE_COLUMNS, i));
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

    private static boolean hit(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }
}
