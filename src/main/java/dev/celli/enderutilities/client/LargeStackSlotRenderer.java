package dev.celli.enderutilities.client;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.celli.enderutilities.util.EUStringUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Orig {@code GuiContainerLargeStacks#renderLargeStackItemOverlayIntoGUI}: 0.5x
 * stack count so 256/1024/4096 stay readable.
 */
public final class LargeStackSlotRenderer {
    private LargeStackSlotRenderer() {
    }

    public static boolean isLargeStackSlot(Slot slot, int first, int count) {
        return slot.index >= first && slot.index < first + count;
    }

    public static void renderOverlay(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        if (stack.isEmpty() || stack.getCount() == 1) {
            return;
        }
        String text = EUStringUtils.getStackSizeString(stack, 4);
        if (stack.getCount() < 1) {
            text = ChatFormatting.RED + text;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 200.0F);
        pose.scale(0.5F, 0.5F, 0.5F);
        graphics.drawString(font, text, 31 - font.width(text), 23, 0xFFFFFF, true);
        pose.popPose();
    }
}
