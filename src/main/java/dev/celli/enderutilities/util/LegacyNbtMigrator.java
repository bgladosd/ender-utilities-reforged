package dev.celli.enderutilities.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.data.WandTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

/**
 * Converts 1.12-shaped builder-wand / template NBT (and item stacks inside it)
 * to 1.21 {@link ItemStack#parse} / named block states. This is not Minecraft's
 * 1.12 DataFixer; unmapped entries are skipped and logged.
 */
public final class LegacyNbtMigrator {
    private static final String[] WOOL = {
            "white_wool", "orange_wool", "magenta_wool", "light_blue_wool",
            "yellow_wool", "lime_wool", "pink_wool", "gray_wool",
            "light_gray_wool", "cyan_wool", "purple_wool", "blue_wool",
            "brown_wool", "green_wool", "red_wool", "black_wool"
    };
    private static final String[] TERRACOTTA = {
            "white_terracotta", "orange_terracotta", "magenta_terracotta", "light_blue_terracotta",
            "yellow_terracotta", "lime_terracotta", "pink_terracotta", "gray_terracotta",
            "light_gray_terracotta", "cyan_terracotta", "purple_terracotta", "blue_terracotta",
            "brown_terracotta", "green_terracotta", "red_terracotta", "black_terracotta"
    };
    private static final String[] GLASS = {
            "white_stained_glass", "orange_stained_glass", "magenta_stained_glass", "light_blue_stained_glass",
            "yellow_stained_glass", "lime_stained_glass", "pink_stained_glass", "gray_stained_glass",
            "light_gray_stained_glass", "cyan_stained_glass", "purple_stained_glass", "blue_stained_glass",
            "brown_stained_glass", "green_stained_glass", "red_stained_glass", "black_stained_glass"
    };
    private static final String[] GLASS_PANE = {
            "white_stained_glass_pane", "orange_stained_glass_pane", "magenta_stained_glass_pane", "light_blue_stained_glass_pane",
            "yellow_stained_glass_pane", "lime_stained_glass_pane", "pink_stained_glass_pane", "gray_stained_glass_pane",
            "light_gray_stained_glass_pane", "cyan_stained_glass_pane", "purple_stained_glass_pane", "blue_stained_glass_pane",
            "brown_stained_glass_pane", "green_stained_glass_pane", "red_stained_glass_pane", "black_stained_glass_pane"
    };
    private static final String[] PLANKS = {
            "oak_planks", "spruce_planks", "birch_planks", "jungle_planks",
            "acacia_planks", "dark_oak_planks"
    };
    private static final String[] LOG = {"oak_log", "spruce_log", "birch_log", "jungle_log"};
    private static final String[] LOG2 = {"acacia_log", "dark_oak_log"};
    private static final String[] STONE = {
            "stone", "granite", "polished_granite", "diorite",
            "polished_diorite", "andesite", "polished_andesite"
    };
    private static final String[] CARPET = {
            "white_carpet", "orange_carpet", "magenta_carpet", "light_blue_carpet",
            "yellow_carpet", "lime_carpet", "pink_carpet", "gray_carpet",
            "light_gray_carpet", "cyan_carpet", "purple_carpet", "blue_carpet",
            "brown_carpet", "green_carpet", "red_carpet", "black_carpet"
    };
    private static final String[] CONCRETE = {
            "white_concrete", "orange_concrete", "magenta_concrete", "light_blue_concrete",
            "yellow_concrete", "lime_concrete", "pink_concrete", "gray_concrete",
            "light_gray_concrete", "cyan_concrete", "purple_concrete", "blue_concrete",
            "brown_concrete", "green_concrete", "red_concrete", "black_concrete"
    };
    private static final String[] CONCRETE_POWDER = {
            "white_concrete_powder", "orange_concrete_powder", "magenta_concrete_powder", "light_blue_concrete_powder",
            "yellow_concrete_powder", "lime_concrete_powder", "pink_concrete_powder", "gray_concrete_powder",
            "light_gray_concrete_powder", "cyan_concrete_powder", "purple_concrete_powder", "blue_concrete_powder",
            "brown_concrete_powder", "green_concrete_powder", "red_concrete_powder", "black_concrete_powder"
    };
    private static final String[] BED = {
            "white_bed", "orange_bed", "magenta_bed", "light_blue_bed",
            "yellow_bed", "lime_bed", "pink_bed", "gray_bed",
            "light_gray_bed", "cyan_bed", "purple_bed", "blue_bed",
            "brown_bed", "green_bed", "red_bed", "black_bed"
    };
    private static final String[] DYE = {
            "ink_sac", "red_dye", "green_dye", "cocoa_beans",
            "lapis_lazuli", "purple_dye", "cyan_dye", "light_gray_dye",
            "gray_dye", "pink_dye", "lime_dye", "yellow_dye",
            "light_blue_dye", "magenta_dye", "orange_dye", "bone_meal"
    };
    private static final String[] FISH = {"cod", "salmon", "tropical_fish", "pufferfish"};
    private static final String[] COOKED_FISH = {"cooked_cod", "cooked_salmon"};
    private static final String[] BOAT = {"oak_boat", "spruce_boat", "birch_boat", "jungle_boat", "acacia_boat", "dark_oak_boat"};
    private static final String[] WOOD_SLAB = {
            "oak_slab", "spruce_slab", "birch_slab", "jungle_slab", "acacia_slab", "dark_oak_slab"
    };
    private static final String[] STONE_SLAB = {
            "smooth_stone_slab", "sandstone_slab", "petrified_oak_slab", "cobblestone_slab",
            "brick_slab", "stone_brick_slab", "nether_brick_slab", "quartz_slab"
    };
    private static final String[] DOUBLE_STONE_SLAB = {
            "smooth_stone", "sandstone", "oak_planks", "cobblestone",
            "bricks", "stone_bricks", "nether_bricks", "quartz_block"
    };
    private static final String[] LEAVES = {"oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves"};
    private static final String[] LEAVES2 = {"acacia_leaves", "dark_oak_leaves"};
    private static final String[] SAPLING = {
            "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling", "acacia_sapling", "dark_oak_sapling"
    };
    private static final String[] TALLGRASS = {"dead_bush", "short_grass", "fern"};
    private static final String[] DOUBLE_PLANT = {
            "sunflower", "lilac", "tall_grass", "large_fern", "rose_bush", "peony"
    };
    private static final String[] RED_FLOWER = {
            "poppy", "blue_orchid", "allium", "azure_bluet",
            "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy"
    };
    private static final String[] STONEBRICK = {
            "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks"
    };
    private static final String[] MONSTER_EGG = {
            "infested_stone", "infested_cobblestone", "infested_stone_bricks",
            "infested_mossy_stone_bricks", "infested_cracked_stone_bricks", "infested_chiseled_stone_bricks"
    };
    private static final String[] SKULL_ITEM = {
            "skeleton_skull", "wither_skeleton_skull", "zombie_head", "player_head", "creeper_head", "dragon_head"
    };
    private static final String[] SKULL_BLOCK = {
            "skeleton_skull", "wither_skeleton_skull", "zombie_head", "player_head", "creeper_head", "dragon_head"
    };
    private static final String[] ANVIL = {"anvil", "chipped_anvil", "damaged_anvil"};
    private static final String[] QUARTZ = {"quartz_block", "chiseled_quartz_block", "quartz_pillar"};
    private static final String[] SANDSTONE = {"sandstone", "chiseled_sandstone", "cut_sandstone"};
    private static final String[] RED_SANDSTONE = {"red_sandstone", "chiseled_red_sandstone", "cut_red_sandstone"};
    private static final String[] PRISMARINE = {"prismarine", "prismarine_bricks", "dark_prismarine"};

