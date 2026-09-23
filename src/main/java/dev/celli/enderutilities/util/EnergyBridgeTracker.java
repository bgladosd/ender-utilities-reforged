package dev.celli.enderutilities.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class EnergyBridgeTracker extends SavedData {
    private static final String DATA_NAME = "enderutilities_energybridges";

    private final Set<BridgePos> locations = new HashSet<>();
    private final Map<ResourceLocation, Integer> counts = new HashMap<>();

    public EnergyBridgeTracker() {
    }

    public static EnergyBridgeTracker get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EnergyBridgeTracker::new, EnergyBridgeTracker::load, DataFixTypes.LEVEL),
                DATA_NAME);
    }

    public static EnergyBridgeTracker load(CompoundTag nbt, HolderLookup.Provider registries) {
        EnergyBridgeTracker data = new EnergyBridgeTracker();
        ListTag list = nbt.getList("EnergyBridges", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation dim = parseDimension(tag);
            if (dim == null || !tag.contains("posX") || !tag.contains("posY") || !tag.contains("posZ")) {
                continue;
            }
            data.addLocation(new BridgePos(dim, new BlockPos(tag.getInt("posX"), tag.getInt("posY"), tag.getInt("posZ"))), false);
        }
        return data;
    }

    private static ResourceLocation parseDimension(CompoundTag tag) {
        if (tag.contains("DimId", Tag.TAG_STRING)) {
            return ResourceLocation.tryParse(tag.getString("DimId"));
        }
        if (tag.contains("Dim", Tag.TAG_INT) && tag.getInt("Dim") == 1) {
            return Level.END.location();
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        if (!locations.isEmpty()) {
            ListTag list = new ListTag();
            for (BridgePos pos : locations) {
                CompoundTag tag = new CompoundTag();
                tag.putString("DimId", pos.dimension().toString());
                tag.putInt("posX", pos.pos().getX());
                tag.putInt("posY", pos.pos().getY());
                tag.putInt("posZ", pos.pos().getZ());
                list.add(tag);
            }
            nbt.put("EnergyBridges", list);
            nbt.putInt("BridgeCountInEnd", counts.getOrDefault(Level.END.location(), 0));
        }
        return nbt;
    }

    public static void addBridgeLocation(Level level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        EnergyBridgeTracker tracker = get(server);
        tracker.addLocation(new BridgePos(level.dimension().location(), pos), true);
    }

    public static void removeBridgeLocation(Level level, BlockPos posIn) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        EnergyBridgeTracker tracker = get(server);
        BridgePos pos = new BridgePos(level.dimension().location(), posIn);
        if (tracker.locations.remove(pos)) {
            ResourceLocation dim = pos.dimension();
            int count = tracker.counts.getOrDefault(dim, 0);
            if (count <= 1) {
                tracker.counts.remove(dim);
            } else {
                tracker.counts.put(dim, count - 1);
            }
            tracker.setDirty();
        }
    }

    public static boolean dimensionHasEnergyBridge(Level level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }
        return get(server).counts.getOrDefault(level.dimension().location(), 0) > 0;
    }

    public static boolean dimensionHasEnergyBridge(ResourceKey<Level> dimension, MinecraftServer server) {
        return get(server).counts.getOrDefault(dimension.location(), 0) > 0;
    }

    public static boolean endHasEnergyBridges(Level level) {
        MinecraftServer server = level.getServer();
        return server != null && endHasEnergyBridges(server);
    }

    public static boolean endHasEnergyBridges(MinecraftServer server) {
        return get(server).counts.getOrDefault(Level.END.location(), 0) > 0;
    }

    private void addLocation(BridgePos pos, boolean markDirty) {
        if (!locations.contains(pos)) {
            locations.add(pos);
            counts.merge(pos.dimension(), 1, Integer::sum);
        }
        if (markDirty) {
            setDirty();
        }
    }

    private record BridgePos(ResourceLocation dimension, BlockPos pos) {
    }
}
