package dev.celli.enderutilities.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.celli.enderutilities.client.ClientAccess;
import dev.celli.enderutilities.config.ModCommonConfig;
import dev.celli.enderutilities.data.StoredEntities;
import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.reference.HotKeys;
import dev.celli.enderutilities.reference.HotKeys.EnumKey;
import dev.celli.enderutilities.registry.ModDataComponents;
import dev.celli.enderutilities.util.EUStringUtils;
import dev.celli.enderutilities.util.EnderItemTooltips;
import dev.celli.enderutilities.util.EntityUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemLivingManipulator extends ItemModular {
    public static final int MAX_ENTITIES_PER_CARD = 16;

    public enum Mode {
        NORMAL,
        CAPTURE,
        RELEASE;

        public static Mode from(int id) {
            Mode[] values = values();
            return id >= 0 && id < values.length ? values[id] : NORMAL;
        }

        public String tooltipKey() {
            return "enderutilities.tooltip.item." + name().toLowerCase();
        }
    }

    public ItemLivingManipulator(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static Mode getMode(ItemStack stack) {
        return Mode.from(stack.getOrDefault(ModDataComponents.ITEM_MODE, 0));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Mode mode = getMode(stack);
        if (mode == Mode.NORMAL || mode == Mode.RELEASE) {
            if (context.getLevel().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            Player player = context.getPlayer();
            if (player == null) {
                return InteractionResult.PASS;
            }
            return releaseEntity(stack, player, context.getLevel(), context.getClickedPos(),
                    context.getClickLocation().x, context.getClickLocation().y, context.getClickLocation().z, context.getClickedFace());
        }
        return InteractionResult.PASS;
    }

    public InteractionResult handleInteraction(ItemStack stack, Player player, LivingEntity living) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (getMode(stack) == Mode.RELEASE) {
            return releaseEntity(stack, player, player.level(), living.blockPosition(), living.getX(), living.getY(), living.getZ(), Direction.UP);
        }
        return captureEntity(stack, player, living);
    }

    private InteractionResult captureEntity(ItemStack stack, Player player, LivingEntity living) {
        if (living instanceof Player) {
            return InteractionResult.PASS;
        }
        int count = storedEntityCount(stack);
        if (count < MAX_ENTITIES_PER_CARD) {
            return storeEntity(stack, living, player);
        }
        player.displayClientMessage(Component.translatable("enderutilities.chat.message.memorycard.full"), true);
        return InteractionResult.FAIL;
    }

    private InteractionResult storeEntity(ItemStack container, LivingEntity living, Player player) {
        ItemStack card = ModularHelper.getSelectedModuleStack(container, ModuleType.MEMORY_CARD_MISC);
        if (card.isEmpty() || !canStoreEntity(living)) {
            return InteractionResult.PASS;
        }
        living.stopRiding();
        living.ejectPassengers();
        CompoundTag tag = new CompoundTag();
        if (!living.saveAsPassenger(tag)) {
            return InteractionResult.FAIL;
        }
        String id = EntityType.getKey(living.getType()).toString();
        tag.putString("id", id);
        tag.putString("EntityString", id);
        tag.putFloat("playerYaw", player.getYRot());
        StoredEntities stored = card.getOrDefault(ModDataComponents.STORED_ENTITIES, StoredEntities.EMPTY);
        card.set(ModDataComponents.STORED_ENTITIES, stored.insert(stored.current(), tag));
        ModularHelper.setSelectedModuleStack(container, ModuleType.MEMORY_CARD_MISC, card);
        living.discard();
        return InteractionResult.SUCCESS;
    }

    private InteractionResult releaseEntity(ItemStack container, Player player, Level level, BlockPos pos,
            double x, double y, double z, Direction side) {
        ItemStack card = ModularHelper.getSelectedModuleStack(container, ModuleType.MEMORY_CARD_MISC);
        if (card.isEmpty()) {
            return InteractionResult.PASS;
        }
        StoredEntities stored = card.getOrDefault(ModDataComponents.STORED_ENTITIES, StoredEntities.EMPTY);
        if (stored.entities().isEmpty()) {
            return InteractionResult.PASS;
        }
        int current = Math.max(0, Math.min(stored.current(), stored.entities().size() - 1));
        CompoundTag tag = stored.entities().get(current).copy();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        boolean isShulker = false;
        String id = tag.getString("id");
        if ("minecraft:shulker".equals(id) || "Shulker".equals(id)) {
            if (tag.contains("APX", Tag.TAG_INT)) {
                int xi = pos.getX() + side.getStepX();
                int yi = pos.getY() + side.getStepY();
                int zi = pos.getZ() + side.getStepZ();
                ListTag entityPos = new ListTag();
                entityPos.add(DoubleTag.valueOf(xi + 0.5d));
                entityPos.add(DoubleTag.valueOf(yi));
                entityPos.add(DoubleTag.valueOf(zi + 0.5d));
                tag.put("Pos", entityPos);
                tag.putInt("APX", xi);
                tag.putInt("APY", yi);
                tag.putInt("APZ", zi);
                tag.putByte("AttachFace", (byte) side.get3DDataValue());
                isShulker = true;
            }
        }
        Entity entity = EntityType.loadEntityRecursive(tag, serverLevel, spawned -> spawned);
        if (entity == null) {
            return InteractionResult.FAIL;
        }
        if (!isShulker) {
            if (tag.contains("playerYaw", Tag.TAG_FLOAT)) {
                float yawDiff = player.getYRot() - tag.getFloat("playerYaw");
                entity.setYRot((entity.getYRot() + yawDiff) % 360);
            }
            double nx = x + side.getStepX() * entity.getBbWidth() * 0.5;
            double ny = y + (side == Direction.DOWN ? -entity.getBbHeight() : 0);
            double nz = z + side.getStepZ() * entity.getBbWidth() * 0.5;
            entity.moveTo(nx, ny, nz, entity.getYRot(), entity.getXRot());
        }
        entity.setDeltaMovement(entity.getDeltaMovement().x, 0.0, entity.getDeltaMovement().z);
        entity.fallDistance = 0;
        entity.setOnGround(true);
        if (entity instanceof Mob mob && ModularHelper.getInstalledModuleCount(container, ModuleType.MOBPERSISTENCE) > 0) {
            EntityUtils.applyMobPersistence(mob);
        }
        serverLevel.addFreshEntity(entity);
        card.set(ModDataComponents.STORED_ENTITIES, stored.removeCurrent());
        ModularHelper.setSelectedModuleStack(container, ModuleType.MEMORY_CARD_MISC, card);
        return InteractionResult.SUCCESS;
    }

    private static int storedEntityCount(ItemStack container) {
        ItemStack card = ModularHelper.getSelectedModuleStack(container, ModuleType.MEMORY_CARD_MISC);
        if (card.isEmpty()) {
            return 0;
        }
        return card.getOrDefault(ModDataComponents.STORED_ENTITIES, StoredEntities.EMPTY).entities().size();
    }

    private static int currentIndex(ItemStack container) {
        ItemStack card = ModularHelper.getSelectedModuleStack(container, ModuleType.MEMORY_CARD_MISC);
        if (card.isEmpty()) {
            return 0;
        }
        return card.getOrDefault(ModDataComponents.STORED_ENTITIES, StoredEntities.EMPTY).current();
    }

    private static boolean canStoreEntity(Entity entity) {
        String id = EntityType.getKey(entity.getType()).toString();
        if (ModCommonConfig.LMM_LIST_IS_WHITELIST.get()) {
            return ModCommonConfig.LMM_WHITELIST.get().contains(id);
        }
        return !ModCommonConfig.LMM_BLACKLIST.get().contains(id);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack card = ModularHelper.getSelectedModuleStack(stack, ModuleType.MEMORY_CARD_MISC);
        if (card.isEmpty()) {
            return super.getName(stack);
        }
        String base = super.getName(stack).getString();
        MutableComponent name = Component.literal(base.length() >= 14 ? EUStringUtils.getInitialsWithDots(base) : base);
        ChatFormatting green = ChatFormatting.GREEN;
        ChatFormatting white = ChatFormatting.WHITE;
        if (card.has(DataComponents.CUSTOM_NAME)) {
            name.append(" ").append(Component.literal(card.getHoverName().getString()).withStyle(green, ChatFormatting.ITALIC));
        } else {
            int selected = ModularHelper.getClampedModuleSelection(stack, ModuleType.MEMORY_CARD_MISC) + 1;
            name.append(" MC: ").append(Component.literal(String.valueOf(selected)).withStyle(green));
        }
        int index = currentIndex(stack);
        int count = storedEntityCount(stack);
        name.append(" E: ").append(Component.literal((index + 1) + "/" + count).withStyle(green));
        Component entity = selectedEntityName(stack, true);
        if (entity != null) {
            name.append(Component.literal(" -> ").withStyle(white)).append(entity);
        }
        return name;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (ModularHelper.data(stack).modules().isEmpty()) {
            EnderItemTooltips.addTranslatedTips(getDescriptionId() + ".tooltips", tooltip, ClientAccess.isShiftDown());
        }
        List<Component> lines = new ArrayList<>();
        addTooltipLines(stack, lines, true);
        EnderItemTooltips.addFoldedLines(tooltip, lines);
    }

    private void addTooltipLines(ItemStack stack, List<Component> list, boolean verbose) {
        if (ModularHelper.data(stack).modules().isEmpty()) {
            list.add(Component.translatable("enderutilities.tooltip.item.usetoolworkstation").withStyle(ChatFormatting.GRAY));
            return;
        }

        ChatFormatting gray = ChatFormatting.GRAY;
        ChatFormatting blue = ChatFormatting.BLUE;
        list.add(Component.translatable("enderutilities.tooltip.item.mode")
                .withStyle(gray)
                .append(": ")
                .append(Component.translatable(getMode(stack).tooltipKey()).withStyle(ChatFormatting.DARK_GREEN)));

        if (verbose && getMaxModules(stack, ModuleType.MOBPERSISTENCE) > 0) {
            boolean jailer = ModularHelper.getInstalledModuleCount(stack, ModuleType.MOBPERSISTENCE) > 0;
            list.add(Component.translatable("enderutilities.tooltip.item.jailer")
                    .withStyle(gray)
                    .append(": ")
                    .append(Component.translatable(jailer
                                    ? "enderutilities.tooltip.item.yes"
                                    : "enderutilities.tooltip.item.no")
                            .withStyle(jailer ? ChatFormatting.GREEN : ChatFormatting.RED)));
        }

        ItemStack card = ModularHelper.getSelectedModuleStack(stack, ModuleType.MEMORY_CARD_MISC);
        if (card.isEmpty()) {
            list.add(Component.translatable("enderutilities.tooltip.item.nomemorycards").withStyle(gray));
            return;
        }
        if (!verbose) {
            return;
        }

        int num = ModularHelper.getInstalledModuleCount(stack, ModuleType.MEMORY_CARD_MISC);
        int sel = ModularHelper.getClampedModuleSelection(stack, ModuleType.MEMORY_CARD_MISC) + 1;
        MutableComponent cardLine = Component.translatable("enderutilities.tooltip.item.selectedmemorycard.short")
                .withStyle(gray)
                .append(" ");
        if (card.has(DataComponents.CUSTOM_NAME)) {
            cardLine.append(Component.literal(card.getHoverName().getString())
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC))
                    .append(Component.literal(" ").withStyle(gray));
        }
        cardLine.append(Component.literal("(").withStyle(gray))
                .append(Component.literal(String.valueOf(sel)).withStyle(blue))
                .append(Component.literal(" / ").withStyle(gray))
                .append(Component.literal(String.valueOf(num)).withStyle(blue))
                .append(Component.literal(")").withStyle(gray));
        list.add(cardLine);

        StoredEntities stored = card.getOrDefault(ModDataComponents.STORED_ENTITIES, StoredEntities.EMPTY);
        addGroupedEntityLines(stored, list);
    }

    /**
     * Orig listed every stored mob. Identical unnamed types collapse to one counted line
     * so a card of sheep plus {@code jeb_} stays compact.
     */
    private static void addGroupedEntityLines(StoredEntities stored, List<Component> list) {
        List<CompoundTag> entities = stored.entities();
        if (entities.isEmpty()) {
            return;
        }
        int current = Math.max(0, Math.min(stored.current(), entities.size() - 1));
        Map<String, EntityGroup> groups = new LinkedHashMap<>();
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag tag = entities.get(i);
            String typeId = entityIdFromTag(tag);
            String custom = customNameFromTag(tag);
            String key = custom + "\0" + typeId;
            EntityGroup group = groups.get(key);
            if (group == null) {
                group = new EntityGroup(custom, typeNameFromTag(tag), healthFromTag(tag));
                groups.put(key, group);
            }
            group.count++;
            Float health = healthFromTag(tag);
            if (group.health != null && (health == null || Float.compare(group.health, health) != 0)) {
                group.health = null;
            }
            if (i == current) {
                group.selected = true;
            }
        }
        for (EntityGroup group : groups.values()) {
            MutableComponent line = Component.literal(group.selected ? "=> " : "   ").withStyle(ChatFormatting.GRAY);
            line.append(formatEntityLabel(group.customName, group.typeName, group.health, ChatFormatting.GRAY));
            if (group.count > 1) {
                line.append(Component.literal(" x" + group.count).withStyle(ChatFormatting.GRAY));
            }
            list.add(line);
        }
    }

    private static Component selectedEntityName(ItemStack container, boolean displayNameFormatting) {
        ItemStack card = ModularHelper.getSelectedModuleStack(container, ModuleType.MEMORY_CARD_MISC);
        if (card.isEmpty()) {
            return null;
        }
        StoredEntities stored = card.getOrDefault(ModDataComponents.STORED_ENTITIES, StoredEntities.EMPTY);
        if (stored.entities().isEmpty()) {
            return null;
        }
        int index = Math.max(0, Math.min(stored.current(), stored.entities().size() - 1));
        return formatEntityFromTag(stored.entities().get(index), displayNameFormatting);
    }

    private static Component formatEntityFromTag(CompoundTag tag, boolean displayNameFormatting) {
        ChatFormatting rst = displayNameFormatting ? ChatFormatting.WHITE : ChatFormatting.GRAY;
        return formatEntityLabel(customNameFromTag(tag), typeNameFromTag(tag), healthFromTag(tag), rst);
    }

    private static Component formatEntityLabel(String customName, String typeName, Float health, ChatFormatting rst) {
        MutableComponent name;
        if (!customName.isEmpty()) {
            name = Component.literal(customName).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC)
                    .append(Component.literal(" (" + typeName + ")").withStyle(rst));
        } else {
            name = Component.literal(typeName).withStyle(rst);
        }
        if (health != null) {
            name.append(Component.literal(String.format(" (%.1f HP)", health)).withStyle(rst));
        }
        return name;
    }

    private static String entityIdFromTag(CompoundTag tag) {
        if (tag.contains("EntityString", Tag.TAG_STRING)) {
            return tag.getString("EntityString");
        }
        if (tag.contains("id", Tag.TAG_STRING)) {
            return tag.getString("id");
        }
        return "";
    }

    private static String typeNameFromTag(CompoundTag tag) {
        String id = entityIdFromTag(tag);
        if (id.isEmpty()) {
            return "unknown";
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location != null) {
            return BuiltInRegistries.ENTITY_TYPE.getOptional(location)
                    .map(type -> type.getDescription().getString())
                    .orElse(id);
        }
        return id;
    }

    private static String customNameFromTag(CompoundTag tag) {
        if (!tag.contains("CustomName", Tag.TAG_STRING)) {
            return "";
        }
        String raw = tag.getString("CustomName");
        if (raw.isEmpty()) {
            return "";
        }
        try {
            Component parsed = Component.Serializer.fromJson(raw, RegistryAccess.EMPTY);
            if (parsed != null) {
                String text = parsed.getString();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        } catch (Exception ignored) {
        }
        return raw;
    }

    private static Float healthFromTag(CompoundTag tag) {
        return tag.contains("Health") ? tag.getFloat("Health") : null;
    }

    @Override
    public boolean doKeyBindingAction(Player player, ItemStack stack, int key) {
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL)) {
            return ModularHelper.changeSelectedModule(
                    stack,
                    ModuleType.MEMORY_CARD_MISC,
                    EnumKey.keypressActionIsReversed(key) || EnumKey.keypressContainsShift(key)
            );
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT) || EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT)) {
            changeEntitySelection(stack, EnumKey.keypressActionIsReversed(key));
            return true;
        }
        if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE)) {
            int next = (getMode(stack).ordinal() + 1) % Mode.values().length;
            stack.set(ModDataComponents.ITEM_MODE, next);
            return true;
        }
        return false;
    }

    private static void changeEntitySelection(ItemStack container, boolean reverse) {
        ItemStack card = ModularHelper.getSelectedModuleStack(container, ModuleType.MEMORY_CARD_MISC);
        if (card.isEmpty()) {
            return;
        }
        StoredEntities stored = card.getOrDefault(ModDataComponents.STORED_ENTITIES, StoredEntities.EMPTY);
        int count = stored.entities().size();
        int current = stored.current();
        if (reverse) {
            if (--current < 0) {
                current = count > 0 ? count - 1 : 0;
            }
        } else if (++current >= count) {
            current = 0;
        }
        card.set(ModDataComponents.STORED_ENTITIES, stored.withCurrent(current));
        ModularHelper.setSelectedModuleStack(container, ModuleType.MEMORY_CARD_MISC, card);
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 5;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType type) {
        return switch (type) {
            case MEMORY_CARD_MISC -> 4;
            case MOBPERSISTENCE -> 1;
            default -> 0;
        };
    }

    @Override
    public int getMaxModules(ItemStack container, ItemStack moduleStack) {
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IModule module)) {
            return 0;
        }
        return getMaxModules(container, module.getModuleType(moduleStack));
    }

    private static final class EntityGroup {
        private final String customName;
        private final String typeName;
        private Float health;
        private int count;
        private boolean selected;

        private EntityGroup(String customName, String typeName, Float health) {
            this.customName = customName;
            this.typeName = typeName;
            this.health = health;
        }
    }
}
