package dev.celli.enderutilities.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Orig {@code NBTUtils.getCachedInventoryStrings} / {@code ItemBlockStorage}:
 * list stored stacks on JSU/MSU/ASU/barrel item tooltips.
 */
public final class StorageContentsTooltip {
    private StorageContentsTooltip() {
    }

    public static void add(ItemStack stack, HolderLookup.Provider registries, List<Component> tooltip, int maxLines) {
        List<StoredLine> lines = read(stack, registries);
        if (lines.isEmpty()) {
            return;
        }
        int totalStacks = lines.size();
        long totalItems = 0;
        for (StoredLine line : lines) {
            totalItems += line.count;
        }
        tooltip.add(Component.translatable("enderutilities.tooltip.item.memorycard.items.stackcount",
                totalStacks, EUStringUtils.formatNumberWithKSeparators(totalItems)));
        int shown = Math.min(lines.size(), maxLines);
        for (int i = 0; i < shown; i++) {
            StoredLine line = lines.get(i);
            tooltip.add(Component.literal("  ")
                    .append(Component.literal(EUStringUtils.formatNumberWithKSeparators(line.count))
                            .withStyle(ChatFormatting.WHITE))
                    .append(" ")
                    .append(Component.literal(line.name).withStyle(ChatFormatting.GRAY)));
        }
        if (totalStacks > maxLines) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.andmorestacksnotlisted",
                    ChatFormatting.WHITE.toString(), totalStacks - maxLines, ChatFormatting.RESET.toString() + ChatFormatting.GRAY));
        }
    }

    public static Component displayName(ItemStack stack, HolderLookup.Provider registries, Component base) {
        List<StoredLine> lines = read(stack, registries);
        if (lines.isEmpty()) {
            return base;
        }
        if (lines.size() == 1) {
            StoredLine line = lines.getFirst();
            return Component.literal(base.getString() + " - ")
                    .append(Component.literal(line.name).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" (" + EUStringUtils.formatNumber(line.count, 9999, 4) + ")")
                            .withStyle(ChatFormatting.WHITE));
        }
        return Component.literal(base.getString() + " (" + lines.size() + " ")
                .append(Component.translatable("enderutilities.tooltip.item.stacks"))
                .append(")");
    }

    private static List<StoredLine> read(ItemStack stack, HolderLookup.Provider registries) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return List.of();
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains("Inventory", Tag.TAG_COMPOUND)) {
            return List.of();
        }
        ListTag items = tag.getCompound("Inventory").getList("Items", Tag.TAG_COMPOUND);
        List<StoredLine> lines = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            Optional<ItemStack> parsed = ItemStack.parse(registries, itemTag);
            if (parsed.isEmpty() || parsed.get().isEmpty()) {
                continue;
            }
            ItemStack stored = parsed.get();
            lines.add(new StoredLine(stored.getHoverName().getString(), stored.getCount()));
        }
        return lines;
    }

    private record StoredLine(String name, int count) {
    }
}
