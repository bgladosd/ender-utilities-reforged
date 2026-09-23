package dev.celli.enderutilities.client;

import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.blockentity.EnderInfuserBlockEntity;
import dev.celli.enderutilities.menu.EnderInfuserMenu;
import dev.celli.enderutilities.util.EUStringUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class EnderInfuserScreen extends AbstractContainerScreen<EnderInfuserMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/ender_infuser.png");
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/widgets.png");

    public EnderInfuserScreen(EnderInfuserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = 84;
        this.titleLabelY = 5;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int stored = this.menu.getAmountStored();
        if (stored > 0) {
            int h = 48;
            int t = stored * h / EnderInfuserBlockEntity.MAX_AMOUNT;
            graphics.blit(TEXTURE, this.leftPos + 87, this.topPos + 23 + h - t, 176, 18 + h - t, 28, t);
        }

        int melting = this.menu.getMeltingProgress();
        if (melting > 0) {
            int t = melting * 15 / 100;
            graphics.blit(TEXTURE, this.leftPos + 66, this.topPos + 26, 204, 18, t, 11);
        }

        int progress = this.menu.getChargeProgress();
        if (progress > 0) {
            progress = progress * 15 / 100;
            graphics.blit(TEXTURE, this.leftPos + 116, this.topPos + 39, 204, 18, progress, 11);
        }

        if (!this.menu.getSlot(0).hasItem()) {
            graphics.blit(WIDGETS, this.leftPos + 44, this.topPos + 24, 240, 160, 16, 16);
        }
        if (!this.menu.getSlot(1).hasItem()) {
            graphics.blit(WIDGETS, this.leftPos + 134, this.topPos + 8, 240, 16, 16, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 5, 0x404025, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, 84, 0x404025, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Slot slot = this.hoveredSlot;
        if (slot != null && slot.index == 0 && !slot.hasItem()) {
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable("enderutilities.gui.label.enderinfuser.input")), mouseX, mouseY);
            return;
        }
        if (slot != null && slot.index == 1 && !slot.hasItem()) {
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable("enderutilities.gui.label.enderinfuser.chargeableinput")), mouseX, mouseY);
            return;
        }
        int x = this.leftPos;
        int y = this.topPos;
        if (mouseX >= x + 87 && mouseX <= x + 114 && mouseY >= y + 23 && mouseY <= y + 71) {
            int stored = this.menu.getAmountStored();
            int ec = stored * EnderInfuserBlockEntity.ENDER_CHARGE_PER_MILLIBUCKET;
            int ecCapacity = EnderInfuserBlockEntity.MAX_AMOUNT * EnderInfuserBlockEntity.ENDER_CHARGE_PER_MILLIBUCKET;
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.literal(EUStringUtils.formatNumberWithKSeparators(ec) + " / "
                            + EUStringUtils.formatNumberWithKSeparators(ecCapacity) + " EC"),
                    Component.literal("(" + EUStringUtils.formatNumberWithKSeparators(stored) + " / "
                            + EUStringUtils.formatNumberWithKSeparators(EnderInfuserBlockEntity.MAX_AMOUNT) + " mB)")
            ), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }
}
