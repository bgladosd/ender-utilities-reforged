package dev.celli.enderutilities.util;

import java.util.List;

import dev.celli.enderutilities.config.ModCommonConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class WorldUtils {
    private WorldUtils() {
    }

    public static boolean isEndDimension(Level level) {
        return level.dimension() == Level.END;
    }

    public static boolean isBlockValidBedrockForEnergyBridge(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        List<? extends String> whitelist = ModCommonConfig.ENERGY_BRIDGE_BEDROCK_WHITELIST.get();
        if (whitelist.isEmpty()) {
            return state.is(Blocks.BEDROCK);
        }
        return whitelist.contains(id.toString());
    }
}
