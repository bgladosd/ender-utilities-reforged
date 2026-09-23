package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.menu.BarrelMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BarrelScreen extends MachineScreen<BarrelMenu> {
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.widgets.png");

    public BarrelScreen(BarrelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "textures/gui/barrel.png", 175, menu.getBarrel().getBlockPos());
        this.titleLabelY = 5;
        this.inventoryLabelY = 80;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        var upgrades = menu.getBarrel().getUpgrades();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (upgrades.getStackInSlot(i).isEmpty()) {
                graphics.blit(guiTexture(), leftPos + 44 + i * 18, topPos + 59, 176, i * 16, 16, 16);
            }
        }
        if (menu.isCreative()) {
            graphics.blit(WIDGETS, leftPos + 161, topPos + 5, 120, 24, 10, 10);
        }
        graphics.blit(WIDGETS, leftPos + 162, topPos + 6, 0, menu.isCreative() ? 112 : 40, 8, 8);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (x >= 162 && x < 170 && y >= 6 && y < 14) {
            sendAction(1, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (x >= 160 && x < 171 && y >= 17 && y < 28) {
            String translated = Component.translatable(
                    "enderutilities.gui.infoarea.barrel",
                    ModCommonConfig.BARREL_CAPACITY_UPGRADE_STACKS_PER.get()).getString();
            List<Component> lines = new ArrayList<>();
            for (String line : translated.split("\\|lf")) {
                lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (x >= 162 && x < 170 && y >= 6 && y < 14) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable(menu.isCreative()
                    ? "enderutilities.gui.label.msu.creative.enabled"
                    : "enderutilities.gui.label.msu.creative.disabled")), mouseX, mouseY);
        }
    }
}
