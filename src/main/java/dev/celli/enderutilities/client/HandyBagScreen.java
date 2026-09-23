package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.data.ModularItemData;
import dev.celli.enderutilities.item.ItemHandyBag;
import dev.celli.enderutilities.menu.HandyBagMenu;
import dev.celli.enderutilities.network.ItemGuiActionPayload;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class HandyBagScreen extends AbstractContainerScreen<HandyBagMenu> {
    private static final ResourceLocation WIDGETS =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.widgets.png");
    private static final ResourceLocation INVENTORY =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final int[] MOVE_X = {0, 18, 36, 108, 126, 144};
    private static final String[] MOVE_TIPS = {
            "enderutilities.gui.label.moveallitemsexcepthotbar",
            "enderutilities.gui.label.movematchingitemsexcepthotbar",
            "enderutilities.gui.label.leaveonefilledstack",
            "enderutilities.gui.label.fillstacks",
            "enderutilities.gui.label.movematchingitems",
            "enderutilities.gui.label.moveallitems"
    };
    private static final String[] SHIFT_TIPS = {
            "enderutilities.gui.label.handybag.shiftclick.tobag",
            "enderutilities.gui.label.handybag.shiftclick.invhotbar",
            "enderutilities.gui.label.handybag.shiftclick.doubletapshift"
    };
    private boolean hasActivePotionEffects;

    public HandyBagScreen(HandyBagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = menu.getBagTier() == 1 ? 256 : 176;
        this.imageHeight = 256;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    private ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID,
                "textures/gui/gui.container.handybag." + menu.getBagTier() + ".png");
    }

    private int xOff() {
        return menu.getBagTier() == 1 ? 40 : 0;
    }

    @Override
    protected void init() {
        super.init();
        updateActivePotionEffects();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateActivePotionEffects();
    }

    private void updateActivePotionEffects() {
        if (minecraft == null || minecraft.player == null || minecraft.player.getActiveEffects().isEmpty()) {
            leftPos = (width - imageWidth) / 2;
            hasActivePotionEffects = false;
            return;
        }
        leftPos = 160 + (width - imageWidth - 200) / 2;
        hasActivePotionEffects = true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        Slot first = menu.getSlot(0);
        int moduleX = leftPos + first.x + 5 * 18;
        int moduleY = topPos + first.y - 33;

        if (!menu.hasSelectedCard()) {
            for (int i = 0; i < menu.getBagSlots(); i++) {
                Slot slot = menu.getSlot(i);
                graphics.blit(WIDGETS, leftPos + slot.x - 1, topPos + slot.y - 1, 102, 0, 18, 18);
            }
        }

        ItemStack bag = menu.getBag();
        ModularItemData modular = bag.getOrDefault(ModDataComponents.MODULAR, ModularItemData.EMPTY);
        int selected = menu.getSelectedModule();
        if (selected >= 0 && selected < HandyBagMenu.MODULE_SLOTS) {
            graphics.blit(WIDGETS, moduleX - 1 + selected * 18, moduleY - 1, 102, 18, 18, 18);
            graphics.blit(WIDGETS, moduleX + 3 + selected * 18, moduleY + 18, 120, 0, 10, 10);
        }
        for (int i = 0; i < HandyBagMenu.MODULE_SLOTS; i++) {
            if (modular.stackInSlot(i).isEmpty()) {
                graphics.blit(WIDGETS, moduleX + i * 18, moduleY, 240, 80, 16, 16);
            }
        }

        if (menu.getBagTier() == 1) {
            int[] highlightX = {114, 20, 226};
            for (int i = 0; i < 3; i++) {
                if (ItemHandyBag.isSectionMasked(bag, i, 0)) {
                    graphics.blit(WIDGETS, leftPos + highlightX[i], topPos + 90, 120, 24, 10, 10);
                }
            }
        }

        if (!bag.isEmpty() && ItemHandyBag.getEffectiveShiftMode(bag) == 0) {
            graphics.blit(WIDGETS, leftPos + xOff() + 64, topPos + 157, 154, 0, 12, 12);
        }

        for (IconButton button : buttons()) {
            graphics.blit(WIDGETS, leftPos + button.x, topPos + button.y, button.u, button.v, button.w, button.h);
        }

        if (minecraft != null && minecraft.player != null) {
            int px = leftPos + 51 + xOff();
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, px - 25, topPos + 15, px + 26, topPos + 84, 30, 0.0625F, mouseX, mouseY, minecraft.player);
        }
    }

    @Override
    protected void renderSlotContents(GuiGraphics graphics, ItemStack stack, Slot slot, String countString) {
        if (LargeStackSlotRenderer.isLargeStackSlot(slot, 0, menu.getBagSlots()) && stack.getCount() != 1) {
            graphics.renderItem(stack, slot.x, slot.y, slot.x + slot.y * imageWidth);
            LargeStackSlotRenderer.renderOverlay(graphics, font, stack, slot.x, slot.y);
            return;
        }
        super.renderSlotContents(graphics, stack, slot, countString);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = xOff();
        graphics.drawString(font, Component.translatable("enderutilities.container.handybag"), x + 8, 5, 0x404040, false);
        graphics.drawString(font, Component.translatable("container.crafting"), x + 97, 5, 0x404040, false);
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.memorycards"), x + 99, 59, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        for (IconButton icon : buttons()) {
            if (!hit(x, y, icon.x, icon.y, icon.w, icon.h)) {
                continue;
            }
            int element = icon.element;
            if (icon.action == ItemHandyBag.GUI_ACTION_MOVE_ITEMS && hasShiftDown()) {
                element |= 0x8000;
            } else if (icon.action == ItemHandyBag.GUI_ACTION_TOGGLE_MODES && button == 1) {
                element |= 0x8000;
            } else if (icon.action == ItemHandyBag.GUI_ACTION_TOGGLE_SHIFTCLICK) {
                element = button;
            }
            send(icon.action, element);
            if (icon.action == ItemHandyBag.GUI_ACTION_OPEN_CURIOS && minecraft != null && minecraft.player != null) {
                minecraft.setScreen(new InventoryScreen(minecraft.player));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hasActivePotionEffects) {
            drawActivePotionEffects(graphics);
        }
        if (!drawButtonTooltip(graphics, mouseX, mouseY)) {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private boolean drawButtonTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        for (IconButton icon : buttons()) {
            if (!hit(x, y, icon.x, icon.y, icon.w, icon.h) || icon.tips.length == 0) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            for (String tip : icon.tips) {
                lines.addAll(EnderItemTooltips.splitTranslated(tip));
            }
            if (!lines.isEmpty()) {
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    private void drawActivePotionEffects(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        var effects = minecraft.player.getActiveEffects();
        if (effects.isEmpty()) {
            return;
        }
        int x = leftPos - 124;
        int y = topPos;
        int entryHeight = effects.size() > 5 ? 132 / (effects.size() - 1) : 33;
        for (var instance : effects) {
            graphics.blit(INVENTORY, x, y, 0, 166, 140, 32);
            var sprite = minecraft.getMobEffectTextures().get(instance.getEffect());
            graphics.blit(x + 6, y + 7, 0, 18, 18, sprite);
            var name = Component.translatable(instance.getDescriptionId());
            int amp = instance.getAmplifier();
            if (amp >= 1 && amp <= 3) {
                name = name.copy().append(" ").append(Component.translatable("enchantment.level." + (amp + 1)));
            }
            graphics.drawString(font, name, x + 28, y + 6, 0xFFFFFF);
            float tickRate = minecraft.level != null ? minecraft.level.tickRateManager().tickrate() : 20.0F;
            graphics.drawString(font, net.minecraft.world.effect.MobEffectUtil.formatDuration(instance, 1.0F, tickRate),
                    x + 28, y + 16, 0x7F7F7F);
            y += entryHeight;
        }
    }

    private List<IconButton> buttons() {
        List<IconButton> list = new ArrayList<>();
        Slot first = menu.getSlot(0);
        int moduleX = first.x + 5 * 18;
        int moduleY = first.y - 33;
        int x = first.x + 2;
        int moveY = first.y + 55;
        int modeY = first.y - 11;
        ItemStack bag = menu.getBag();
        int pickup = bag.getItem() instanceof ItemHandyBag handy ? handy.getPickupMode(bag) : 0;
        int restock = ItemHandyBag.getRestockMode(bag);
        int shift = ItemHandyBag.getShiftMode(bag);

        for (int i = 0; i < HandyBagMenu.MODULE_SLOTS; i++) {
            list.add(new IconButton(moduleX + 4 + i * 18, moduleY + 19, 8, 8, 0, 0,
                    ItemHandyBag.GUI_ACTION_SELECT_MODULE, i));
        }
        for (int i = 0; i < 6; i++) {
            String[] tips = i < 2
                    ? new String[] {MOVE_TIPS[i], "enderutilities.gui.label.holdshifttoincludehotbar"}
                    : new String[] {MOVE_TIPS[i]};
            list.add(new IconButton(x + MOVE_X[i], moveY, 12, 12, 24, i * 12,
                    ItemHandyBag.GUI_ACTION_MOVE_ITEMS, i, tips));
        }

        list.add(new IconButton(x - 1, modeY, 8, 8, 0, ItemHandyBag.isLocked(bag) ? 48 : 0,
                ItemHandyBag.GUI_ACTION_TOGGLE_MODES, 0,
                ItemHandyBag.isLocked(bag) ? "enderutilities.gui.label.bag.disabled" : "enderutilities.gui.label.bag.enabled"));
        list.add(new IconButton(x + 11, modeY, 8, 8, 0, restock == 0 ? 40 : restock == 1 ? 80 : 72,
                ItemHandyBag.GUI_ACTION_TOGGLE_MODES, 2, restockTip(restock)));
        list.add(new IconButton(x + 23, modeY, 8, 8, 0, pickup == 0 ? 40 : pickup == 1 ? 56 : 64,
                ItemHandyBag.GUI_ACTION_TOGGLE_MODES, 1, pickupTip(pickup)));
        list.add(new IconButton(x + 35, modeY, 8, 8, 0, shift == 0 ? 16 : shift == 1 ? 96 : 104,
                ItemHandyBag.GUI_ACTION_TOGGLE_SHIFTCLICK, 0, SHIFT_TIPS[Math.max(0, Math.min(2, shift))]));

        if (dev.celli.enderutilities.compat.ModCompat.curiosLoaded()) {
            list.add(new IconButton(68 + xOff(), 15, 10, 10, 120, 0,
                    ItemHandyBag.GUI_ACTION_OPEN_CURIOS, 0, "curios"));
        }
        if (menu.getBagTier() == 0) {
            list.add(new IconButton(x + 74, modeY, 8, 8, 0, 24,
                    ItemHandyBag.GUI_ACTION_SORT_ITEMS, 0, "enderutilities.gui.label.sortitems"));
            list.add(new IconButton(x + 74, modeY + 70, 8, 8, 0, 24,
                    ItemHandyBag.GUI_ACTION_SORT_ITEMS, 3, "enderutilities.gui.label.sortitems.player"));
            list.add(new IconButton(x + 62, modeY, 8, 8, 0, ItemHandyBag.isSectionMasked(bag, 0, 1) ? 88 : 40,
                    ItemHandyBag.GUI_ACTION_TOGGLE_UPDATE, 0, updateTip(bag, 0)));
        } else {
            list.add(new IconButton(x - 17, modeY, 8, 8, 0, 24,
                    ItemHandyBag.GUI_ACTION_SORT_ITEMS, 1, "enderutilities.gui.label.sortitems"));
            list.add(new IconButton(x + 53, modeY, 8, 8, 0, 24,
                    ItemHandyBag.GUI_ACTION_SORT_ITEMS, 0, "enderutilities.gui.label.sortitems"));
            list.add(new IconButton(x + 165, modeY, 8, 8, 0, 24,
                    ItemHandyBag.GUI_ACTION_SORT_ITEMS, 2, "enderutilities.gui.label.sortitems"));
            list.add(new IconButton(x + 74, modeY + 70, 8, 8, 0, 24,
                    ItemHandyBag.GUI_ACTION_SORT_ITEMS, 3, "enderutilities.gui.label.sortitems.player"));
            int[] lockX = {x + 65, x - 29, x + 177};
            int[] updateX = {x + 77, x - 41, x + 189};
            for (int i = 0; i < 3; i++) {
                list.add(new IconButton(lockX[i], modeY, 8, 8, 0, ItemHandyBag.isSectionMasked(bag, i, 0) ? 40 : 0,
                        ItemHandyBag.GUI_ACTION_TOGGLE_BLOCK, i, lockTip(bag, i)));
                list.add(new IconButton(updateX[i], modeY, 8, 8, 0, ItemHandyBag.isSectionMasked(bag, i, 1) ? 88 : 40,
                        ItemHandyBag.GUI_ACTION_TOGGLE_UPDATE, i, updateTip(bag, i)));
            }
        }
        return list;
    }

    private static String pickupTip(int pickup) {
        return pickup == 2 ? "enderutilities.gui.label.pickupmode.all"
                : pickup == 1 ? "enderutilities.gui.label.pickupmode.matching"
                : "enderutilities.gui.label.pickupmode.disabled";
    }

    private static String restockTip(int restock) {
        return restock == 2 ? "enderutilities.gui.label.restockmode.all"
                : restock == 1 ? "enderutilities.gui.label.restockmode.hotbar"
                : "enderutilities.gui.label.restockmode.disabled";
    }

    private static String updateTip(ItemStack bag, int section) {
        return ItemHandyBag.isSectionMasked(bag, section, 1)
                ? "enderutilities.gui.label.updateitems.enabled"
                : "enderutilities.gui.label.updateitems.disabled";
    }

    private static String lockTip(ItemStack bag, int section) {
        return ItemHandyBag.isSectionMasked(bag, section, 0)
                ? "enderutilities.gui.label.blockquickactions.enabled"
                : "enderutilities.gui.label.blockquickactions.disabled";
    }

    private static void send(int action, int element) {
        PacketDistributor.sendToServer(new ItemGuiActionPayload(action, element));
    }

    private static boolean hit(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }

    private record IconButton(int x, int y, int w, int h, int u, int v, int action, int element, String... tips) {
    }
}