    private LegacyNbtMigrator() {
    }

    public static boolean isLegacyTemplate(CompoundTag tag) {
        if (tag.contains("blockidmap", Tag.TAG_LIST) || tag.contains("author", Tag.TAG_STRING)
                && tag.contains("version", Tag.TAG_ANY_NUMERIC)) {
            return true;
        }
        if (tag.contains("palette", Tag.TAG_LIST) && tag.contains("size", Tag.TAG_LIST)) {
            return true;
        }
        ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
        if (blocks.isEmpty()) {
            return tag.contains("size", Tag.TAG_LIST) && !tag.contains("name", Tag.TAG_STRING);
        }
        CompoundTag first = blocks.getCompound(0);
        return first.contains("pos", Tag.TAG_LIST) && first.contains("state") && !first.contains("offset");
    }

    public static boolean isLegacyItem(CompoundTag tag) {
        if (tag.isEmpty() || tag.contains("components", Tag.TAG_COMPOUND)) {
            return false;
        }
        return tag.contains("Damage", Tag.TAG_ANY_NUMERIC)
                || tag.contains("tag", Tag.TAG_COMPOUND)
                || tag.contains("ForgeData", Tag.TAG_COMPOUND)
                || tag.contains("ForgeCaps", Tag.TAG_COMPOUND)
                || tag.contains("Count", Tag.TAG_ANY_NUMERIC) && !tag.contains("count", Tag.TAG_ANY_NUMERIC);
    }

