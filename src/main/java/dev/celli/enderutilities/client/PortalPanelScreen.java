package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.menu.PortalPanelMenu;
import dev.celli.enderutilities.network.RenamePayload;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

public class PortalPanelScreen extends AbstractContainerScreen<PortalPanelMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/portal_panel.png");
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/widgets.png");

    private EditBox nameField;
    private String nameLast = "";

    public PortalPanelScreen(PortalPanelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 251;
        this.inventoryLabelY = 156;
        this.titleLabelY = 5;
    }

    @Override
    protected void init() {
        super.init();
        this.nameField = new EditBox(this.font, this.leftPos + 11, this.topPos + 131, 154, 12, Component.empty());
        this.nameField.setTextColor(-1);
        this.nameField.setTextColorUneditable(-1);
        this.nameField.setBordered(false);
        this.nameField.setMaxLength(50);
        this.nameField.setValue(this.menu.getPanel().getPanelDisplayName());
        this.addRenderableWidget(this.nameField);
        this.addRenderableWidget(Button.builder(Component.translatable("enderutilities.gui.label.setname"), button ->
                PacketDistributor.sendToServer(new RenamePayload(this.nameField.getValue()))
        ).bounds(this.leftPos + 108, this.topPos + 144, 60, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameField.isFocused() && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        if (this.nameField.isFocused() && this.nameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        String name = this.menu.getPanel().getPanelDisplayName();
        if (!this.nameLast.equals(name)) {
            this.nameField.setValue(name);
            this.nameLast = name;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (button == 1
                && mouseX >= nameField.getX() && mouseX < nameField.getX() + nameField.getWidth()
                && mouseY >= nameField.getY() && mouseY < nameField.getY() + nameField.getHeight()) {
            nameField.setValue("");
            nameField.setFocused(true);
            return true;
        }
        int xOff = 57;
        int yOff = 56;
        for (int i = 0; i < 8; i++) {
            if (x >= xOff && x < xOff + 8 && y >= yOff && y < yOff + 8) {
                PacketDistributor.sendToServer(new dev.celli.enderutilities.network.GuiActionPayload(
                        menu.getPanel().getBlockPos(), 0, i));
                return true;
            }
            xOff += 18;
            if (i == 3) {
                xOff = 57;
                yOff = 106;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int u = 240;
        int v = 32;
        int active = menu.getActiveTargetId();
        for (int i = 0; i < 16; i++) {
            Slot slot = menu.getSlot(i);
            if (i < 8 && active == i) {
                graphics.blit(WIDGETS, leftPos + slot.x - 1, topPos + slot.y - 1, 102, 54, 18, 18);
                graphics.blit(WIDGETS, leftPos + slot.x - 1, topPos + slot.y - 1 + 18, 102, 54, 18, 18);
            }
            if (!slot.hasItem()) {
                graphics.blit(WIDGETS, leftPos + slot.x, topPos + slot.y, u, v, 16, 16);
            }
            if (i == 7) {
                u = 0;
                v = 240;
            }
        }
        int xOff = 57;
        int yOff = 56;
        for (int i = 0; i < 8; i++) {
            graphics.blit(WIDGETS, leftPos + xOff, topPos + yOff, 0, 0, 8, 8);
            xOff += 18;
            if (i == 3) {
                xOff = 57;
                yOff = 106;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("enderutilities.gui.label.settargetname"), 8, 118, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        if (relX >= 153 && relX <= 171 && relY >= 5 && relY <= 23) {
            List<Component> lines = new ArrayList<>();
            EnderItemTooltips.addTranslatedTips("enderutilities.gui.infoarea.portalpanel", lines, true);
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        Slot slot = this.hoveredSlot;
        if (slot != null && !slot.hasItem()) {
            List<Component> list = new ArrayList<>();
            if (slot.index >= 8 && slot.index <= 15) {
                EnderItemTooltips.addTranslatedTips("enderutilities.gui.label.portalpanel.dyeslot", list, true);
            } else if (slot.index >= 0 && slot.index < 8) {
                if (menu.portalOnly()) {
                    list.add(Component.translatable("item.enderutilities.linkcrystal_2"));
                } else {
                    list.add(Component.translatable("item.enderutilities.linkcrystal_2")
                            .append(" / ")
                            .append(Component.translatable("item.enderutilities.linkcrystal_0")));
                }
            }
            if (!list.isEmpty()) {
                graphics.renderComponentTooltip(this.font, list, x, y);
                return;
            }
        }
        super.renderTooltip(graphics, x, y);
    }
}
