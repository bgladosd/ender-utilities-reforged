package dev.celli.enderutilities.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Orig {@code Configs} builders-wand ghost options. Client-only rendering.
 */
public final class ModClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue BUILDERS_WAND_USE_TRANSLUCENT_GHOST_BLOCKS = BUILDER
            .comment("Use translucent ghost block rendering instead of opaque")
            .define("buildersWandUseTranslucentGhostBlocks", true);

    public static final ModConfigSpec.DoubleValue BUILDERS_WAND_GHOST_BLOCK_ALPHA = BUILDER
            .comment("The alpha value to use for the translucent ghost block rendering mode")
            .defineInRange("buildersWandGhostBlockAlpha", 0.7d, 0.0d, 1.0d);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ModClientConfig() {
    }

    public static int ghostAlphaByte() {
        return (int) Math.round(BUILDERS_WAND_GHOST_BLOCK_ALPHA.get() * 0xFF) & 0xFF;
    }
}
