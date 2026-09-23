package dev.celli.enderutilities.client;

import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.module.IModular;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.menu.ToolWorkstationMenu;
import dev.celli.enderutilities.network.RenamePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class ToolWorkstationScreen extends AbstractContainerScreen<ToolWorkstationMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/tool_workstation.png");
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/widgets.png");

    private EditBox nameField;

    public ToolWorkstationScreen(ToolWorkstationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 217;
        this.inventoryLabelY = 125;
        this.titleLabelY = 5;
    }

    @Override
    protected void init() {
        super.init();
        this.nameField = new EditBox(this.font, this.leftPos + 34, this.topPos + 100, 134, 12, Component.empty());
        this.nameField.setTextColor(-1);
        this.nameField.setTextColorUneditable(-1);
        this.nameField.setBordered(false);
        this.nameField.setMaxLength(50);
        this.nameField.setValue(this.getRenameName());
        this.addRenderableWidget(this.nameField);
        this.addRenderableWidget(Button.builder(Component.translatable("enderutilities.gui.label.setname"), button ->
                PacketDistributor.sendToServer(new RenamePayload(this.nameField.getValue()))
        ).bounds(this.leftPos + 108, this.topPos + 113, 60, 20).build());
    }

    @Override
    protected void containerTick() {
        String current = this.getRenameName();
        if (!this.nameField.isFocused() && !current.equals(this.nameField.getValue())) {
            this.nameField.setValue(current);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        ItemStack tool = this.menu.getWorkstation().getTool();
        if (tool.isEmpty() || !(tool.getItem() instanceof IModular)) {
            graphics.blit(WIDGETS, this.leftPos + 8, this.topPos + 19, 240, 176, 16, 16);
        }

        int dx = 79;
        int dy = 18;
        for (int i = 0; i < 10; i++) {
            Slot slot = this.menu.getSlot(1 + i);
            ModuleType type = this.menu.getModuleSlotType(i);
            if (!slot.hasItem()) {
                if (type == ModuleType.INVALID) {
                    graphics.blit(WIDGETS, this.leftPos + dx, this.topPos + dy, 102, 0, 18, 18);
                } else if (type.index() >= 0) {
                    graphics.blit(WIDGETS, this.leftPos + dx + 1, this.topPos + dy + 1, 240, type.index() * 16, 16, 16);
                }
            }
            dx += 18;
            if (i == 4) {
                dy += 18;
                dx = 79;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("enderutilities.gui.label.modulestorage"), 8, 56, 0x404040, false);
        graphics.drawString(this.font, Component.translatable("enderutilities.gui.label.renameitems"), 8, 86, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int x, int y) {
        Slot slot = this.hoveredSlot;
        if (slot != null && !slot.hasItem()) {
            if (slot.index == 0) {
                graphics.renderTooltip(this.font, Component.translatable("enderutilities.gui.label.tool_workstation.tool"), x, y);
                return;
            }
            if (slot.index == 20) {
                graphics.renderComponentTooltip(this.font, List.of(
                        Component.translatable("enderutilities.gui.label.tool_workstation.itemtorename"),
                        Component.translatable("enderutilities.gui.label.tool_workstation.useemptynametoreset")
                ), x, y);
                return;
            }
        }
        super.renderTooltip(graphics, x, y);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameField.isFocused() && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && this.nameField.isMouseOver(mouseX, mouseY)) {
            this.nameField.setValue("");
            this.nameField.setFocused(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String getRenameName() {
        ItemStack stack = this.menu.getRenameStack();
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }
}
