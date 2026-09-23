package dev.celli.enderutilities.client;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Orig {@code GuiButtonIcon} / {@code GuiButtonHoverText}: widget UV plus
 * {@code getHoverState} (0 disabled, 1 normal, 2 hovered) times the hover offset.
 */
public final class GuiWidgets {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "textures/gui/gui.widgets.png");

    private GuiWidgets() {
    }

    public record IconButton(
            int x,
            int y,
            int w,
            int h,
            int u,
            int v,
            int hoverOffsetU,
            int hoverOffsetV,
            ResourceLocation texture,
            String... tips
    ) {
        public static IconButton widgets(int x, int y, int w, int h, int u, int v, int hoverOffset, String... tips) {
            return new IconButton(x, y, w, h, u, v, hoverOffset, 0, TEXTURE, tips);
        }

        public static IconButton widgets(int x, int y, int w, int h, int u, int v, String... tips) {
            return widgets(x, y, w, h, u, v, w, tips);
        }
    }

    public static void render(GuiGraphics graphics, IconButton button, int left, int top, int mouseX, int mouseY) {
        render(graphics, button, left, top, mouseX, mouseY, true);
    }

    public static void render(
            GuiGraphics graphics,
            IconButton button,
            int left,
            int top,
            int mouseX,
            int mouseY,
            boolean enabled
    ) {
        boolean hovered = enabled && hit(mouseX - left, mouseY - top, button);
        int state = !enabled ? 0 : hovered ? 2 : 1;
        graphics.blit(
                button.texture(),
                left + button.x(),
                top + button.y(),
                button.u() + state * button.hoverOffsetU(),
                button.v() + state * button.hoverOffsetV(),
                button.w(),
                button.h()
        );
    }

    public static boolean hit(int x, int y, IconButton button) {
        return x >= button.x() && x < button.x() + button.w() && y >= button.y() && y < button.y() + button.h();
    }

    public static boolean tooltip(GuiGraphics graphics, Font font, IconButton button, int left, int top, int mouseX, int mouseY) {
        if (!hit(mouseX - left, mouseY - top, button) || button.tips().length == 0) {
            return false;
        }
        List<Component> lines = new ArrayList<>();
        for (String tip : button.tips()) {
            lines.addAll(EnderItemTooltips.splitTranslated(tip));
        }
        if (lines.isEmpty()) {
            return false;
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        return true;
    }
}
