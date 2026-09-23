package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.menu.SoundBlockMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;

/**
 * Orig {@code GuiSoundBlock}: 176x256, search/list, play/stop/repeat/pitch/volume widgets.
 * Clicks map to the port BE actions (0 volume, 1 pitch, 2 repeat, 3 select, 4 play).
 */
public class SoundBlockScreen extends MachineScreen<SoundBlockMenu> {
    private static final int LIST_X = 8;
    private static final int LIST_Y = 38;
    private static final int LIST_W = 142;
    private static final int LIST_H = 98;
    private static final int VISIBLE = 10;

    private final List<ResourceLocation> allSounds = new ArrayList<>();
    private final List<ResourceLocation> filtered = new ArrayList<>();
    private EditBox searchField;
    private int startIndex;

    public SoundBlockScreen(SoundBlockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "textures/gui/sound_block.png", 256, menu.getSoundBlock().getBlockPos());
        this.titleLabelY = 6;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        allSounds.clear();
        BuiltInRegistries.SOUND_EVENT.forEach(event -> {
            ResourceLocation id = BuiltInRegistries.SOUND_EVENT.getKey(event);
            if (id != null) {
                allSounds.add(id);
            }
        });
        searchField = new EditBox(font, leftPos + 8, topPos + 26, 141, 12, Component.empty());
        searchField.setBordered(false);
        searchField.setMaxLength(50);
        searchField.setTextColor(0xFFFFFF);
        searchField.setResponder(value -> applyFilter());
        addRenderableWidget(searchField);
        applyFilter();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("enderutilities.container.sound_block"), 8, 6, 0x404040, false);
        int max = Math.min(filtered.size() - 1, startIndex + VISIBLE - 1);
        int lineH = font.lineHeight + 1;
        for (int i = 0, index = startIndex; index <= max && index >= 0; index++, i++) {
            ResourceLocation id = filtered.get(index);
            String text = id.toString();
            if (text.length() > 27) {
                text = text.substring(0, 27);
            }
            boolean selected = index == selectedIndex();
            graphics.drawString(font, text, LIST_X + 2, LIST_Y + 1 + i * lineH, selected ? 0xF0F0F0 : 0x202020, false);
        }
        ResourceLocation selected = selectedId();
        if (selected != null) {
            graphics.drawString(font, selected.toString(), 8, 140, 0x404040, false);
        }
        float pitch = menu.getData().get(1) / 100.0F;
        float volume = menu.getData().get(0) / 100.0F;
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.sound_block.pitch")
                .append(String.format(": %.3f", pitch)), 60, 152, 0x404040, false);
        graphics.drawString(font, Component.translatable("enderutilities.gui.label.sound_block.volume")
                .append(String.format(": %.3f", volume)), 60, 163, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int barH = Math.max(8, LIST_H / Math.max(1, filtered.size() - VISIBLE + 1));
        int maxScroll = Math.max(0, filtered.size() - VISIBLE);
        int barY = maxScroll == 0 ? 0 : startIndex * (LIST_H - barH) / maxScroll;
        graphics.blit(guiTexture(), leftPos + 152, topPos + 38 + barY, 176, 0, 12, 12);
        for (GuiWidgets.IconButton button : chromeButtons()) {
            GuiWidgets.render(graphics, button, leftPos, topPos, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        int sign = button == 1 ? -1 : 1;
        if (button == 1 && searchField != null && searchField.isMouseOver(mouseX, mouseY)) {
            searchField.setValue("");
            applyFilter();
            return true;
        }
        if (x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y && y < LIST_Y + LIST_H) {
            int index = (VISIBLE * (y - LIST_Y) / LIST_H) + startIndex;
            if (index >= 0 && index < filtered.size()) {
                sendAction(3, index - selectedIndex());
                return true;
            }
        }
        for (Chrome chrome : chrome()) {
            if (!GuiWidgets.hit(x, y, chrome.button())) {
                continue;
            }
            if (chrome.action() < 0) {
                return true;
            }
            if (chrome.action() == 20) {
                if (searchField != null) {
                    searchField.setValue("");
                    applyFilter();
                }
                return true;
            }
            int amount = sign;
            if ((chrome.action() == 0 || chrome.action() == 1) && chrome.action() != 2) {
                if (hasShiftDown()) {
                    amount *= 10;
                }
                if (hasControlDown()) {
                    amount *= 100;
                }
            }
            sendAction(chrome.action(), amount);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (x >= LIST_X && x < 164 && y >= LIST_Y && y < LIST_Y + LIST_H && scrollY != 0) {
            startIndex = Math.max(0, Math.min(Math.max(0, filtered.size() - VISIBLE), startIndex - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField != null && searchField.keyPressed(keyCode, scanCode, modifiers)) {
            applyFilter();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchField != null && searchField.charTyped(codePoint, modifiers)) {
            applyFilter();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (GuiWidgets.IconButton button : chromeButtons()) {
            if (GuiWidgets.tooltip(graphics, font, button, leftPos, topPos, mouseX, mouseY)) {
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void applyFilter() {
        String filter = searchField == null ? "" : searchField.getValue();
        filtered.clear();
        if (filter.isEmpty()) {
            filtered.addAll(allSounds);
        } else {
            for (ResourceLocation id : allSounds) {
                if (id.toString().contains(filter)) {
                    filtered.add(id);
                }
            }
        }
        startIndex = Math.max(0, Math.min(startIndex, Math.max(0, filtered.size() - VISIBLE)));
    }

    private int selectedIndex() {
        ResourceLocation selected = selectedId();
        if (selected == null) {
            return -1;
        }
        return filtered.indexOf(selected);
    }

    private ResourceLocation selectedId() {
        int index = menu.getData().get(3);
        int i = 0;
        for (SoundEvent event : BuiltInRegistries.SOUND_EVENT) {
            if (i++ == index) {
                return BuiltInRegistries.SOUND_EVENT.getKey(event);
            }
        }
        return null;
    }

    private List<GuiWidgets.IconButton> chromeButtons() {
        List<GuiWidgets.IconButton> list = new ArrayList<>();
        for (Chrome chrome : chrome()) {
            list.add(chrome.button());
        }
        return list;
    }

    private List<Chrome> chrome() {
        boolean repeat = menu.getData().get(2) != 0;
        List<Chrome> list = new ArrayList<>();
        list.add(new Chrome(GuiWidgets.IconButton.widgets(38, 152, 8, 8, 0, repeat ? 88 : 40,
                repeat
                        ? "enderutilities.gui.label.sound_block.repeat.enabled"
                        : "enderutilities.gui.label.sound_block.repeat.disabled"), 2));
        list.add(new Chrome(GuiWidgets.IconButton.widgets(49, 152, 8, 8, 0, 120,
                "enderutilities.gui.label.sound_block.pitch"), 1));
        list.add(new Chrome(GuiWidgets.IconButton.widgets(49, 163, 8, 8, 0, 120,
                "enderutilities.gui.label.sound_block.volume"), 0));
        list.add(new Chrome(new GuiWidgets.IconButton(8, 152, 12, 12, 176, 0, 12, 0, guiTexture(),
                "enderutilities.gui.label.sound_block.play"), 4));
        list.add(new Chrome(new GuiWidgets.IconButton(23, 152, 12, 12, 176, 24, 12, 0, guiTexture(),
                "enderutilities.gui.label.sound_block.stop"), -1));
        list.add(new Chrome(new GuiWidgets.IconButton(152, 24, 12, 12, 176, 48, 12, 0, guiTexture(),
                "enderutilities.gui.label.sound_block.clear"), 20));
        return list;
    }

    private record Chrome(GuiWidgets.IconButton button, int action) {
    }
}