    public static Optional<WandTemplate> migrateTemplate(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            List<WandTemplate.Entry> blocks = new ArrayList<>();
            List<WandTemplate.EntityEntry> entities = new ArrayList<>();
            if (tag.contains("palette", Tag.TAG_LIST)) {
                readVanillaStructure(tag, registries, blocks, entities);
            } else {
                readEnderUtilitiesTemplate(tag, registries, blocks, entities);
            }
            if (blocks.isEmpty()) {
                EnderUtilities.LOGGER.warn("Skipping 1.12 builders wand template: no mapped blocks");
                return Optional.empty();
            }
            String name = tag.contains("name", Tag.TAG_STRING) ? tag.getString("name") : tag.getString("author");
            return Optional.of(new WandTemplate(blocks, name, entities, Direction.NORTH));
        } catch (Exception e) {
            EnderUtilities.LOGGER.warn("Failed to convert 1.12 builders wand template", e);
            return Optional.empty();
        }
    }

    public static Optional<ItemStack> parseItem(HolderLookup.Provider registries, CompoundTag tag) {
        CompoundTag modern = toModernItemTag(tag);
        if (modern == null) {
            EnderUtilities.LOGGER.warn("Skipping unmapped 1.12 item {}", describeItem(tag));
            return Optional.empty();
        }
        return ItemStack.parse(registries, modern);
    }

    public static CompoundTag toModernItemTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        if (!isLegacyItem(tag) && tag.contains("id") && (tag.contains("count") || tag.contains("Count"))) {
            CompoundTag copy = tag.copy();
            if (!copy.contains("count") && copy.contains("Count")) {
                copy.putInt("count", Math.max(1, copy.getByte("Count")));
                copy.remove("Count");
            }
            return copy;
        }
        ResourceLocation id = resolveItemId(tag);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        CompoundTag modern = new CompoundTag();
        modern.putString("id", id.toString());
        int count = 1;
        if (tag.contains("Count", Tag.TAG_ANY_NUMERIC)) {
            count = Math.max(1, tag.getByte("Count"));
        } else if (tag.contains("count", Tag.TAG_ANY_NUMERIC)) {
            count = Math.max(1, tag.getInt("count"));
        }
        modern.putInt("count", count);
        CompoundTag extras = new CompoundTag();
        if (tag.contains("tag", Tag.TAG_COMPOUND)) {
            extras.put("tag", tag.getCompound("tag").copy());
        }
        if (tag.contains("ForgeData", Tag.TAG_COMPOUND)) {
            extras.put("ForgeData", tag.getCompound("ForgeData").copy());
        }
        if (tag.contains("ForgeCaps", Tag.TAG_COMPOUND)) {
            extras.put("ForgeCaps", tag.getCompound("ForgeCaps").copy());
        }
        if (!extras.isEmpty()) {
            CompoundTag components = new CompoundTag();
            components.put("minecraft:custom_data", extras);
            modern.put("components", components);
        }
        return modern;
    }

    private static void readEnderUtilitiesTemplate(
            CompoundTag tag,
            HolderLookup.Provider registries,
            List<WandTemplate.Entry> blocks,
            List<WandTemplate.EntityEntry> entities
    ) {
        Map<Integer, String> idMap = new HashMap<>();
        ListTag mapList = tag.getList("blockidmap", Tag.TAG_COMPOUND);
        for (int i = 0; i < mapList.size(); i++) {
            CompoundTag entry = mapList.getCompound(i);
            idMap.put(entry.getShort("id") & 0xFFF, entry.getString("name"));
        }
        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag entry = blockList.getCompound(i);
            BlockPos pos = readIntPos(entry.getList("pos", Tag.TAG_INT));
            int stateId = entry.getInt("state");
            String name = idMap.get(stateId & 0xFFF);
            int meta = (stateId >> 12) & 0xF;
            BlockState state = name == null ? null : blockState(name, meta);
            if (state == null || state.isAir()) {
                EnderUtilities.LOGGER.warn("Skipping unmapped 1.12 template block {} meta {}", name, meta);
                continue;
            }
            Optional<CompoundTag> nbt = Optional.empty();
            if (entry.contains("nbt", Tag.TAG_COMPOUND)) {
                nbt = Optional.of(migrateNestedItems(entry.getCompound("nbt").copy(), registries));
            }
            blocks.add(new WandTemplate.Entry(pos, state, nbt));
        }
        readEntities(tag.getList("entities", Tag.TAG_COMPOUND), entities);
    }

    private static void readVanillaStructure(
            CompoundTag tag,
            HolderLookup.Provider registries,
            List<WandTemplate.Entry> blocks,
            List<WandTemplate.EntityEntry> entities
    ) {
        ListTag palette = tag.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> states = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag entry = palette.getCompound(i);
            String name = entry.contains("Name") ? entry.getString("Name") : entry.getString("id");
            BlockState state = blockStateFromPalette(name, entry.getCompound("Properties"));
            if (state == null) {
                EnderUtilities.LOGGER.warn("Skipping unmapped 1.12 palette entry {}", name);
                states.add(Blocks.AIR.defaultBlockState());
            } else {
                states.add(state);
            }
        }
        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag entry = blockList.getCompound(i);
            int index = entry.getInt("state");
            if (index < 0 || index >= states.size() || states.get(index).isAir()) {
                continue;
            }
            BlockPos pos = readIntPos(entry.getList("pos", Tag.TAG_INT));
            Optional<CompoundTag> nbt = Optional.empty();
            if (entry.contains("nbt", Tag.TAG_COMPOUND)) {
                nbt = Optional.of(migrateNestedItems(entry.getCompound("nbt").copy(), registries));
            }
            blocks.add(new WandTemplate.Entry(pos, states.get(index), nbt));
        }
        readEntities(tag.getList("entities", Tag.TAG_COMPOUND), entities);
    }

    private static void readEntities(ListTag list, List<WandTemplate.EntityEntry> entities) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Vec3 pos = readDoublePos(entry.getList("pos", Tag.TAG_DOUBLE));
            BlockPos blockPos = entry.contains("blockPos", Tag.TAG_LIST)
                    ? readIntPos(entry.getList("blockPos", Tag.TAG_INT))
                    : BlockPos.containing(pos);
            CompoundTag nbt = entry.contains("nbt", Tag.TAG_COMPOUND) ? entry.getCompound("nbt") : new CompoundTag();
            if (nbt.isEmpty()) {
                EnderUtilities.LOGGER.warn("Skipping unmapped 1.12 template entity at {}", pos);
                continue;
            }
            entities.add(new WandTemplate.EntityEntry(pos, blockPos, nbt));
        }
    }

    private static CompoundTag migrateNestedItems(CompoundTag nbt, HolderLookup.Provider registries) {
        for (String key : List.of("Items", "ItemsInv", "Inventory", "inventory")) {
            if (nbt.contains(key, Tag.TAG_LIST)) {
                ListTag items = nbt.getList(key, Tag.TAG_COMPOUND);
                ListTag next = new ListTag();
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag item = items.getCompound(i);
                    CompoundTag modern = toModernItemTag(item);
                    if (modern == null) {
                        EnderUtilities.LOGGER.warn("Skipping unmapped 1.12 nested item {}", describeItem(item));
                        continue;
                    }
                    if (item.contains("Slot")) {
                        modern.putByte("Slot", item.getByte("Slot"));
                    }
                    next.add(modern);
                }
                nbt.put(key, next);
            }
        }
        if (nbt.contains("Item", Tag.TAG_COMPOUND)) {
            CompoundTag modern = toModernItemTag(nbt.getCompound("Item"));
            if (modern != null) {
                nbt.put("Item", modern);
            }
        }
        return nbt;
    }

    private static ResourceLocation resolveItemId(CompoundTag tag) {
        String raw = readIdString(tag);
        if (raw == null) {
            return null;
        }
        int damage = tag.contains("Damage", Tag.TAG_ANY_NUMERIC) ? tag.getShort("Damage") : 0;
        String mapped = mapItemId(raw, damage);
        return mapped == null ? null : ResourceLocation.tryParse(mapped);
    }

    private static String readIdString(CompoundTag tag) {
        if (!tag.contains("id")) {
            return null;
        }
        Tag id = tag.get("id");
        if (id instanceof net.minecraft.nbt.StringTag stringTag) {
            return stringTag.getAsString();
        }
        if (id instanceof NumericTag numeric) {
            return numeric.getAsInt() == 0 ? "minecraft:air" : null;
        }
        return null;
    }

    static String mapItemId(String raw, int damage) {
        ResourceLocation location = ResourceLocation.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        if ("enderutilities".equals(location.getNamespace())) {
            return mapEnderUtilitiesItem(path, damage);
        }
        if (!"minecraft".equals(location.getNamespace())) {
            return location.toString();
        }
        String flattened = flattenMinecraftItem(path, damage);
        return flattened == null ? location.toString() : "minecraft:" + flattened;
    }

    private static String mapEnderUtilitiesItem(String path, int damage) {
        return switch (path) {
            case "frame" -> "enderutilities:portal_frame";
            case "enderfurnace" -> "enderutilities:ender_furnace";
            case "enderpart" -> switch (damage) {
                case 0 -> "enderutilities:enderpart_enderalloy_0";
                case 1 -> "enderutilities:enderpart_enderalloy_1";
                case 2 -> "enderutilities:enderpart_enderalloy_2";
                case 10 -> "enderutilities:enderpart_endercore_0_inactive";
                case 11 -> "enderutilities:enderpart_endercore_1_inactive";
                case 12 -> "enderutilities:enderpart_endercore_2_inactive";
                case 15 -> "enderutilities:enderpart_endercore_0_active";
                case 16 -> "enderutilities:enderpart_endercore_1_active";
                case 17 -> "enderutilities:enderpart_endercore_2_active";
                case 20 -> "enderutilities:enderpart_enderstick";
                case 21 -> "enderutilities:enderpart_enderrope";
                case 30 -> "enderutilities:enderpart_creative_breaking";
                case 40 -> "enderutilities:enderpart_enderrelic";
                case 45 -> "enderutilities:enderpart_jailer";
                case 50 -> "enderutilities:enderpart_memorycard_misc";
                case 51 -> "enderutilities:enderpart_memorycard_items_0";
                case 52 -> "enderutilities:enderpart_memorycard_items_1";
                case 53 -> "enderutilities:enderpart_memorycard_items_2";
                case 54 -> "enderutilities:enderpart_memorycard_items_3";
                case 70 -> "enderutilities:enderpart_barrel_upgrade_0";
                case 71 -> "enderutilities:enderpart_barrel_upgrade_1";
                case 72 -> "enderutilities:enderpart_barrel_upgrade_2";
                case 73 -> "enderutilities:enderpart_barrel_upgrade_3";
                case 80 -> "enderutilities:enderpart_storage_key";
                case 81 -> "enderutilities:enderpart_storage_key_creative";
                default -> null;
            };
            case "endercapacitor" -> "enderutilities:endercapacitor_" + Math.max(0, Math.min(3, damage));
            case "linkcrystal" -> "enderutilities:linkcrystal_" + Math.max(0, Math.min(2, damage));
            case "endertool" -> switch (damage) {
                case 1 -> "enderutilities:endertool_enderaxe";
                case 2 -> "enderutilities:endertool_endershovel";
                case 3 -> "enderutilities:endertool_enderhoe";
                default -> "enderutilities:endertool_enderpickaxe";
            };
            case "enderpearlreusable" -> damage == 1
                    ? "enderutilities:enderpearlreusable_elite"
                    : "enderutilities:enderpearlreusable";
            case "enderporter" -> damage == 1
                    ? "enderutilities:enderporter_advanced"
                    : "enderutilities:enderporter";
            case "icemelter" -> damage == 1 ? "enderutilities:icemelter_super" : "enderutilities:icemelter";
            case "handybag" -> damage == 1 ? "enderutilities:handybag_large" : "enderutilities:handybag";
            case "syringe" -> switch (damage) {
                case 1 -> "enderutilities:syringe_paralyzer";
                case 2 -> "enderutilities:syringe_stimulant";
                case 3 -> "enderutilities:syringe_passifier";
                default -> "enderutilities:syringe_empty";
            };
            case "machine_0" -> "enderutilities:ender_furnace";
            case "machine_1" -> switch (damage) {
                case 1 -> "enderutilities:tool_workstation";
                case 2 -> "enderutilities:creation_station";
                case 3 -> "enderutilities:quick_stacker_advanced";
                default -> "enderutilities:ender_infuser";
            };
            case "energy_bridge" -> switch (damage) {
                case 1 -> "enderutilities:energy_bridge_receiver";
                case 2 -> "enderutilities:energy_bridge_transmitter";
                default -> "enderutilities:energy_bridge_resonator";
            };
            case "storage_0" -> switch (damage) {
                case 1 -> "enderutilities:memory_chest_1";
                case 2 -> "enderutilities:memory_chest_2";
                case 3 -> "enderutilities:handy_chest_0";
                case 4 -> "enderutilities:handy_chest_1";
                case 5 -> "enderutilities:handy_chest_2";
                case 6 -> "enderutilities:handy_chest_3";
                case 7 -> "enderutilities:jsu";
                default -> "enderutilities:memory_chest_0";
            };
            case "msu" -> damage == 1 ? "enderutilities:msu_bundle" : "enderutilities:msu";
            case "floor" -> (damage & 1) != 0 ? "enderutilities:floor_cracked" : "enderutilities:floor";
            case "phasing" -> (damage & 1) != 0 ? "enderutilities:phasing_inverted" : "enderutilities:phasing";
            case "inserter" -> (damage & 1) != 0 ? "enderutilities:inserter_filtered" : "enderutilities:inserter";
            case "draw_bridge" -> (damage & 1) != 0 ? "enderutilities:draw_bridge_advanced" : "enderutilities:draw_bridge";
            default -> "enderutilities:" + path;
        };
    }

    static String flattenMinecraftItem(String path, int damage) {
        String colored = flattenColored(path, damage);
        if (colored != null) {
            return colored;
        }
        return switch (path) {
            case "dye" -> color(DYE, damage);
            case "fish" -> color(FISH, damage);
            case "cooked_fish" -> color(COOKED_FISH, damage);
            case "golden_apple" -> damage == 1 ? "enchanted_golden_apple" : "golden_apple";
            case "skull" -> color(SKULL_ITEM, damage);
            case "bed" -> color(BED, damage);
            case "boat" -> color(BOAT, damage);
            case "coal" -> damage == 1 ? "charcoal" : "coal";
            case "melon" -> "melon_slice";
            case "speckled_melon" -> "glistering_melon_slice";
            case "netherbrick" -> "nether_brick";
            case "fireball" -> "fire_charge";
            case "wooden_door" -> "oak_door";
            case "reeds" -> "sugar_cane";
            case "seeds" -> "wheat_seeds";
            case "sign" -> "oak_sign";
            case "record_13" -> "music_disc_13";
            case "record_cat" -> "music_disc_cat";
            case "record_blocks" -> "music_disc_blocks";
            case "record_chirp" -> "music_disc_chirp";
            case "record_far" -> "music_disc_far";
            case "record_mall" -> "music_disc_mall";
            case "record_mellohi" -> "music_disc_mellohi";
            case "record_stal" -> "music_disc_stal";
            case "record_strad" -> "music_disc_strad";
            case "record_ward" -> "music_disc_ward";
            case "record_11" -> "music_disc_11";
            case "record_wait" -> "music_disc_wait";
            default -> flattenShared(path, damage);
        };
    }

    static String flattenMinecraftBlock(String path, int meta) {
        String colored = flattenColored(path, meta);
        if (colored != null) {
            return colored;
        }
        return switch (path) {
            case "grass" -> "grass_block";
            case "grass_path" -> "dirt_path";
            case "web" -> "cobweb";
            case "snow" -> "snow_block";
            case "snow_layer" -> "snow";
            case "brick_block" -> "bricks";
            case "nether_brick" -> "nether_bricks";
            case "red_nether_brick" -> "red_nether_bricks";
            case "end_bricks" -> "end_stone_bricks";
            case "melon_block" -> "melon";
            case "pumpkin" -> "carved_pumpkin";
            case "lit_pumpkin" -> "jack_o_lantern";
            case "noteblock" -> "note_block";
            case "wooden_door" -> "oak_door";
            case "wooden_pressure_plate" -> "oak_pressure_plate";
            case "wooden_button" -> "oak_button";
            case "trapdoor" -> "oak_trapdoor";
            case "fence" -> "oak_fence";
            case "fence_gate" -> "oak_fence_gate";
            case "waterlily" -> "lily_pad";
            case "reeds" -> "sugar_cane";
            case "deadbush" -> "dead_bush";
            case "yellow_flower" -> "dandelion";
            case "mob_spawner" -> "spawner";
            case "stonebrick" -> color(STONEBRICK, meta);
            case "monster_egg" -> color(MONSTER_EGG, meta);
            case "cobblestone_wall" -> meta == 1 ? "mossy_cobblestone_wall" : "cobblestone_wall";
            case "hardened_clay" -> "terracotta";
            case "quartz_ore" -> "nether_quartz_ore";
            case "magma" -> "magma_block";
            case "slime" -> "slime_block";
            case "lit_furnace" -> "furnace";
            case "lit_redstone_ore" -> "redstone_ore";
            case "lit_redstone_lamp" -> "redstone_lamp";
            case "unlit_redstone_torch" -> "redstone_torch";
            case "powered_repeater", "unpowered_repeater" -> "repeater";
            case "powered_comparator", "unpowered_comparator" -> "comparator";
            case "wall_sign" -> "oak_wall_sign";
            case "standing_sign" -> "oak_sign";
            case "wall_banner" -> "white_wall_banner";
            case "standing_banner" -> "white_banner";
            case "stone_stairs" -> "cobblestone_stairs";
            case "wooden_slab" -> color(WOOD_SLAB, meta & 7);
            case "double_wooden_slab" -> color(PLANKS, meta & 7);
            case "stone_slab" -> color(STONE_SLAB, meta & 7);
            case "double_stone_slab" -> color(DOUBLE_STONE_SLAB, meta & 7);
            case "stone_slab2" -> "red_sandstone_slab";
            case "double_stone_slab2" -> "red_sandstone";
            case "leaves" -> color(LEAVES, meta & 3);
            case "leaves2" -> color(LEAVES2, meta & 3);
            case "sapling" -> color(SAPLING, meta & 7);
            case "tallgrass" -> color(TALLGRASS, meta);
            case "double_plant" -> color(DOUBLE_PLANT, meta & 7);
            case "red_flower" -> color(RED_FLOWER, meta);
            case "skull" -> color(SKULL_BLOCK, meta);
            case "bed" -> color(BED, meta);
            case "anvil" -> color(ANVIL, meta);
            case "quartz_block" -> color(QUARTZ, meta);
            case "sandstone" -> color(SANDSTONE, meta);
            case "red_sandstone" -> color(RED_SANDSTONE, meta);
            case "prismarine" -> color(PRISMARINE, meta);
            case "sponge" -> meta == 1 ? "wet_sponge" : "sponge";
            case "sand" -> meta == 1 ? "red_sand" : "sand";
            case "silver_shulker_box" -> "light_gray_shulker_box";
            default -> flattenShared(path, meta);
        };
    }

    private static String flattenColored(String path, int meta) {
        return switch (path) {
            case "wool" -> color(WOOL, meta);
            case "stained_hardened_clay" -> color(TERRACOTTA, meta);
            case "stained_glass" -> color(GLASS, meta);
            case "stained_glass_pane" -> color(GLASS_PANE, meta);
            case "carpet" -> color(CARPET, meta);
            case "concrete" -> color(CONCRETE, meta);
            case "concrete_powder" -> color(CONCRETE_POWDER, meta);
            case "planks" -> color(PLANKS, meta);
            case "log" -> color(LOG, meta & 3);
            case "log2" -> color(LOG2, meta & 1);
            default -> null;
        };
    }

    private static String flattenShared(String path, int meta) {
        return switch (path) {
            case "stone" -> color(STONE, meta);
            case "dirt" -> meta == 1 ? "coarse_dirt" : meta == 2 ? "podzol" : "dirt";
            default -> null;
        };
    }

    static BlockState blockState(String raw, int meta) {
        ResourceLocation location = ResourceLocation.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
        if (location == null) {
            return null;
        }
        String mapped = mapBlockId(location, meta);
        ResourceLocation resolved = ResourceLocation.tryParse(mapped);
        if (resolved == null || !BuiltInRegistries.BLOCK.containsKey(resolved)) {
            return null;
        }
        return BuiltInRegistries.BLOCK.get(resolved).defaultBlockState();
    }

    private static BlockState blockStateFromPalette(String raw, CompoundTag properties) {
        ResourceLocation location = ResourceLocation.tryParse(raw == null ? "" : raw.contains(":") ? raw : "minecraft:" + raw);
        if (location == null) {
            return null;
        }
        String mapped = mapBlockId(location, 0);
        ResourceLocation resolved = ResourceLocation.tryParse(mapped);
        if (resolved == null || !BuiltInRegistries.BLOCK.containsKey(resolved)) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(resolved);
        BlockState state = block.defaultBlockState();
        if (properties == null || properties.isEmpty()) {
            return state;
        }
        for (String key : properties.getAllKeys()) {
            Property<?> property = block.getStateDefinition().getProperty(key);
            if (property == null) {
                continue;
            }
            state = setProperty(state, property, properties.getString(key));
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
    }

    static String mapBlockId(ResourceLocation location, int meta) {
        if ("enderutilities".equals(location.getNamespace())) {
            String item = mapEnderUtilitiesItem(location.getPath(), meta);
            return item == null ? location.toString() : item;
        }
        if (!"minecraft".equals(location.getNamespace())) {
            return location.toString();
        }
        String flattened = flattenMinecraftBlock(location.getPath(), meta);
        return flattened == null ? location.toString() : "minecraft:" + flattened;
    }

    private static String color(String[] names, int meta) {
        if (meta < 0 || meta >= names.length) {
            return names[0];
        }
        return names[meta];
    }

    private static BlockPos readIntPos(ListTag list) {
        if (list.size() < 3) {
            return BlockPos.ZERO;
        }
        return new BlockPos(list.getInt(0), list.getInt(1), list.getInt(2));
    }

    private static Vec3 readDoublePos(ListTag list) {
        if (list.size() < 3) {
            return Vec3.ZERO;
        }
        return new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
    }

    private static String describeItem(CompoundTag tag) {
        return readIdString(tag) + "#" + (tag.contains("Damage") ? tag.getShort("Damage") : 0);
    }
}
