package dev.celli.enderutilities.data;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Orig wrote placement-property values per player on disk. Port stores them on the
 * held stack so G-cycle + place matches orig HUD behaviour without a world save.
 */
public record PlacementPropertyValues(int index, Map<String, Integer> values) {
    public static final PlacementPropertyValues EMPTY = new PlacementPropertyValues(0, Map.of());

    public static final Codec<PlacementPropertyValues> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("index", 0).forGetter(PlacementPropertyValues::index),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("values", Map.of())
                    .forGetter(PlacementPropertyValues::values)
    ).apply(instance, PlacementPropertyValues::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacementPropertyValues> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlacementPropertyValues::index,
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
            value -> new LinkedHashMap<>(value.values()),
            PlacementPropertyValues::new
    );

    public int get(String key, int fallback) {
        return values.getOrDefault(key, fallback);
    }

    public PlacementPropertyValues withIndex(int index) {
        return new PlacementPropertyValues(index, values);
    }

    public PlacementPropertyValues withValue(String key, int value) {
        Map<String, Integer> next = new LinkedHashMap<>(values);
        next.put(key, value);
        return new PlacementPropertyValues(index, Map.copyOf(next));
    }
}
