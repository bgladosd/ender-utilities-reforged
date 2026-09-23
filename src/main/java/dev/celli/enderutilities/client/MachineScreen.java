package dev.celli.enderutilities.client;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.network.GuiActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;

public class MachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private final ResourceLocation texture;
    private final BlockPos pos;

    public MachineScreen(T menu, Inventory inventory, Component title, String texturePath, int height, BlockPos pos) {
        super(menu, inventory, title);
        this.texture = ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, texturePath);
        this.imageHeight = height;
        this.inventoryLabelY = height - 94;
        this.pos = pos;
    }

    protected ResourceLocation guiTexture() {
        return texture;
    }

    protected void sendAction(int action, int element) {
        if (pos != null) {
            PacketDistributor.sendToServer(new GuiActionPayload(pos, action, element));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
