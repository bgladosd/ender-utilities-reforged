package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemInventorySwapper;
import dev.celli.enderutilities.menu.InventorySwapperMenu;
import dev.celli.enderutilities.network.ItemGuiActionPayload;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class InventorySwapperScreen extends AbstractContainerScreen<InventorySwapperMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.container.inventoryswapper.png");
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.widgets.png");
    private static final int INFO_X = 7;
    private static final int INFO_Y = 43;
    private static final int INFO_SIZE = 11;

    private int firstModuleX;
    private int firstModuleY;
    private int firstPlayerX;
    private int firstPlayerY;
    private int firstArmorX;
    private int firstArmorY;
    private int infoX;
    private int infoY;

    public InventorySwapperScreen(InventorySwapperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // Orig without Baubles is 199x249; expand only when Curios is loaded (orig 243 for Baubles).
        this.imageWidth = menu.curiosLoaded() ? 243 : 199;
        this.imageHeight = 249;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
        this.infoX = menu.curiosLoaded() ? 227 : INFO_X;
        this.infoY = menu.curiosLoaded() ? 5 : INFO_Y;
    }

    @Override
    protected void init() {
        super.init();
        Slot module = menu.getSlot(menu.moduleSlotStart());
        firstModuleX = module.x;
        firstModuleY = module.y;
        Slot player = menu.getSlot(menu.playerInvStart());
        firstPlayerX = player.x;
        firstPlayerY = player.y;
        Slot armor = menu.getSlot(menu.playerArmorStart());
        firstArmorX = armor.x;
        firstArmorY = armor.y;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos + menu.xOffset(), topPos, 0, 0, 199, imageHeight);
        if (menu.curiosLoaded()) {
            blitCuriosColumns(graphics);
        }
        graphics.blit(WIDGETS, leftPos + infoX, topPos + infoY, 134, 66, INFO_SIZE, INFO_SIZE);

        if (!menu.cardAccessible()) {
            for (int i = 0; i < menu.cardSlots(); i++) {
                Slot slot = menu.getSlot(i);
                graphics.blit(WIDGETS, leftPos + slot.x - 1, topPos + slot.y - 1, 102, 0, 18, 18);
            }
        }
        if (!menu.moduleAccessible()) {
            for (int i = 0; i < InventorySwapperMenu.MODULE_SLOTS; i++) {
                graphics.blit(WIDGETS, leftPos + firstModuleX - 1 + i * 18, topPos + firstModuleY - 1, 102, 0, 18, 18);
            }
        }

        int module = menu.selectedModule();
        if (module >= 0) {
            graphics.blit(WIDGETS, leftPos + firstModuleX - 1 + module * 18, topPos + firstModuleY - 1, 102, 18, 18, 18);
            graphics.blit(WIDGETS, leftPos + firstModuleX + 3 + module * 18, topPos + firstModuleY + 18, 120, 0, 10, 10);
        }

        ItemStack stack = menu.container();
        if (!stack.isEmpty() && stack.getItem() instanceof ItemInventorySwapper) {
            int preset = menu.selectedPreset();
            graphics.blit(WIDGETS, leftPos + firstPlayerX + 111 + preset * 12, topPos + firstPlayerY - 29, 120, 0, 10, 10);
            long mask = menu.enabledMask();
            long bit = 1L;
            for (int c = 0; c < 9; c++, bit <<= 1) {
                if ((mask & bit) != 0) {
                    graphics.blit(WIDGETS, leftPos + firstPlayerX - 1 + c * 18, topPos + firstPlayerY - 1 + 58, 102, 18, 18, 18);
                }
            }
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 9; c++, bit <<= 1) {
                    if ((mask & bit) != 0) {
                        graphics.blit(WIDGETS, leftPos + firstPlayerX - 1 + c * 18, topPos + firstPlayerY - 1 + r * 18, 102, 18, 18, 18);
                    }
                }
            }
            for (int r = 0; r < 4; r++, bit <<= 1) {
                if ((mask & bit) != 0) {
                    graphics.blit(WIDGETS, leftPos + firstArmorX - 1, topPos + firstArmorY - 1 + (3 - r) * 18, 102, 18, 18, 18);
                }
            }
            if ((mask & bit) != 0) {
                graphics.blit(WIDGETS, leftPos + firstArmorX - 1, topPos + firstArmorY - 1 + 4 * 18, 102, 18, 18, 18);
            }
            bit <<= 1;
            if (menu.curiosLoaded()) {
                for (int i = 0; i < InventorySwapperMenu.CURIOS_SLOT_COUNT; i++, bit <<= 1) {
                    if ((mask & bit) != 0) {
                        graphics.blit(WIDGETS, leftPos + firstArmorX - 21, topPos + firstArmorY - 1 + i * 18, 102, 18, 18, 18);
                    }
                }
            }
        }

        for (int i = 0; i < InventorySwapperMenu.MODULE_SLOTS; i++) {
            if (menu.moduleSlotEmpty(i)) {
                graphics.blit(WIDGETS, leftPos + firstModuleX + i * 18, topPos + firstModuleY, 240, 144, 16, 16);
            }
        }

        for (int i = 0; i < InventorySwapperMenu.MODULE_SLOTS; i++) {
            blitButton(graphics, mouseX, mouseY, firstModuleX + 4 + i * 18, firstModuleY + 19, 8, 8, 0, 0, 8);
        }
        for (int i = 0; i < ItemInventorySwapper.NUM_PRESETS; i++) {
            blitButton(graphics, mouseX, mouseY, firstPlayerX + 112 + i * 12, firstPlayerY - 28, 8, 8, 0, 128 + i * 8, 8);
        }
        blitButton(graphics, mouseX, mouseY, firstPlayerX - 17, firstPlayerY + 59, 14, 14, 60, 28, 14);
        for (int i = 0; i < 3; i++) {
            blitButton(graphics, mouseX, mouseY, firstPlayerX - 17, firstPlayerY + 1 + i * 18, 14, 14, 60, 28, 14);
        }
        for (int i = 0; i < 9; i++) {
            blitButton(graphics, mouseX, mouseY, firstPlayerX + 1 + i * 18, firstPlayerY - 17, 14, 14, 60, 42, 14);
        }
        blitButton(graphics, mouseX, mouseY, firstArmorX + 1, firstArmorY + 91, 14, 14, 60, 56, 14);
        blitButton(graphics, mouseX, mouseY, firstPlayerX + 1, firstPlayerY - 28, 8, 8, 0, ItemInventorySwapper.isEnabled(stack) ? 0 : 48, 8);
        blitButton(graphics, mouseX, mouseY, firstPlayerX + 13, firstPlayerY - 28, 8, 8, 0,
                ItemInventorySwapper.cycleMode(stack) ? 88 : 40, 8);
        if (menu.curiosLoaded()) {
            blitButton(graphics, mouseX, mouseY, firstArmorX - 19, firstArmorY + 127, 14, 14, 60, 56, 14);
        }
    }

    private void blitCuriosColumns(GuiGraphics graphics) {
        for (int i = 0; i < InventorySwapperMenu.CURIOS_SLOT_COUNT; i++) {
            Slot swapper = menu.getSlot(menu.swapperCuriosStart() + i);
            Slot playerSlot = menu.getSlot(menu.playerCuriosStart() + i);
            graphics.blit(WIDGETS, leftPos + swapper.x - 1, topPos + swapper.y - 1, 102, 0, 18, 18);
            graphics.blit(WIDGETS, leftPos + playerSlot.x - 1, topPos + playerSlot.y - 1, 102, 0, 18, 18);
            if (!swapper.hasItem()) {
                graphics.blit(WIDGETS, leftPos + firstModuleX + 77, topPos + firstArmorY + i * 18, 224, i * 16, 16, 16);
            }
            if (!playerSlot.hasItem()) {
                graphics.blit(WIDGETS, leftPos + firstArmorX - 20, topPos + firstArmorY + i * 18, 224, i * 16, 16, 16);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int xOff = menu.xOffset();
        graphics.drawString(font, Component.translatable("enderutilities.container.inventoryswapper"), 6 + xOff, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.memorycards"), firstModuleX, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.slotpresets").append(":"), 58 + xOff, 139, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        for (int i = 0; i < InventorySwapperMenu.MODULE_SLOTS; i++) {
            if (hit(x, y, firstModuleX + 4 + i * 18, firstModuleY + 19, 8, 8)) {
                send(ItemInventorySwapper.GUI_ACTION_SELECT_MODULE, i);
                return true;
            }
        }
        for (int i = 0; i < ItemInventorySwapper.NUM_PRESETS; i++) {
            if (hit(x, y, firstPlayerX + 112 + i * 12, firstPlayerY - 28, 8, 8)) {
                send(ItemInventorySwapper.GUI_ACTION_CHANGE_PRESET, i);
                return true;
            }
        }
        if (hit(x, y, firstPlayerX - 17, firstPlayerY + 59, 14, 14)) {
            send(ItemInventorySwapper.GUI_ACTION_TOGGLE_ROWS, 0);
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (hit(x, y, firstPlayerX - 17, firstPlayerY + 1 + i * 18, 14, 14)) {
                send(ItemInventorySwapper.GUI_ACTION_TOGGLE_ROWS, i + 1);
                return true;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (hit(x, y, firstPlayerX + 1 + i * 18, firstPlayerY - 17, 14, 14)) {
                send(ItemInventorySwapper.GUI_ACTION_TOGGLE_COLUMNS, i);
                return true;
            }
        }
        if (hit(x, y, firstArmorX + 1, firstArmorY + 91, 14, 14)) {
            send(ItemInventorySwapper.GUI_ACTION_TOGGLE_COLUMNS, 9);
            return true;
        }
        if (hit(x, y, firstPlayerX + 1, firstPlayerY - 28, 8, 8)) {
            send(ItemInventorySwapper.GUI_ACTION_TOGGLE_LOCKED, 0);
            return true;
        }
        if (hit(x, y, firstPlayerX + 13, firstPlayerY - 28, 8, 8)) {
            send(ItemInventorySwapper.GUI_ACTION_TOGGLE_CYCLE_MODE, 0);
            return true;
        }
        if (menu.curiosLoaded() && hit(x, y, firstArmorX - 19, firstArmorY + 127, 14, 14)) {
            send(ItemInventorySwapper.GUI_ACTION_TOGGLE_COLUMNS, 10);
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

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (hit(x, y, infoX, infoY, INFO_SIZE, INFO_SIZE)) {
            List<Component> lines = new ArrayList<>();
            EnderItemTooltips.addTranslatedTips("enderutilities.gui.infoarea.inventoryswapper", lines, true);
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (hit(x, y, firstPlayerX + 1, firstPlayerY - 28, 8, 8)) {
            boolean enabled = ItemInventorySwapper.isEnabled(menu.container());
            graphics.renderComponentTooltip(font, List.of(Component.translatable(
                    enabled ? "enderutilities.gui.label.item.enabled" : "enderutilities.gui.label.item.disabled")), mouseX, mouseY);
            return;
        }
        if (hit(x, y, firstPlayerX + 13, firstPlayerY - 28, 8, 8)) {
            boolean cycle = ItemInventorySwapper.cycleMode(menu.container());
            graphics.renderComponentTooltip(font, List.of(Component.translatable(
                    cycle ? "enderutilities.gui.label.cyclemode.enabled" : "enderutilities.gui.label.cyclemode.disabled")), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void blitButton(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h, int u, int v, int hoverU) {
        int state = hit(mouseX - leftPos, mouseY - topPos, x, y, w, h) ? 2 : 1;
        graphics.blit(WIDGETS, leftPos + x, topPos + y, u + state * hoverU, v, w, h);
    }

    private static void send(int action, int element) {
        PacketDistributor.sendToServer(new ItemGuiActionPayload(action, element));
    }

    private static boolean hit(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }
}
