package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.menu.InserterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Orig {@code GuiInserter}: stack/delay widgets, redstone + filter state icons, orig hover keys.
 */
public class InserterScreen extends MachineScreen<InserterMenu> {
    public InserterScreen(InserterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title,
                menu.getInserter().isFiltered() ? "textures/gui/inserter_filtered.png" : "textures/gui/inserter_normal.png",
                menu.getInserter().isFiltered() ? 197 : 141,
                menu.getInserter().getBlockPos());
        this.titleLabelY = 6;
        this.inventoryLabelY = menu.getInserter().isFiltered() ? 105 : 49;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        String stackLimit = String.valueOf(menu.getData().get(1));
        String delay = String.valueOf(menu.getData().get(0));
        graphics.drawString(font, stackLimit, 134 - font.width(stackLimit), 22, 0x404040, false);
        graphics.drawString(font, delay, 134 - font.width(delay), 36, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        for (ButtonSpec spec : specs()) {
            GuiWidgets.render(graphics, spec.button(), leftPos, topPos, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        int sign = button == 1 ? -1 : 1;
        for (ButtonSpec spec : specs()) {
            if (!GuiWidgets.hit(x, y, spec.button())) {
                continue;
            }
            sendAction(spec.action(), spec.amount(button, sign));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (ButtonSpec spec : specs()) {
            if (GuiWidgets.tooltip(graphics, font, spec.button(), leftPos, topPos, mouseX, mouseY)) {
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private List<ButtonSpec> specs() {
        List<ButtonSpec> list = new ArrayList<>();
        list.add(new ButtonSpec(GuiWidgets.IconButton.widgets(138, 21, 8, 8, 0, 120,
                "enderutilities.gui.label.stacklimit"), 1, this::stackAmount));
        list.add(new ButtonSpec(GuiWidgets.IconButton.widgets(138, 35, 8, 8, 0, 120,
                "enderutilities.gui.label.delay.inserter"), 0, this::delayAmount));
        int redstone = menu.getData().get(3);
        int redstoneV = switch (redstone) {
            case 1 -> 196;
            case 2 -> 210;
            default -> 224;
        };
        String redstoneTip = switch (redstone) {
            case 1 -> "enderutilities.gui.label.redstone.low";
            case 2 -> "enderutilities.gui.label.redstone.high";
            default -> "enderutilities.gui.label.redstone.ignored";
        };
        list.add(new ButtonSpec(GuiWidgets.IconButton.widgets(153, 29, 14, 14, 60, redstoneV, redstoneTip),
                3, (button, sign) -> sign));
        if (menu.getInserter().isFiltered()) {
            int mask = menu.getData().get(2);
            list.add(new ButtonSpec(GuiWidgets.IconButton.widgets(9, 30, 14, 14, 60, (mask & 0x01) != 0 ? 42 : 56,
                    (mask & 0x01) != 0
                            ? "enderutilities.gui.label.inserter.filter.input"
                            : "enderutilities.gui.label.inserter.filter.output"),
                    2, (button, sign) -> 1));
            list.add(new ButtonSpec(GuiWidgets.IconButton.widgets(27, 30, 14, 14, 60, (mask & 0x02) != 0 ? 84 : 70,
                    (mask & 0x02) != 0
                            ? "enderutilities.gui.label.whitelist"
                            : "enderutilities.gui.label.blacklist"),
                    2, (button, sign) -> 2));
            list.add(new ButtonSpec(GuiWidgets.IconButton.widgets(45, 30, 14, 14, 60, (mask & 0x04) != 0 ? 112 : 126,
                    (mask & 0x04) != 0
                            ? "enderutilities.gui.label.meta.match"
                            : "enderutilities.gui.label.meta.ignore"),
                    2, (button, sign) -> 4));
            list.add(new ButtonSpec(GuiWidgets.IconButton.widgets(63, 30, 14, 14, 60, (mask & 0x08) != 0 ? 140 : 154,
                    (mask & 0x08) != 0
                            ? "enderutilities.gui.label.nbt.match"
                            : "enderutilities.gui.label.nbt.ignore"),
                    2, (button, sign) -> 8));
        }
        return list;
    }

    private int stackAmount(int button, int sign) {
        if (button == 2) {
            return hasShiftDown() ? 64 : -64;
        }
        int amount = sign;
        if (hasShiftDown()) {
            amount *= 16;
        } else if (hasControlDown()) {
            amount *= 64;
        }
        return amount;
    }

    private int delayAmount(int button, int sign) {
        if (button == 2) {
            return hasShiftDown() ? 72000 : -72000;
        }
        int amount = sign;
        if (hasShiftDown() && hasControlDown()) {
            amount *= 1000;
        } else if (hasShiftDown()) {
            amount *= 10;
        } else if (hasControlDown()) {
            amount *= 100;
        }
        return amount;
    }

    private record ButtonSpec(GuiWidgets.IconButton button, int action, Amount amount) {
        int amount(int mouseButton, int sign) {
            return amount.resolve(mouseButton, sign);
        }
    }

    @FunctionalInterface
    private interface Amount {
        int resolve(int mouseButton, int sign);
    }
}
