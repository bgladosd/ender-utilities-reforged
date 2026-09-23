package dev.celli.enderutilities.client;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemNullifier;
import dev.celli.enderutilities.menu.NullifierMenu;
import dev.celli.enderutilities.network.ItemGuiActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class NullifierScreen extends AbstractContainerScreen<NullifierMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.container.nullifier.png");
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.widgets.png");

    public NullifierScreen(NullifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 151;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 58;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        var data = ItemNullifier.data(menu.container());
        graphics.blit(WIDGETS, leftPos + 11 + data.selected() * 18, topPos + 42, 120, 24, 10, 10);
        for (int i = 0; i < ItemNullifier.NUM_SLOTS; i++) {
            graphics.blit(WIDGETS, leftPos + 12 + i * 18, topPos + 43, 0, 0, 8, 8);
        }
        graphics.blit(WIDGETS, leftPos + 138, topPos + 12, 0, data.disabled() ? 48 : 0, 8, 8);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        for (int i = 0; i < ItemNullifier.NUM_SLOTS; i++) {
            if (x >= 12 + i * 18 && x < 20 + i * 18 && y >= 43 && y < 51) {
                PacketDistributor.sendToServer(new ItemGuiActionPayload(ItemNullifier.GUI_ACTION_SELECT_SLOT, i));
                return true;
            }
        }
        if (x >= 138 && x < 146 && y >= 12 && y < 20) {
            PacketDistributor.sendToServer(new ItemGuiActionPayload(ItemNullifier.GUI_ACTION_TOGGLE_DISABLED, 0));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
