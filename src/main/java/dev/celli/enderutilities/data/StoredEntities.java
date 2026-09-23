package dev.celli.enderutilities.data;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StoredEntities(List<CompoundTag> entities, int current) {
    public static final StoredEntities EMPTY = new StoredEntities(List.of(), 0);

    public static final Codec<StoredEntities> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompoundTag.CODEC.listOf().optionalFieldOf("entities", List.of()).forGetter(StoredEntities::entities),
            Codec.INT.optionalFieldOf("current", 0).forGetter(StoredEntities::current)
    ).apply(instance, StoredEntities::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoredEntities> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG.apply(ByteBufCodecs.list()), StoredEntities::entities,
            ByteBufCodecs.VAR_INT, StoredEntities::current,
            StoredEntities::new
    );

    public StoredEntities {
        entities = List.copyOf(entities);
    }

    public StoredEntities withCurrent(int value) {
        return new StoredEntities(entities, value);
    }

    public StoredEntities insert(int index, CompoundTag tag) {
        List<CompoundTag> next = new ArrayList<>(entities);
        int count = next.size();
        int at = index;
        if (at >= count) {
            at = count > 0 ? count - 1 : 0;
        }
        at = Math.max(0, at);
        next.add(at, tag);
        return new StoredEntities(next, at);
    }

    public StoredEntities removeCurrent() {
        if (entities.isEmpty()) {
            return EMPTY;
        }
        int index = Math.max(0, Math.min(current, entities.size() - 1));
        List<CompoundTag> next = new ArrayList<>(entities);
        next.remove(index);
        int nextCurrent = next.isEmpty() ? 0 : Math.min(index, next.size() - 1);
        return new StoredEntities(next, nextCurrent);
    }
}
