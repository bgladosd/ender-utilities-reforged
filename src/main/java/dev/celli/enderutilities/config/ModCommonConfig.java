package dev.celli.enderutilities.config;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModCommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Log extra debug information while the mod is loading and running.")
            .define("debugLogging", false);

    public static final ModConfigSpec.BooleanValue ENDER_LASSO_ALLOW_PLAYERS = BUILDER
            .comment("Is the Ender Lasso allowed to teleport players (directly or in a 'stack' riding something)")
            .define("enderLassoAllowPlayers", true);

    public static final ModConfigSpec.BooleanValue USE_ENDER_CHARGE = BUILDER
            .comment("Whether modular items consume Ender Charge from an installed capacitor")
            .define("useEnderCharge", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENERGY_BRIDGE_BEDROCK_WHITELIST = BUILDER
            .comment("Blocks treated as bedrock for Energy Bridge vertical beams (opaque blocks stop the beam unless listed)")
            .defineListAllowEmpty("energyBridgeBedrockWhitelist", List.of("minecraft:bedrock"), () -> "", o -> o instanceof String);

    public static final ModConfigSpec.BooleanValue ENDER_BOW_ALLOW_SELF_TP = BUILDER
            .comment("Allow the Ender Bow to teleport the shooter")
            .define("enderBowAllowSelfTP", true);

    public static final ModConfigSpec.BooleanValue ENDER_BOW_ALLOW_PLAYERS = BUILDER
            .comment("Allow the Ender Bow to teleport players in target mode")
            .define("enderBowAllowPlayers", true);

    public static final ModConfigSpec.IntValue MSU_MAX_ITEMS = BUILDER
            .comment("Maximum items stored per slot in Massive Storage Units")
            .defineInRange("msuMaxItems", 1_000_000_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue BARREL_CAPACITY_UPGRADE_STACKS_PER = BUILDER
            .comment("How many extra 64-item stacks each Barrel Capacity Upgrade adds")
            .defineInRange("barrelCapacityUpgradeStacksPer", 256, 1, 1024);

    public static final ModConfigSpec.IntValue BARREL_MAX_CAPACITY_UPGRADES = BUILDER
            .comment("Maximum Barrel Capacity Upgrades in one barrel")
            .defineInRange("barrelMaxCapacityUpgrades", 64, 1, 256);

    public static final ModConfigSpec.IntValue PORTAL_AREA_CHECK_LIMIT = BUILDER
            .comment("How many blocks to check at most when checking that one portal area is valid")
            .defineInRange("portalAreaCheckLimit", 10000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue PORTAL_FRAME_CHECK_LIMIT = BUILDER
            .comment("How many Portal Frame blocks to check at most")
            .defineInRange("portalFrameCheckLimit", 2000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue PORTAL_LOOP_CHECK_LIMIT = BUILDER
            .comment("How many blocks to check at most when checking portal enclosing loops")
            .defineInRange("portalLoopCheckLimit", 2000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue HANDY_BAG_OPEN_REQUIRES_SNEAK = BUILDER
            .comment("If true, sneak + inventory opens the Handy Bag instead of the player inventory")
            .define("handyBagOpenRequiresSneak", true);

    public static final ModConfigSpec.IntValue ENDER_BUCKET_CAPACITY = BUILDER
            .comment("Capacity of the Ender Bucket in millibuckets when not linked to a tank")
            .defineInRange("enderBucketCapacity", 16000, 1000, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue PORTAL_ONLY_ALLOWS_PORTAL_TYPE_LINK_CRYSTALS = BUILDER
            .comment("If true, then the Portal Panel only accepts Portal type Link Crystals")
            .define("portalOnlyAllowsPortalTypeLinkCrystals", false);

    public static final ModConfigSpec.BooleanValue LMM_LIST_IS_WHITELIST = BUILDER
            .comment("If true, Living Matter Manipulator only stores entities on livingMatterManipulatorWhiteList")
            .define("livingMatterManipulatorListIsWhitelist", false);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> LMM_BLACKLIST = BUILDER
            .comment("Entity ids the Living Matter Manipulator cannot store when using the blacklist")
            .defineListAllowEmpty("livingMatterManipulatorBlackList",
                    List.of("minecraft:ender_dragon", "minecraft:wither"),
                    () -> "", o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> LMM_WHITELIST = BUILDER
            .comment("Entity ids the Living Matter Manipulator can store when using the whitelist")
            .defineListAllowEmpty("livingMatterManipulatorWhiteList", List.of(), () -> "", o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ModCommonConfig() {
    }
}
