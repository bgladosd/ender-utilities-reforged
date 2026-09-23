package dev.celli.enderutilities.registry;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemBuildersWand;
import dev.celli.enderutilities.item.ItemChairWand;
import dev.celli.enderutilities.item.ItemDolly;
import dev.celli.enderutilities.item.ItemEnderArrow;
import dev.celli.enderutilities.item.ItemEnderBag;
import dev.celli.enderutilities.item.ItemEnderBow;
import dev.celli.enderutilities.item.ItemEnderBucket;
import dev.celli.enderutilities.item.ItemEnderCapacitor;
import dev.celli.enderutilities.item.ItemHandyBag;
import dev.celli.enderutilities.item.ItemInventorySwapper;
import dev.celli.enderutilities.item.ItemBlockPlacementProperty;
import dev.celli.enderutilities.item.ItemLivingManipulator;
import dev.celli.enderutilities.item.ItemNullifier;
import dev.celli.enderutilities.item.ItemPickupManager;
import dev.celli.enderutilities.item.ItemPortalScaler;
import dev.celli.enderutilities.item.ItemQuickStacker;
import dev.celli.enderutilities.item.ItemEnderCore;
import dev.celli.enderutilities.item.ItemEnderLasso;
import dev.celli.enderutilities.item.ItemEnderPearlReusable;
import dev.celli.enderutilities.item.ItemEnderPorter;
import dev.celli.enderutilities.item.ItemEnderRelic;
import dev.celli.enderutilities.item.ItemEnderUtilities;
import dev.celli.enderutilities.item.ItemIceMelter;
import dev.celli.enderutilities.item.ItemJailerModule;
import dev.celli.enderutilities.item.ItemLinkCrystal;
import dev.celli.enderutilities.item.ItemLocationBoundCrystal;
import dev.celli.enderutilities.item.ItemMemoryCard;
import dev.celli.enderutilities.item.ItemMobHarness;
import dev.celli.enderutilities.item.ItemModulePart;
import dev.celli.enderutilities.item.ItemPetContract;
import dev.celli.enderutilities.item.ItemRuler;
import dev.celli.enderutilities.item.ItemSyringe;
import dev.celli.enderutilities.item.module.ModuleType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnderUtilities.MOD_ID);

    public static final DeferredItem<Item> ENDER_ALLOY_BASIC = ITEMS.register("enderpart_enderalloy_0",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_ALLOY_ENHANCED = ITEMS.register("enderpart_enderalloy_1",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_ALLOY_ADVANCED = ITEMS.register("enderpart_enderalloy_2",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_CORE_ENHANCED_INACTIVE = ITEMS.register("enderpart_endercore_1_inactive",
            () -> new ItemEnderCore(new Item.Properties(), 1, false));
    public static final DeferredItem<Item> ENDER_CORE_BASIC_INACTIVE = ITEMS.register("enderpart_endercore_0_inactive",
            () -> new ItemEnderCore(new Item.Properties(), 0, false));
    public static final DeferredItem<Item> ENDER_CORE_BASIC_ACTIVE = ITEMS.register("enderpart_endercore_0_active",
            () -> new ItemEnderCore(new Item.Properties(), 0, true));
    public static final DeferredItem<Item> ENDER_CORE_ENHANCED_ACTIVE = ITEMS.register("enderpart_endercore_1_active",
            () -> new ItemEnderCore(new Item.Properties(), 1, true));
    public static final DeferredItem<Item> ENDER_CORE_ADVANCED_INACTIVE = ITEMS.register("enderpart_endercore_2_inactive",
            () -> new ItemEnderCore(new Item.Properties(), 2, false));
    public static final DeferredItem<Item> ENDER_CORE_ADVANCED_ACTIVE = ITEMS.register("enderpart_endercore_2_active",
            () -> new ItemEnderCore(new Item.Properties(), 2, true));
    public static final DeferredItem<Item> ENDER_ROPE = ITEMS.register("enderpart_enderrope",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_STICK = ITEMS.register("enderpart_enderstick",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_RELIC = ITEMS.register("enderpart_enderrelic",
            () -> new ItemEnderRelic(new Item.Properties()));
    public static final DeferredItem<Item> CREATIVE_BREAKING = ITEMS.register("enderpart_creative_breaking",
            () -> new ItemModulePart(new Item.Properties(), ModuleType.CREATIVE_BREAKING, 0));
    public static final DeferredItem<Item> MEMORY_CARD_MISC = ITEMS.register("enderpart_memorycard_misc",
            () -> new ItemMemoryCard(new Item.Properties(), ModuleType.MEMORY_CARD_MISC, 0));
    public static final DeferredItem<Item> MEMORY_CARD_ITEMS_6B = ITEMS.register("enderpart_memorycard_items_0",
            () -> new ItemMemoryCard(new Item.Properties(), ModuleType.MEMORY_CARD_ITEMS, 0));
    public static final DeferredItem<Item> MEMORY_CARD_ITEMS_8B = ITEMS.register("enderpart_memorycard_items_1",
            () -> new ItemMemoryCard(new Item.Properties(), ModuleType.MEMORY_CARD_ITEMS, 1));
    public static final DeferredItem<Item> MEMORY_CARD_ITEMS_10B = ITEMS.register("enderpart_memorycard_items_2",
            () -> new ItemMemoryCard(new Item.Properties(), ModuleType.MEMORY_CARD_ITEMS, 2));
    public static final DeferredItem<Item> MEMORY_CARD_ITEMS_12B = ITEMS.register("enderpart_memorycard_items_3",
            () -> new ItemMemoryCard(new Item.Properties(), ModuleType.MEMORY_CARD_ITEMS, 3));
    public static final DeferredItem<Item> BARREL_LABEL = ITEMS.register("enderpart_barrel_upgrade_0",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> BARREL_STRUCTURAL = ITEMS.register("enderpart_barrel_upgrade_1",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> BARREL_CAPACITY = ITEMS.register("enderpart_barrel_upgrade_2",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> BARREL_VOID = ITEMS.register("enderpart_barrel_upgrade_3",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> STORAGE_KEY = ITEMS.register("enderpart_storage_key",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> STORAGE_KEY_CREATIVE = ITEMS.register("enderpart_storage_key_creative",
            () -> new ItemEnderUtilities(new Item.Properties()));
    public static final DeferredItem<Item> JAILER = ITEMS.register("enderpart_jailer", () -> new ItemJailerModule(new Item.Properties()));

    public static final DeferredItem<Item> LINK_CRYSTAL_LOCATION = ITEMS.register("linkcrystal_0",
            () -> new ItemLocationBoundCrystal(new Item.Properties(), ItemLinkCrystal.TYPE_LOCATION));
    public static final DeferredItem<Item> LINK_CRYSTAL_BLOCK = ITEMS.register("linkcrystal_1",
            () -> new ItemLocationBoundCrystal(new Item.Properties(), ItemLinkCrystal.TYPE_BLOCK));
    public static final DeferredItem<Item> LINK_CRYSTAL_PORTAL = ITEMS.register("linkcrystal_2",
            () -> new ItemLocationBoundCrystal(new Item.Properties(), ItemLinkCrystal.TYPE_PORTAL));

    public static final DeferredItem<Item> ENDER_CAPACITOR_BASIC = ITEMS.register("endercapacitor_0",
            () -> new ItemEnderCapacitor(new Item.Properties().stacksTo(1), ItemEnderCapacitor.TIER_BASIC));
    public static final DeferredItem<Item> ENDER_CAPACITOR_ENHANCED = ITEMS.register("endercapacitor_1",
            () -> new ItemEnderCapacitor(new Item.Properties().stacksTo(1), ItemEnderCapacitor.TIER_ENHANCED));
    public static final DeferredItem<Item> ENDER_CAPACITOR_ADVANCED = ITEMS.register("endercapacitor_2",
            () -> new ItemEnderCapacitor(new Item.Properties().stacksTo(1), ItemEnderCapacitor.TIER_ADVANCED));
    public static final DeferredItem<Item> ENDER_CAPACITOR_CREATIVE = ITEMS.register("endercapacitor_3",
            () -> new ItemEnderCapacitor(new Item.Properties().stacksTo(1), ItemEnderCapacitor.TIER_CREATIVE));

    public static final DeferredItem<Item> ENDER_LASSO = ITEMS.register("enderlasso", () -> new ItemEnderLasso(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_BAG = ITEMS.register("enderbag", () -> new ItemEnderBag(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_PEARL_REUSABLE = ITEMS.register("enderpearlreusable",
            () -> new ItemEnderPearlReusable(new Item.Properties(), false));
    public static final DeferredItem<Item> ENDER_PEARL_REUSABLE_ELITE = ITEMS.register("enderpearlreusable_elite",
            () -> new ItemEnderPearlReusable(new Item.Properties(), true));
    public static final DeferredItem<Item> ENDER_PORTER = ITEMS.register("enderporter",
            () -> new ItemEnderPorter(new Item.Properties(), false));
    public static final DeferredItem<Item> ENDER_PORTER_ADVANCED = ITEMS.register("enderporter_advanced",
            () -> new ItemEnderPorter(new Item.Properties(), true));
    public static final DeferredItem<Item> ICE_MELTER = ITEMS.register("icemelter",
            () -> new ItemIceMelter(new Item.Properties(), false));
    public static final DeferredItem<Item> ICE_MELTER_SUPER = ITEMS.register("icemelter_super",
            () -> new ItemIceMelter(new Item.Properties(), true));
    public static final DeferredItem<Item> PET_CONTRACT = ITEMS.register("pet_contract",
            () -> new ItemPetContract(new Item.Properties()));
    public static final DeferredItem<Item> MOB_HARNESS = ITEMS.register("mobharness",
            () -> new ItemMobHarness(new Item.Properties()));
    public static final DeferredItem<Item> SYRINGE_EMPTY = ITEMS.register("syringe_empty",
            () -> new ItemSyringe(new Item.Properties(), ItemSyringe.Kind.EMPTY));
    public static final DeferredItem<Item> SYRINGE_PARALYZER = ITEMS.register("syringe_paralyzer",
            () -> new ItemSyringe(new Item.Properties(), ItemSyringe.Kind.PARALYZER));
    public static final DeferredItem<Item> SYRINGE_STIMULANT = ITEMS.register("syringe_stimulant",
            () -> new ItemSyringe(new Item.Properties(), ItemSyringe.Kind.STIMULANT));
    public static final DeferredItem<Item> SYRINGE_PASSIFIER = ITEMS.register("syringe_passifier",
            () -> new ItemSyringe(new Item.Properties(), ItemSyringe.Kind.PASSIFIER));
    public static final DeferredItem<Item> RULER = ITEMS.register("ruler",
            () -> new ItemRuler(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_ARROW = ITEMS.register("enderarrow",
            () -> new ItemEnderArrow(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_BOW = ITEMS.register("enderbow",
            () -> new ItemEnderBow(new Item.Properties()));
    public static final DeferredItem<Item> ENDER_BUCKET = ITEMS.register("enderbucket",
            () -> new ItemEnderBucket(new Item.Properties()));
    public static final DeferredItem<Item> CHAIR_WAND = ITEMS.register("chairwand",
            () -> new ItemChairWand(new Item.Properties()));
    public static final DeferredItem<Item> BUILDERS_WAND = ITEMS.register("builderswand",
            () -> new ItemBuildersWand(new Item.Properties()));
    public static final DeferredItem<Item> DOLLY = ITEMS.register("dolly",
            () -> new ItemDolly(new Item.Properties()));
    public static final DeferredItem<Item> PORTAL_SCALER = ITEMS.register("portalscaler",
            () -> new ItemPortalScaler(new Item.Properties()));
    public static final DeferredItem<Item> NULLIFIER = ITEMS.register("nullifier",
            () -> new ItemNullifier(new Item.Properties()));
    public static final DeferredItem<Item> QUICK_STACKER = ITEMS.register("quickstacker",
            () -> new ItemQuickStacker(new Item.Properties()));
    public static final DeferredItem<Item> PICKUP_MANAGER = ITEMS.register("pickupmanager",
            () -> new ItemPickupManager(new Item.Properties()));
    public static final DeferredItem<Item> INVENTORY_SWAPPER = ITEMS.register("inventoryswapper",
            () -> new ItemInventorySwapper(new Item.Properties()));
    public static final DeferredItem<Item> LIVING_MANIPULATOR = ITEMS.register("livingmanipulator",
            () -> new ItemLivingManipulator(new Item.Properties()));
    public static final DeferredItem<Item> HANDY_BAG = ITEMS.register("handybag",
            () -> new ItemHandyBag(new Item.Properties(), ItemHandyBag.TIER_1));
    public static final DeferredItem<Item> HANDY_BAG_LARGE = ITEMS.register("handybag_large",
            () -> new ItemHandyBag(new Item.Properties(), ItemHandyBag.TIER_2));
    public static final DeferredItem<Item> ENDER_PICKAXE = ITEMS.register("endertool_enderpickaxe",
            () -> new dev.celli.enderutilities.item.tool.ItemEnderTool(new Item.Properties(),
                    dev.celli.enderutilities.item.tool.ItemEnderTool.Kind.PICKAXE));
    public static final DeferredItem<Item> ENDER_AXE = ITEMS.register("endertool_enderaxe",
            () -> new dev.celli.enderutilities.item.tool.ItemEnderTool(new Item.Properties(),
                    dev.celli.enderutilities.item.tool.ItemEnderTool.Kind.AXE));
    public static final DeferredItem<Item> ENDER_SHOVEL = ITEMS.register("endertool_endershovel",
            () -> new dev.celli.enderutilities.item.tool.ItemEnderTool(new Item.Properties(),
                    dev.celli.enderutilities.item.tool.ItemEnderTool.Kind.SHOVEL));
    public static final DeferredItem<Item> ENDER_HOE = ITEMS.register("endertool_enderhoe",
            () -> new dev.celli.enderutilities.item.tool.ItemEnderTool(new Item.Properties(),
                    dev.celli.enderutilities.item.tool.ItemEnderTool.Kind.HOE));
    public static final DeferredItem<Item> ENDER_SWORD = ITEMS.register("endersword",
            () -> new dev.celli.enderutilities.item.tool.ItemEnderSword(new Item.Properties()));
    public static final DeferredItem<Item> VOID_PICKAXE = ITEMS.register("void_pickaxe",
            () -> new dev.celli.enderutilities.item.tool.ItemVoidPickaxe(new Item.Properties()));

    public static final DeferredItem<BlockItem> ENDER_INFUSER = ModBlocks.registerBlockItem(ITEMS, ModBlocks.ENDER_INFUSER);
    public static final DeferredItem<BlockItem> TOOL_WORKSTATION = ModBlocks.registerBlockItem(ITEMS, ModBlocks.TOOL_WORKSTATION);
    public static final DeferredItem<BlockItem> ENERGY_BRIDGE_RESONATOR = ModBlocks.registerBlockItem(ITEMS, ModBlocks.ENERGY_BRIDGE_RESONATOR);
    public static final DeferredItem<BlockItem> ENERGY_BRIDGE_RECEIVER = ModBlocks.registerBlockItem(ITEMS, ModBlocks.ENERGY_BRIDGE_RECEIVER);
    public static final DeferredItem<BlockItem> ENERGY_BRIDGE_TRANSMITTER = ModBlocks.registerBlockItem(ITEMS, ModBlocks.ENERGY_BRIDGE_TRANSMITTER);
    public static final DeferredItem<BlockItem> ENDER_ELEVATOR = ModBlocks.registerBlockItem(ITEMS, ModBlocks.ENDER_ELEVATOR);
    public static final DeferredItem<BlockItem> ENDER_ELEVATOR_SLAB = ModBlocks.registerBlockItem(ITEMS, ModBlocks.ENDER_ELEVATOR_SLAB);
    public static final DeferredItem<BlockItem> ENDER_ELEVATOR_LAYER = ModBlocks.registerBlockItem(ITEMS, ModBlocks.ENDER_ELEVATOR_LAYER);
    public static final DeferredItem<BlockItem> ASU = ITEMS.register("asu",
            () -> ItemBlockPlacementProperty.asu(ModBlocks.ASU.get()));
    public static final DeferredItem<BlockItem> BARREL = ModBlocks.registerBlockItem(ITEMS, ModBlocks.BARREL);
    public static final DeferredItem<BlockItem> DRAW_BRIDGE = ITEMS.register("draw_bridge",
            () -> ItemBlockPlacementProperty.drawbridge(ModBlocks.DRAW_BRIDGE.get(), false));
    public static final DeferredItem<BlockItem> DRAW_BRIDGE_ADVANCED = ITEMS.register("draw_bridge_advanced",
            () -> ItemBlockPlacementProperty.drawbridge(ModBlocks.DRAW_BRIDGE_ADVANCED.get(), true));
    public static final DeferredItem<BlockItem> ENDER_FURNACE = ModBlocks.registerBlockItem(ITEMS, ModBlocks.ENDER_FURNACE);
    public static final DeferredItem<BlockItem> FLOOR = ModBlocks.registerBlockItem(ITEMS, ModBlocks.FLOOR);
    public static final DeferredItem<BlockItem> FLOOR_CRACKED = ModBlocks.registerBlockItem(ITEMS, ModBlocks.FLOOR_CRACKED);
    public static final DeferredItem<BlockItem> INSERTER = ITEMS.register("inserter",
            () -> ItemBlockPlacementProperty.inserter(ModBlocks.INSERTER.get()));
    public static final DeferredItem<BlockItem> INSERTER_FILTERED = ITEMS.register("inserter_filtered",
            () -> ItemBlockPlacementProperty.inserter(ModBlocks.INSERTER_FILTERED.get()));
    public static final DeferredItem<BlockItem> CREATION_STATION = ModBlocks.registerBlockItem(ITEMS, ModBlocks.CREATION_STATION);
    public static final DeferredItem<BlockItem> QUICK_STACKER_ADVANCED = ModBlocks.registerBlockItem(ITEMS, ModBlocks.QUICK_STACKER_ADVANCED);
    public static final DeferredItem<BlockItem> MOLECULAR_EXCITER = ModBlocks.registerBlockItem(ITEMS, ModBlocks.MOLECULAR_EXCITER);
    public static final DeferredItem<BlockItem> MSU = ModBlocks.registerBlockItem(ITEMS, ModBlocks.MSU);
    public static final DeferredItem<BlockItem> MSU_BUNDLE = ModBlocks.registerBlockItem(ITEMS, ModBlocks.MSU_BUNDLE);
    public static final DeferredItem<BlockItem> PHASING = ModBlocks.registerBlockItem(ITEMS, ModBlocks.PHASING);
    public static final DeferredItem<BlockItem> PHASING_INVERTED = ModBlocks.registerBlockItem(ITEMS, ModBlocks.PHASING_INVERTED);
    public static final DeferredItem<BlockItem> PORTAL_FRAME = ModBlocks.registerBlockItem(ITEMS, ModBlocks.PORTAL_FRAME);
    public static final DeferredItem<BlockItem> PORTAL_PANEL = ModBlocks.registerBlockItem(ITEMS, ModBlocks.PORTAL_PANEL);
    public static final DeferredItem<BlockItem> SOUND_BLOCK = ModBlocks.registerBlockItem(ITEMS, ModBlocks.SOUND_BLOCK);
    public static final DeferredItem<BlockItem> MEMORY_CHEST_0 = ModBlocks.registerBlockItem(ITEMS, ModBlocks.MEMORY_CHEST_0);
    public static final DeferredItem<BlockItem> MEMORY_CHEST_1 = ModBlocks.registerBlockItem(ITEMS, ModBlocks.MEMORY_CHEST_1);
    public static final DeferredItem<BlockItem> MEMORY_CHEST_2 = ModBlocks.registerBlockItem(ITEMS, ModBlocks.MEMORY_CHEST_2);
    public static final DeferredItem<BlockItem> HANDY_CHEST_0 = ModBlocks.registerBlockItem(ITEMS, ModBlocks.HANDY_CHEST_0);
    public static final DeferredItem<BlockItem> HANDY_CHEST_1 = ModBlocks.registerBlockItem(ITEMS, ModBlocks.HANDY_CHEST_1);
    public static final DeferredItem<BlockItem> HANDY_CHEST_2 = ModBlocks.registerBlockItem(ITEMS, ModBlocks.HANDY_CHEST_2);
    public static final DeferredItem<BlockItem> HANDY_CHEST_3 = ModBlocks.registerBlockItem(ITEMS, ModBlocks.HANDY_CHEST_3);
    public static final DeferredItem<BlockItem> JSU = ModBlocks.registerBlockItem(ITEMS, ModBlocks.JSU);

    private ModItems() {
    }
}
