package dev.celli.enderutilities.item;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.data.ItemInventoryData;
import dev.celli.enderutilities.data.OwnerInfo;
import dev.celli.enderutilities.data.StoredModule;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EUStringUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemMemoryCard extends ItemEnderUtilities implements IModule {
    private final ModuleType type;
    private final int tier;

    public ItemMemoryCard(Properties properties, ModuleType type, int tier) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    @Override
    public ModuleType getModuleType(ItemStack stack) {
        return type;
    }

    @Override
    public int getModuleTier(ItemStack stack) {
        return tier;
    }

    /**
     * Orig {@code InventoryItem.getInventoryStackLimitFromContainerStack}: {@code 2^bits}
     * where bits are 6 / 8 / 10 / 12 (64 / 256 / 1024 / 4096).
     */
    public static int getInventoryStackLimitFromContainerStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemMemoryCard card)
                || card.getModuleType(stack) != ModuleType.MEMORY_CARD_ITEMS) {
            return 64;
        }
        int bits = 6 + card.getModuleTier(stack) * 2;
        if (bits < 6 || bits > 12) {
            return 64;
        }
        return 1 << bits;
    }

    /**
     * Orig {@code OwnerData.removeOwnerDataFromItem}: owner only, silent fail otherwise.
     */
    public static boolean removeOwnerDataFromItem(ItemStack stack, Player player) {
        OwnerInfo owner = stack.get(ModDataComponents.OWNER);
        if (owner != null && owner.isOwner(player)) {
            stack.remove(ModDataComponents.OWNER);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (type != ModuleType.MEMORY_CARD_ITEMS) {
            return super.use(level, player, hand);
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                removeOwnerDataFromItem(stack, player);
            } else {
                OwnerInfo owner = stack.get(ModDataComponents.OWNER);
                if (owner == null) {
                    stack.set(ModDataComponents.OWNER, OwnerInfo.of(player, false));
                } else if (owner.isOwner(player)) {
                    stack.set(ModDataComponents.OWNER, owner.withPublic(!owner.isPublic()));
                }
            }
        }
        return ItemEnderUtilities.handledWithoutUseAnim(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Player player = ClientAccess.player();
        OwnerInfo owner = stack.get(ModDataComponents.OWNER);
        if (owner != null && player != null && !owner.canAccess(player)) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.owner")
                    .append(": ")
                    .append(Component.literal(owner.name()).withStyle(ChatFormatting.WHITE))
                    .append(" - ")
                    .append(Component.translatable("enderutilities.tooltip.item.private").withStyle(ChatFormatting.RED)));
            return;
        }

        if (type == ModuleType.MEMORY_CARD_MISC) {
            addMiscDataLines(stack, tooltip);
        } else if (type == ModuleType.MEMORY_CARD_ITEMS) {
            addStoredItemLines(stack, tooltip);
        }

        if (owner != null) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.owner")
                    .append(": ")
                    .append(Component.literal(owner.name()).withStyle(ChatFormatting.WHITE))
                    .append(" - ")
                    .append(Component.translatable(owner.isPublic()
                                    ? "enderutilities.tooltip.item.public"
                                    : "enderutilities.tooltip.item.private")
                            .withStyle(owner.isPublic() ? ChatFormatting.GREEN : ChatFormatting.RED)));
        }
    }

    private static void addStoredItemLines(ItemStack stack, List<Component> tooltip) {
        ItemInventoryData data = stack.getOrDefault(ModDataComponents.ITEM_INVENTORY, ItemInventoryData.EMPTY);
        List<Component> lines = new ArrayList<>();
        int itemCount = 0;
        int stackCount = 0;
        int overflow = 0;
        for (StoredModule stored : data.slots()) {
            ItemStack storedStack = stored.stack();
            if (storedStack.isEmpty()) {
                continue;
            }
            stackCount++;
            itemCount += storedStack.getCount();
            if (lines.size() < 20) {
                lines.add(Component.literal("  ")
                        .append(Component.literal(String.format("%4d", storedStack.getCount())).withStyle(ChatFormatting.WHITE))
                        .append(" ")
                        .append(storedStack.getHoverName().copy().withStyle(ChatFormatting.GRAY)));
            } else {
                overflow++;
            }
        }
        if (stackCount == 0) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.memorycard.noitems"));
            return;
        }
        tooltip.add(Component.translatable(
                "enderutilities.tooltip.item.memorycard.items.stackcount",
                stackCount,
                EUStringUtils.formatNumberWithKSeparators(itemCount)));
        tooltip.addAll(lines);
        if (overflow > 0) {
            tooltip.add(Component.literal("     ... ")
                    .append(Component.translatable("enderutilities.tooltip.item.and"))
                    .append(" ")
                    .append(Component.literal(String.valueOf(overflow)).withStyle(ChatFormatting.WHITE))
                    .append(" ")
                    .append(Component.translatable("enderutilities.tooltip.item.morestacksnotlisted")));
        }
    }

    private static void addMiscDataLines(ItemStack stack, List<Component> tooltip) {
        List<String> keys = new ArrayList<>();
        stack.getComponents().forEach(typed -> {
            DataComponentType<?> type = typed.type();
            if (type == DataComponents.CUSTOM_NAME || type == DataComponents.LORE
                    || type == DataComponents.REPAIR_COST || type == ModDataComponents.OWNER.get()) {
                return;
            }
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (id != null) {
                keys.add(id.toString());
            }
        });
        if (keys.isEmpty()) {
            tooltip.add(Component.translatable("enderutilities.tooltip.item.memorycard.nodata"));
            return;
        }
        tooltip.add(Component.translatable("enderutilities.tooltip.item.memorycard.datatypecount", keys.size()));
        for (String key : keys) {
            tooltip.add(Component.literal("  " + key));
        }
    }
}
