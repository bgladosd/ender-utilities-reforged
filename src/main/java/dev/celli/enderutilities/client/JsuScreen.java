package dev.celli.enderutilities.client;

import dev.celli.enderutilities.menu.JsuMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class JsuScreen extends MachineScreen<JsuMenu> {
    public JsuScreen(JsuMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "textures/gui/jsu.png", 220, menu.getJsu().getBlockPos());
        this.imageWidth = 192;
        this.titleLabelY = 6;
        this.inventoryLabelY = 129;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        menu.setRowOffset(menu.getRowOffset() + (scrollY < 0 ? 1 : -1));
        sendAction(0, scrollY < 0 ? 1 : -1);
        return true;
    }
}
