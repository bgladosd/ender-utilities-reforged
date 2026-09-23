package dev.celli.enderutilities.client;

import java.util.List;

import dev.celli.enderutilities.menu.EnderFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class EnderFurnaceScreen extends MachineScreen<EnderFurnaceMenu> {
    private static final ItemStack ENDER_CHEST = new ItemStack(Blocks.ENDER_CHEST);

    public EnderFurnaceScreen(EnderFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "textures/gui/ender_furnace.png", 166, menu.getFurnace().getBlockPos());
        this.titleLabelY = 5;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 5, 0x404025, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 4, 0x404025, false);
        String owner = this.menu.getOwnerName();
        if (owner != null && !owner.isEmpty()) {
            graphics.drawString(this.font, owner, this.imageWidth - this.font.width(owner) - 6, 20, 0x404025, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int x = this.leftPos;
        int y = this.topPos;

        if (this.menu.isOutputToEnderChest()) {
            graphics.blit(guiTexture(), x + 114, y + 34, 176, 78, 24, 16);
        }

        if (this.menu.isBurning()) {
            int uOffset = this.menu.isFastMode() ? 14 : 0;
            int h = this.menu.getFuelProgress() * 13 / 100;
            graphics.blit(guiTexture(), x + 34, y + 36 + 12 - h, 176 + uOffset, 12 - h, 14, h + 1);
        }

        if (this.menu.getSmeltingProgress() > 0) {
            int vOffset = 0;
            int w = this.menu.getSmeltingProgress() * 24 / 100;
            if (this.menu.isBurning()) {
                vOffset = this.menu.isFastMode() ? 32 : 16;
            }
            graphics.blit(guiTexture(), x + 57, y + 34, 176, 14 + vOffset, w, 16);
        }

        blitModeButton(graphics, mouseX, mouseY, x + 10, y + 53, this.menu.isFastMode() ? 30 : 14);
        blitModeButton(graphics, mouseX, mouseY, x + 145, y + 53, this.menu.isOutputToEnderChest() ? 62 : 46);

        graphics.renderItem(ENDER_CHEST, x + 145, y + 34);
    }

    private void blitModeButton(GuiGraphics graphics, int mouseX, int mouseY, int bx, int by, int v) {
        int u = 200;
        if (mouseX >= bx && mouseX < bx + 16 && mouseY >= by && mouseY < by + 16) {
            u += 16;
        }
        graphics.blit(guiTexture(), bx, by, u, v, 16, 16);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = mouseX - this.leftPos;
        int y = mouseY - this.topPos;
        if (x >= 10 && x < 26 && y >= 53 && y < 69) {
            String key = this.menu.isFastMode()
                    ? "enderutilities.gui.label.furnace.fast"
                    : "enderutilities.gui.label.furnace.slow";
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(key)), mouseX, mouseY);
            return;
        }
        if (x >= 145 && x < 161 && y >= 53 && y < 69) {
            String key = this.menu.isOutputToEnderChest()
                    ? "enderutilities.gui.label.outputtoenderchest.enabled"
                    : "enderutilities.gui.label.outputtoenderchest.disabled";
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(key)), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - this.leftPos;
        int y = (int) mouseY - this.topPos;
        if (x >= 10 && x < 26 && y >= 53 && y < 69) {
            sendAction(0, 0);
            return true;
        }
        if (x >= 145 && x < 161 && y >= 53 && y < 69) {
            sendAction(1, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
