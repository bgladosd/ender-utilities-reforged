package dev.celli.enderutilities.client;

import org.apache.commons.lang3.StringUtils;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemBuildersWand;
import dev.celli.enderutilities.network.RenamePayload;
import dev.celli.enderutilities.registry.ModDataComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class BuilderWandTemplateScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.builderswand.png");
    private EditBox nameField;
    private int leftPos;
    private int topPos;
    private final int imageWidth = 192;
    private final int imageHeight = 82;

    public BuilderWandTemplateScreen() {
        super(Component.translatable("enderutilities.gui.label.builderswand.template"));
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        nameField = new EditBox(font, leftPos + 10, topPos + 25, 173, 12, Component.empty());
        nameField.setBordered(false);
        nameField.setMaxLength(60);
        nameField.setValue(templateName());
        nameField.setFocused(true);
        addRenderableWidget(nameField);
        addRenderableWidget(Button.builder(Component.translatable("enderutilities.gui.label.setname"), button -> save())
                .bounds(leftPos + 8, topPos + 40, 80, 20)
                .build());
        setInitialFocus(nameField);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, topPos + 6, 0x404040);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            save();
            return true;
        }
        return nameField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && nameField.isMouseOver(mouseX, mouseY)) {
            nameField.setValue("");
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void save() {
        PacketDistributor.sendToServer(new RenamePayload(StringUtils.defaultString(nameField.getValue())));
        onClose();
    }

    private String templateName() {
        if (minecraft == null || minecraft.player == null) {
            return "";
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.getItem() instanceof ItemBuildersWand
                && ItemBuildersWand.Mode.fromStack(stack) == ItemBuildersWand.Mode.COPY) {
            return stack.getOrDefault(ModDataComponents.WAND_TEMPLATE, dev.celli.enderutilities.data.WandTemplate.EMPTY).name();
        }
        return "";
    }
}
