package dev.celli.enderutilities.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.data.RulerCardData;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EUStringUtils;
import dev.celli.enderutilities.util.EnderItemTooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemRuler extends ItemModular {
    public static final int FLAG_DISTANCE_DIFFERENCE = 0x1;
    public static final int FLAG_RENDER_UNSELECTED = 0x2;
    public static final int FLAG_RENDER_ALL = 0x4;

    private final Map<UUID, Long> lastClick = new HashMap<>();

    public ItemRuler(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int flags(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ITEM_MODE, 0);
    }

    public static void setFlags(ItemStack stack, int flags) {
        stack.set(ModDataComponents.ITEM_MODE, flags);
    }

    public static boolean distanceDifference(ItemStack stack) {
        return (flags(stack) & FLAG_DISTANCE_DIFFERENCE) != 0;
    }

    public static boolean renderWhenUnselected(ItemStack stack) {
        return (flags(stack) & FLAG_RENDER_UNSELECTED) != 0;
    }

    public static boolean renderAll(ItemStack stack) {
        return (flags(stack) & FLAG_RENDER_ALL) != 0;
    }

    public static ItemStack selectedCard(ItemStack ruler) {
        return ModularHelper.getSelectedModuleStack(ruler, ModuleType.MEMORY_CARD_MISC);
    }

    public static boolean hasMemoryCard(ItemStack ruler) {
        return !selectedCard(ruler).isEmpty();
    }

    public static RulerCardData cardData(ItemStack ruler) {
        ItemStack card = selectedCard(ruler);
        if (card.isEmpty()) {
            return RulerCardData.EMPTY;
        }
        return card.getOrDefault(ModDataComponents.RULER_CARD, RulerCardData.EMPTY);
    }

    public static void setCardData(ItemStack ruler, RulerCardData data) {
        ItemStack card = selectedCard(ruler);
        if (card.isEmpty()) {
            return;
        }
        card.set(ModDataComponents.RULER_CARD, data);
        ModularHelper.setSelectedModuleStack(ruler, ModuleType.MEMORY_CARD_MISC, card);
    }

    public static int locationSelection(ItemStack ruler) {
        return cardData(ruler).selected();
    }

    public static int locationCount(ItemStack ruler) {
        return cardData(ruler).spots().size();
    }

    public static boolean alwaysRenderLocation(ItemStack ruler, int index) {
        RulerCardData data = cardData(ruler);
        if (index < 0) {
            index = data.selected();
        }
        if (index < 0 || index >= data.spots().size()) {
            return false;
        }
        return data.spots().get(index).renderWithAll();
    }

    public static Optional<BlockPos> position(ItemStack ruler, int index, boolean start) {
        RulerCardData data = cardData(ruler);
        if (index < 0) {
            index = data.selected();
        }
        if (index < 0 || index >= data.spots().size()) {
            return Optional.empty();
        }
        RulerCardData.Spot spot = data.spots().get(index);
        return start ? spot.start() : spot.end();
    }

    public static Optional<BlockPos> start(ItemStack ruler) {
        return position(ruler, -1, true);
    }

    public static Optional<BlockPos> end(ItemStack ruler) {
        return position(ruler, -1, false);
    }

    /**
     * Writes the corner to the selected Misc Memory Card. Clicking the same stored
     * position again clears it. No card → orig no-op.
     */
    public static void setOrRemovePosition(ItemStack ruler, BlockPos pos, boolean startCorner, boolean adjust, Direction face) {
        if (!hasMemoryCard(ruler)) {
            return;
        }
        if (adjust && face != null) {
            pos = pos.relative(face);
        }
        pos = pos.immutable();
        RulerCardData data = cardData(ruler);
        RulerCardData.Spot current = data.current();
        Optional<BlockPos> old = startCorner ? current.start() : current.end();
        Optional<BlockPos> nextPos = old.filter(pos::equals).isPresent() ? Optional.empty() : Optional.of(pos);
        RulerCardData.Spot next = startCorner
                ? new RulerCardData.Spot(nextPos, current.end(), current.renderWithAll())
                : new RulerCardData.Spot(current.start(), nextPos, current.renderWithAll());
        setCardData(ruler, data.withCurrent(next));
    }

    private boolean debounce(Player player, Level level, int ticks) {
        Long last = lastClick.get(player.getUUID());
        long now = level.getGameTime();
        if (last != null && now - last < ticks) {
            lastClick.put(player.getUUID(), now);
            return true;
        }
        lastClick.put(player.getUUID(), now);
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide && !debounce(player, context.getLevel(), 6)) {
            setOrRemovePosition(context.getItemInHand(), context.getClickedPos(), false,
                    !player.isShiftKeyDown(), context.getClickedFace());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    public void onLeftClickBlock(Player player, Level level, ItemStack stack, BlockPos pos, Direction face) {
        if (level.isClientSide || debounce(player, level, 4)) {
            return;
        }
        setOrRemovePosition(stack, pos, true, !player.isShiftKeyDown(), face);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT)) {
            if (hasMemoryCard(stack)) {
                setCardData(stack, cardData(stack).cycle(EnumKey.keypressActionIsReversed(key)));
            }
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_ALT)) {
            setFlags(stack, flags(stack) ^ FLAG_RENDER_UNSELECTED);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL_ALT)) {
            setFlags(stack, flags(stack) ^ FLAG_DISTANCE_DIFFERENCE);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_ALT)) {
            setFlags(stack, flags(stack) ^ FLAG_RENDER_ALL);
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE)) {
            if (hasMemoryCard(stack)) {
                RulerCardData data = cardData(stack);
                RulerCardData.Spot current = data.current();
                setCardData(stack, data.withCurrent(new RulerCardData.Spot(
                        current.start(), current.end(), !current.renderWithAll())));
            }
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL)) {
            return ModularHelper.changeSelectedModule(
                    stack,
                    ModuleType.MEMORY_CARD_MISC,
                    EnumKey.keypressActionIsReversed(key) || EnumKey.keypressContainsShift(key)
            );
        }
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        String base = super.getName(stack).getString();
        if (base.length() >= 14) {
            base = EUStringUtils.getInitialsWithDots(base);
        }
        int count = locationCount(stack);
        if (count <= 0) {
            return Component.literal(base);
        }
        int sel = locationSelection(stack);
        boolean render = alwaysRenderLocation(stack, sel);
        MutableComponent name = Component.literal(base)
                .append(" - sel: ")
                .append(Component.literal(String.valueOf(sel + 1)).withStyle(ChatFormatting.GREEN))
                .append("/")
                .append(String.valueOf(count))
                .append(" - R: ")
                .append(Component.literal(render ? "yes" : "no").withStyle(render ? ChatFormatting.GREEN : ChatFormatting.RED));
        return name.withStyle(ChatFormatting.WHITE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean verbose = ClientAccess.isShiftDown();
        EnderItemTooltips.addTranslatedTips(getDescriptionId() + ".tooltips", tooltip, verbose);
        List<Component> lines = new ArrayList<>();
        addTooltipLines(stack, lines, true);
        if (!verbose && lines.size() > 2) {
            lines.clear();
            addTooltipLines(stack, lines, false);
            if (!lines.isEmpty()) {
                tooltip.add(lines.getFirst());
            }
            tooltip.add(Component.translatable("enderutilities.tooltip.item.holdshift"));
        } else {
            tooltip.addAll(lines);
        }
    }

    private void addTooltipLines(ItemStack stack, List<Component> list, boolean verbose) {
        if (!hasMemoryCard(stack) && flags(stack) == 0) {
            list.add(Component.translatable("enderutilities.tooltip.item.usetoolworkstation").withStyle(ChatFormatting.GRAY));
            return;
        }
        ChatFormatting blue = ChatFormatting.BLUE;
        ChatFormatting gray = ChatFormatting.GRAY;
        int selected = locationSelection(stack);
        Optional<BlockPos> posStart = position(stack, selected, true);
        Optional<BlockPos> posEnd = position(stack, selected, false);

        if (!verbose) {
            if (posStart.isPresent() && posEnd.isPresent()) {
                BlockPos a = posStart.get();
                BlockPos b = posEnd.get();
                list.add(Component.literal("x: ").withStyle(gray)
                        .append(Component.literal(String.valueOf(a.getX())).withStyle(blue))
                        .append(Component.literal(" y: ").withStyle(gray))
                        .append(Component.literal(String.valueOf(a.getY())).withStyle(blue))
                        .append(Component.literal(" z: ").withStyle(gray))
                        .append(Component.literal(String.valueOf(a.getZ())).withStyle(blue))
                        .append(Component.literal(" ... x: ").withStyle(gray))
                        .append(Component.literal(String.valueOf(b.getX())).withStyle(blue))
                        .append(Component.literal(" y: ").withStyle(gray))
                        .append(Component.literal(String.valueOf(b.getY())).withStyle(blue))
                        .append(Component.literal(" z: ").withStyle(gray))
                        .append(Component.literal(String.valueOf(b.getZ())).withStyle(blue)));
            }
            return;
        }

        posStart.ifPresent(pos -> list.add(coordLine("enderutilities.tooltip.item.start", pos)));
        posEnd.ifPresent(pos -> list.add(coordLine("enderutilities.tooltip.item.end", pos)));

        boolean difference = distanceDifference(stack);
        list.add(Component.translatable("enderutilities.tooltip.item.mode").withStyle(gray)
                .append(": ")
                .append(Component.translatable(difference
                        ? "enderutilities.tooltip.item.ruler.difference"
                        : "enderutilities.tooltip.item.ruler.dimensions").withStyle(ChatFormatting.DARK_GREEN)));

        list.add(yesNo("enderutilities.tooltip.item.rendercurrentwithall", alwaysRenderLocation(stack, selected)));
        list.add(yesNo("enderutilities.tooltip.item.renderall", renderAll(stack)));
        list.add(yesNo("enderutilities.tooltip.item.renderwhenunselected", renderWhenUnselected(stack)));

        int count = locationCount(stack);
        list.add(Component.translatable("enderutilities.tooltip.item.selected").withStyle(gray)
                .append(": ")
                .append(Component.literal(String.valueOf(selected + 1)).withStyle(blue))
                .append(Component.literal(" / ").withStyle(gray))
                .append(Component.literal(String.valueOf(count)).withStyle(blue)));

        int installed = ModularHelper.getInstalledModuleCount(stack, ModuleType.MEMORY_CARD_MISC);
        if (installed > 0) {
            int slotNum = ModularHelper.getClampedModuleSelection(stack, ModuleType.MEMORY_CARD_MISC);
            int max = getMaxModules(stack, ModuleType.MEMORY_CARD_MISC);
            ItemStack moduleStack = selectedCard(stack);
            MutableComponent line = Component.translatable("enderutilities.tooltip.item.selectedmemorycard.short")
                    .withStyle(gray)
                    .append(" ");
            if (!moduleStack.isEmpty() && moduleStack.getItem() instanceof IModule) {
                if (moduleStack.getHoverName() != null && moduleStack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                    line.append(Component.literal(moduleStack.getHoverName().getString())
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC))
                            .append(Component.literal(" ").withStyle(gray));
                }
                line.append(Component.literal("(").withStyle(gray))
                        .append(Component.literal(String.valueOf(slotNum + 1)).withStyle(blue))
                        .append(Component.literal(" / ").withStyle(gray))
                        .append(Component.literal(String.valueOf(max)).withStyle(blue))
                        .append(Component.literal(")").withStyle(gray));
            } else {
                line.append(Component.translatable("enderutilities.tooltip.item.selectedmemorycard.notinstalled").withStyle(gray))
                        .append(Component.literal(" (").withStyle(gray))
                        .append(Component.literal(String.valueOf(slotNum + 1)).withStyle(blue))
                        .append(Component.literal(" / ").withStyle(gray))
                        .append(Component.literal(String.valueOf(max)).withStyle(blue))
                        .append(Component.literal(")").withStyle(gray));
            }
            list.add(line);
        }
    }

    private static Component coordLine(String label, BlockPos pos) {
        ChatFormatting blue = ChatFormatting.BLUE;
        ChatFormatting gray = ChatFormatting.GRAY;
        return Component.translatable(label).withStyle(gray)
                .append(Component.literal(String.format(": x: ")).withStyle(gray))
                .append(Component.literal(String.valueOf(pos.getX())).withStyle(blue))
                .append(Component.literal(", y: ").withStyle(gray))
                .append(Component.literal(String.valueOf(pos.getY())).withStyle(blue))
                .append(Component.literal(", z: ").withStyle(gray))
                .append(Component.literal(String.valueOf(pos.getZ())).withStyle(blue));
    }

    private static Component yesNo(String label, boolean value) {
        return Component.translatable(label).withStyle(ChatFormatting.GRAY)
                .append(": ")
                .append(Component.translatable(value ? "enderutilities.tooltip.item.yes" : "enderutilities.tooltip.item.no")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 4;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType type) {
        return type == ModuleType.MEMORY_CARD_MISC ? 4 : 0;
    }

    @Override
    public int getMaxModules(ItemStack container, ItemStack moduleStack) {
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IModule module)) {
            return 0;
        }
        return getMaxModules(container, module.getModuleType(moduleStack));
    }
}
