package dev.celli.enderutilities.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.data.TargetLocation;
import dev.celli.enderutilities.item.ItemEnderCapacitor;
import dev.celli.enderutilities.item.ItemLinkCrystal;
import dev.celli.enderutilities.item.ItemLocationBoundModular;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EnderItemTooltips {
    private static final Pattern LINE_BREAK = Pattern.compile(Pattern.quote("|lf"));

    private EnderItemTooltips() {
    }

    public static void addTranslatedTips(String key, List<Component> tooltip, boolean verbose) {
        List<Component> lines = splitTranslated(key);
        if (!verbose && lines.size() > 2) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.holdshiftfordescription"));
        } else {
            tooltip.addAll(lines);
        }
    }

    public static void addFoldedLines(List<Component> tooltip, List<Component> lines) {
        boolean verbose = ClientAccess.isShiftDown();
        if (!verbose && lines.size() > 2) {
            if (!lines.isEmpty()) {
                tooltip.add(lines.getFirst());
            }
            tooltip.add(Component.translatable("enderutilities.tooltip.item.holdshift"));
        } else {
            tooltip.addAll(lines);
        }
    }

    public static List<Component> splitTranslated(String key) {
        String translated = Component.translatable(key).getString();
        List<Component> lines = new ArrayList<>();
        if (translated.equals(key)) {
            return lines;
        }
        for (String line : LINE_BREAK.split(translated)) {
            lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    public static void addModularTooltips(dev.celli.enderutilities.item.module.IModular item, ItemStack stack, List<Component> tooltip) {
        List<Component> verboseLines = new ArrayList<>();
        addModularLines(item, stack, verboseLines, true);
        if (!ClientAccess.isShiftDown() && verboseLines.size() > 2) {
            List<Component> compact = new ArrayList<>();
            addModularLines(item, stack, compact, false);
            if (!compact.isEmpty()) {
                tooltip.add(compact.getFirst());
            }
            tooltip.add(Component.translatable("enderutilities.tooltip.item.holdshift"));
        } else {
            tooltip.addAll(verboseLines);
        }
    }

    public static void addModularLines(dev.celli.enderutilities.item.module.IModular item, ItemStack stack, List<Component> list, boolean verbose) {
        if (ModularHelper.data(stack).modules().isEmpty()) {
            list.add(Component.translatable("enderutilities.tooltip.item.usetoolworkstation").withStyle(ChatFormatting.GRAY));
            return;
        }

        ItemStack linkCrystal = ModularHelper.getSelectedModuleStack(stack, ModuleType.LINKCRYSTAL);
        ChatFormatting blue = ChatFormatting.BLUE;

        if (!linkCrystal.isEmpty()) {
            TargetLocation target = linkCrystal.get(ModDataComponents.TARGET);
            if (target != null) {
                addTargetLines(linkCrystal, target, list, verbose);
            } else {
                list.add(Component.translatable("enderutilities.tooltip.item.notargetset").withStyle(ChatFormatting.GRAY));
            }

            if (verbose) {
                int sel = ModularHelper.getClampedModuleSelection(stack, ModuleType.LINKCRYSTAL) + 1;
                int num = ModularHelper.getInstalledModuleCount(stack, ModuleType.LINKCRYSTAL);
                MutableComponent line = Component.translatable("enderutilities.tooltip.item.selectedlinkcrystal.short")
                        .withStyle(ChatFormatting.GRAY)
                        .append(" ");
                if (linkCrystal.has(DataComponents.CUSTOM_NAME)) {
                    line.append(Component.literal(linkCrystal.getHoverName().getString())
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC))
                            .append(Component.literal(" ").withStyle(ChatFormatting.GRAY));
                }
                line.append(Component.literal("(").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(sel)).withStyle(blue))
                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(num)).withStyle(blue))
                        .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
                list.add(line);
            }
        } else if (ModularHelper.getInstalledModuleCount(stack, ModuleType.LINKCRYSTAL) > 0) {
            if (verbose) {
                int num = ModularHelper.getInstalledModuleCount(stack, ModuleType.LINKCRYSTAL);
                list.add(Component.translatable("enderutilities.tooltip.item.selectedlinkcrystal.short")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("-").withStyle(blue))
                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(num)).withStyle(blue))
                        .append(Component.literal(")").withStyle(ChatFormatting.GRAY)));
            }
        } else {
            list.add(Component.translatable("enderutilities.tooltip.item.nolinkcrystals").withStyle(ChatFormatting.GRAY));
        }

        if (verbose) {
            if (item.getMaxModules(stack, ModuleType.MOBPERSISTENCE) > 0) {
                boolean jailer = ModularHelper.getInstalledModuleCount(stack, ModuleType.MOBPERSISTENCE) > 0;
                list.add(yesNoLine("enderutilities.tooltip.item.jailer", jailer));
            }
            if (item instanceof ItemLocationBoundModular bound && bound.useBindLocking(stack)) {
                list.add(yesNoLine("enderutilities.tooltip.item.bindlocked", bound.isBindLocked(stack)));
            }
            addChargeLine(ModularHelper.getSelectedModuleStack(stack, ModuleType.ENDERCAPACITOR), list);
        }
    }

    public static void addEnderChestBagLines(ItemStack stack, List<Component> tooltip) {
        TargetLocation target = ModularHelper.getSelectedTarget(stack);
        if (target != null && target.isEnderChest()) {
            List<Component> lines = new ArrayList<>();
            String name = blockDisplayName(target);
            if (name != null) {
                lines.add(Component.translatable("enderutilities.tooltip.item.target")
                        .withStyle(ChatFormatting.GRAY)
                        .append(": ")
                        .append(Component.literal(name).withStyle(ChatFormatting.DARK_GREEN)));
            }
            addChargeLine(ModularHelper.getSelectedModuleStack(stack, ModuleType.ENDERCAPACITOR), lines);
            addFoldedLines(tooltip, lines);
            return;
        }
        addModularTooltips((dev.celli.enderutilities.item.module.IModular) stack.getItem(), stack, tooltip);
    }

    public static void addChargeLine(ItemStack capacitorStack, List<Component> list) {
        if (capacitorStack.getItem() instanceof ItemEnderCapacitor capacitor) {
            list.add(Component.translatable("enderutilities.tooltip.item.charge")
                    .withStyle(ChatFormatting.GRAY)
                    .append(": ")
                    .append(Component.literal(
                            EUStringUtils.formatNumberWithKSeparators(capacitor.getCharge(capacitorStack))
                                    + " / "
                                    + EUStringUtils.formatNumberWithKSeparators(capacitor.getCapacity(capacitorStack)))));
        }
    }

    public static void addTargetLines(ItemStack crystal, TargetLocation target, List<Component> list, boolean verbose) {
        Player player = ClientAccess.player();
        OwnerInfo owner = crystal.get(ModDataComponents.OWNER);
        if (owner != null && player != null && !owner.canAccess(player)) {
            addOwnerLines(owner, list, verbose);
            return;
        }

        String dimName = dimensionName(target.dimension());
        boolean showBlock = isBlockCrystal(crystal);
        String blockName = showBlock ? blockDisplayName(target) : null;
        if (showBlock && (blockName == null || blockName.isEmpty())) {
            showBlock = false;
        }

        if (verbose) {
            MutableComponent dim = Component.translatable("enderutilities.tooltip.dimension")
                    .withStyle(ChatFormatting.GRAY)
                    .append(": ")
                    .append(Component.literal(dimensionId(target.dimension())).withStyle(ChatFormatting.BLUE));
            if (!dimName.isEmpty()) {
                dim.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(dimName).withStyle(ChatFormatting.DARK_GREEN));
            }
            list.add(dim);
            list.add(coordLine(target));
            if (target.hasRotation()) {
                list.add(Component.literal("yaw: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(String.format("%.1f", target.yaw())).withStyle(ChatFormatting.BLUE))
                        .append(Component.literal(" pitch: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.format("%.1f", target.pitch())).withStyle(ChatFormatting.BLUE)));
            }
            if (showBlock) {
                list.add(Component.translatable("enderutilities.tooltip.item.target")
                        .withStyle(ChatFormatting.GRAY)
                        .append(": ")
                        .append(Component.literal(blockName).withStyle(ChatFormatting.DARK_GREEN)));
                list.add(Component.literal(String.format("%s Side: %s",
                        target.blockId().map(ResourceLocation::toString).orElse("unknown"),
                        target.face())).withStyle(ChatFormatting.GRAY));
            }
        } else {
            MutableComponent compact = Component.empty();
            if (showBlock) {
                compact.append(Component.literal(blockName).withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY));
            }
            if (dimName.isEmpty()) {
                compact.append(Component.translatable("enderutilities.tooltip.dimension.compact").withStyle(ChatFormatting.GRAY))
                        .append(": ")
                        .append(Component.literal(dimensionId(target.dimension())).withStyle(ChatFormatting.BLUE));
            } else {
                compact.append(Component.literal(dimName).withStyle(ChatFormatting.DARK_GREEN));
            }
            compact.append(Component.literal(" @ ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format("%.2f", target.exact().x)).withStyle(ChatFormatting.BLUE))
                    .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format("%.2f", target.exact().y)).withStyle(ChatFormatting.BLUE))
                    .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format("%.2f", target.exact().z)).withStyle(ChatFormatting.BLUE));
            list.add(compact);
        }

        addOwnerLines(owner, list, verbose);
    }

    public static String getTargetDisplayName(ItemStack container) {
        ItemStack crystal = ModularHelper.getSelectedModuleStack(container, ModuleType.LINKCRYSTAL);
        if (crystal.isEmpty()) {
            return null;
        }
        if (crystal.has(DataComponents.CUSTOM_NAME)) {
            return ChatFormatting.ITALIC + crystal.getHoverName().getString() + ChatFormatting.RESET;
        }
        TargetLocation target = crystal.get(ModDataComponents.TARGET);
        if (target == null) {
            return null;
        }
        if (crystal.getItem() instanceof ItemLinkCrystal link && link.crystalType() == ItemLinkCrystal.TYPE_LOCATION) {
            return dimensionName(target.dimension(), true);
        }
        return blockDisplayName(target);
    }

    public static String getCrystalTargetDisplayName(ItemStack crystal) {
        if (crystal.has(DataComponents.CUSTOM_NAME)) {
            return null;
        }
        TargetLocation target = crystal.get(ModDataComponents.TARGET);
        if (target == null) {
            return null;
        }
        if (crystal.getItem() instanceof ItemLinkCrystal link && link.crystalType() == ItemLinkCrystal.TYPE_LOCATION) {
            return dimensionName(target.dimension(), true);
        }
        return blockDisplayName(target);
    }

    public static Component namedWithTarget(Component baseName, String targetName) {
        if (targetName == null || targetName.isEmpty()) {
            return baseName;
        }
        String base = baseName.getString();
        Component left = base.length() >= 14
                ? Component.literal(EUStringUtils.getInitialsWithDots(base))
                : baseName;
        return Component.empty()
                .append(left)
                .append(" ")
                .append(Component.literal(targetName).withStyle(ChatFormatting.GREEN));
    }

    public static String blockDisplayName(TargetLocation target) {
        return target.blockId()
                .flatMap(BuiltInRegistries.BLOCK::getOptional)
                .map(block -> new ItemStack(block).getHoverName().getString())
                .orElse(null);
    }

    private static boolean isBlockCrystal(ItemStack stack) {
        return stack.getItem() instanceof IModule module
                && module.getModuleType(stack) == ModuleType.LINKCRYSTAL
                && module.getModuleTier(stack) == ItemLinkCrystal.TYPE_BLOCK;
    }

    private static Component yesNoLine(String labelKey, boolean value) {
        return Component.translatable(labelKey)
                .withStyle(ChatFormatting.GRAY)
                .append(": ")
                .append(Component.translatable(value ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static Component coordLine(TargetLocation target) {
        return Component.literal("x: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.2f", target.exact().x)).withStyle(ChatFormatting.BLUE))
                .append(Component.literal(" y: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.2f", target.exact().y)).withStyle(ChatFormatting.BLUE))
                .append(Component.literal(" z: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.2f", target.exact().z)).withStyle(ChatFormatting.BLUE));
    }

    private static void addOwnerLines(OwnerInfo owner, List<Component> list, boolean verbose) {
        if (owner == null) {
            return;
        }
        Component mode = Component.translatable(owner.isPublic()
                        ? "enderutilities.tooltip.item.public"
                        : "enderutilities.tooltip.item.private")
                .withStyle(owner.isPublic() ? ChatFormatting.GREEN : ChatFormatting.RED);
        if (verbose) {
            list.add(Component.translatable("enderutilities.tooltip.item.mode")
                    .withStyle(ChatFormatting.GRAY)
                    .append(": ")
                    .append(mode));
            list.add(Component.translatable("enderutilities.tooltip.item.owner")
                    .withStyle(ChatFormatting.GRAY)
                    .append(": ")
                    .append(Component.literal(owner.name()).withStyle(ChatFormatting.DARK_GREEN)));
        } else {
            list.add(Component.empty()
                    .append(mode)
                    .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(owner.name()).withStyle(ChatFormatting.DARK_GREEN)));
        }
    }

    private static String dimensionId(ResourceKey<Level> dim) {
        if (dim.equals(Level.OVERWORLD)) {
            return "0";
        }
        if (dim.equals(Level.NETHER)) {
            return "-1";
        }
        if (dim.equals(Level.END)) {
            return "1";
        }
        return dim.location().toString();
    }

    private static String dimensionName(ResourceKey<Level> dim, boolean useFallback) {
        if (dim.equals(Level.OVERWORLD)) {
            return "Overworld";
        }
        if (dim.equals(Level.NETHER)) {
            return "the Nether";
        }
        if (dim.equals(Level.END)) {
            return "the End";
        }
        return useFallback ? "DIM: " + dim.location() : "";
    }

    private static String dimensionName(ResourceKey<Level> dim) {
        return dimensionName(dim, false);
    }
}
