package dev.celli.enderutilities.item;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.blockentity.AsuBlockEntity;
import dev.celli.enderutilities.blockentity.DrawbridgeBlockEntity;
import dev.celli.enderutilities.blockentity.InserterBlockEntity;
import dev.celli.enderutilities.data.PlacementPropertyValues;
import dev.celli.enderutilities.item.module.IKeyBound;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Orig {@code ItemBlockPlacementProperty}: G cycles properties while held,
 * values are applied to the block entity on place.
 */
public class ItemBlockPlacementProperty extends ItemBlockEnderUtilities implements IKeyBound {
    private final List<Property> properties = new ArrayList<>();

    public ItemBlockPlacementProperty(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemBlockPlacementProperty asu(Block block) {
        ItemBlockPlacementProperty item = new ItemBlockPlacementProperty(block, new Properties());
        item.add("asu.stack_limit", 1, AsuBlockEntity.MAX_STACK_SIZE);
        item.add("asu.slots", 1, AsuBlockEntity.MAX_INV_SIZE);
        return item;
    }

    public static ItemBlockPlacementProperty drawbridge(Block block, boolean advanced) {
        ItemBlockPlacementProperty item = new ItemBlockPlacementProperty(block, new Properties());
        int maxLength = advanced ? DrawbridgeBlockEntity.MAX_LENGTH_ADVANCED : DrawbridgeBlockEntity.MAX_LENGTH_NORMAL;
        item.add("drawbridge.delay", 1, 255);
        item.add("drawbridge.length", 1, maxLength);
        item.add("drawbridge.redstone_mode", 0, 2, "extend", "retract", "toggle");
        return item;
    }

    public static ItemBlockPlacementProperty inserter(Block block) {
        ItemBlockPlacementProperty item = new ItemBlockPlacementProperty(block, new Properties());
        item.add("inserter.delay", 0, 72000);
        item.add("inserter.redstone_mode", 0, 2, "ignored", "low", "high");
        item.add("inserter.stack_limit", 1, 64);
        return item;
    }

    public ItemBlockPlacementProperty add(String key, int min, int max, String... names) {
        properties.add(new Property(key, min, max, names.length == 0 ? null : names));
        return this;
    }

    public boolean hasPlacementProperty(ItemStack stack) {
        return !properties.isEmpty();
    }

    public List<Property> properties() {
        return properties;
    }

    public static PlacementPropertyValues values(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PLACEMENT_PROPERTIES, PlacementPropertyValues.EMPTY);
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (properties.isEmpty()) {
            return false;
        }
        PlacementPropertyValues stored = values(stack);
        int index = Mth.clamp(stored.index(), 0, properties.size() - 1);
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE) || EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT)) {
            index += EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT) ? -1 : 1;
            if (index < 0) {
                index = properties.size() - 1;
            } else if (index >= properties.size()) {
                index = 0;
            }
            stack.set(ModDataComponents.PLACEMENT_PROPERTIES, stored.withIndex(index));
            return true;
        }
        Property property = properties.get(index);
        if (EnumKey.SCROLL.matches(key, HotKeys.MOD_NONE)
                || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL)
                || EnumKey.SCROLL.matches(key, HotKeys.MOD_ALT)
                || EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT_CTRL)
                || EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT_ALT)
                || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL_ALT)
                || EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT_CTRL_ALT)) {
            if (EnumKey.keypressContainsShift(key)
                    && !EnumKey.keypressContainsControl(key)
                    && !EnumKey.keypressContainsAlt(key)) {
                return false;
            }
            int change = EnumKey.keypressActionIsReversed(key) ? -1 : 1;
            if (EnumKey.keypressContainsShift(key)) {
                change *= 10;
            }
            if (EnumKey.keypressContainsControl(key)) {
                change *= EnumKey.keypressContainsAlt(key) ? 100 : 10;
            }
            int value = stored.get(property.key, property.min) + change;
            if (value < property.min) {
                value = property.max;
            }
            if (value > property.max) {
                value = property.min;
            }
            stack.set(ModDataComponents.PLACEMENT_PROPERTIES, stored.withValue(property.key, value));
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand().copy();
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !context.getLevel().isClientSide) {
            applyTo(context.getLevel().getBlockEntity(context.getClickedPos()), stack);
        }
        return result;
    }

    public static void applyTo(BlockEntity be, ItemStack stack) {
        if (be == null || !stack.has(ModDataComponents.PLACEMENT_PROPERTIES)) {
            return;
        }
        PlacementPropertyValues values = values(stack);
        if (be instanceof AsuBlockEntity asu) {
            asu.applyPlacementProperties(values.get("asu.stack_limit", asu.getInventory().getStackLimit()),
                    values.get("asu.slots", asu.getInventory().getVisibleSlots()));
        } else if (be instanceof DrawbridgeBlockEntity drawbridge) {
            drawbridge.applyPlacementProperties(
                    values.get("drawbridge.delay", 4),
                    values.get("drawbridge.length", 1),
                    values.get("drawbridge.redstone_mode", 0));
        } else if (be instanceof InserterBlockEntity inserter) {
            inserter.applyPlacementProperties(
                    values.get("inserter.delay", 4),
                    values.get("inserter.redstone_mode", 0),
                    values.get("inserter.stack_limit", 1));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!properties.isEmpty()) {
            EnderItemTooltips.addTranslatedTips("enderutilities.tooltip.placementproperties.tooltips", tooltip,
                    ClientAccess.isShiftDown());
        }
    }

    public List<Component> hudLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        PlacementPropertyValues stored = values(stack);
        int selected = Mth.clamp(stored.index(), 0, Math.max(0, properties.size() - 1));
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            int value = stored.get(property.key, property.min);
            String prefix = i == selected ? "> " : "  ";
            Component name = Component.translatable("enderutilities.placement_properties." + property.key);
            Component valueText;
            if (property.names != null && value >= 0 && value < property.names.length) {
                valueText = Component.translatable(
                        "enderutilities.placement_properties.valuenames." + property.key + "." + property.names[value]);
            } else {
                valueText = Component.literal(String.valueOf(value));
            }
            lines.add(Component.literal(prefix).append(name).append(": ")
                    .append(valueText.copy().withStyle(ChatFormatting.GREEN)));
        }
        return lines;
    }

    public record Property(String key, int min, int max, String[] names) {
    }
}
