package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.inventory.LockableItemStackHandler;
import dev.celli.enderutilities.menu.MemoryChestMenu;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MemoryChestScreen extends MachineScreen<MemoryChestMenu> {
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.widgets.png");
    private static final int LOCK_X = 138;
    private static final int LOCK_Y = 15;
    private static final int LOCK_SIZE = 8;
    private static final int INFO_X = 160;
    private static final int INFO_Y = 5;
    private static final int INFO_SIZE = 11;

    public MemoryChestScreen(MemoryChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title,
                "textures/gui/gui.container.memory_chest." + menu.getChest().getStorageTier() + ".png",
                switch (menu.getChest().getStorageTier()) {
                    case 0 -> 140;
                    case 1 -> 176;
                    default -> 234;
                },
                menu.getChest().getBlockPos());
        this.titleLabelX = 8;
        this.titleLabelY = 15;
        this.inventoryLabelY = 10000;
    }

    private int inventoryLabelY() {
        return switch (menu.getChest().getStorageTier()) {
            case 0 -> 47;
            case 1 -> 83;
            default -> 137;
        };
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 15, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY(), 0x404025, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int lockV = menu.isPublic() ? 0 : 48;
        int lockState = hit(mouseX - leftPos, mouseY - topPos, LOCK_X, LOCK_Y, LOCK_SIZE, LOCK_SIZE) ? 2 : 1;
        graphics.blit(WIDGETS, leftPos + LOCK_X, topPos + LOCK_Y, lockState * LOCK_SIZE, lockV, LOCK_SIZE, LOCK_SIZE);
        graphics.blit(WIDGETS, leftPos + INFO_X, topPos + INFO_Y, 134, 66, INFO_SIZE, INFO_SIZE);
        drawLockedSlots(graphics);
    }

    private void drawLockedSlots(GuiGraphics graphics) {
        LockableItemStackHandler inv = menu.getChest().getLockable();
        for (int i = 0; i < menu.getMachineSlots(); i++) {
            if (!inv.isSlotLocked(i)) {
                continue;
            }
            Slot slot = menu.getSlot(i);
            ItemStack stack = inv.getStackInSlot(i);
            int v = 18;
            if (stack.isEmpty()) {
                v = 36;
                ItemStack template = inv.getTemplateStackInSlot(i);
                if (!template.isEmpty()) {
                    graphics.renderItem(template, leftPos + slot.x, topPos + slot.y);
                }
            } else if (!ItemStack.isSameItemSameComponents(stack, inv.getTemplateStackInSlot(i))) {
                v = 72;
            }
            graphics.blit(WIDGETS, leftPos + slot.x - 1, topPos + slot.y - 1, 102, v, 18, 18);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (hit(x, y, INFO_X, INFO_Y, INFO_SIZE, INFO_SIZE)) {
            List<Component> lines = new ArrayList<>();
            EnderItemTooltips.addTranslatedTips("enderutilities.gui.infoarea.memory_chest", lines, true);
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (hit(x, y, LOCK_X, LOCK_Y, LOCK_SIZE, LOCK_SIZE)) {
            String owner = menu.getChest().getOwnerName();
            String key = menu.isPublic() ? "enderutilities.gui.label.public" : "enderutilities.gui.label.private";
            graphics.renderComponentTooltip(font, List.of(Component.translatable(key)
                    .append(" (")
                    .append(Component.translatable("enderutilities.tooltip.item.owner"))
                    .append(": ")
                    .append(owner)
                    .append(")")), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (hit(x, y, LOCK_X, LOCK_Y, LOCK_SIZE, LOCK_SIZE)) {
            sendAction(1, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean hit(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }
}
