package dev.celli.enderutilities.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RulerCardData(List<Spot> spots, int selected) {
    public static final RulerCardData EMPTY = new RulerCardData(List.of(), 0);

    public record Spot(Optional<BlockPos> start, Optional<BlockPos> end, boolean renderWithAll) {
        public static final Spot EMPTY = new Spot(Optional.empty(), Optional.empty(), false);

        public static final Codec<Spot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.optionalFieldOf("start").forGetter(Spot::start),
                BlockPos.CODEC.optionalFieldOf("end").forGetter(Spot::end),
                Codec.BOOL.optionalFieldOf("render_all", false).forGetter(Spot::renderWithAll)
        ).apply(instance, Spot::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Spot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Spot::start,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Spot::end,
                ByteBufCodecs.BOOL, Spot::renderWithAll,
                Spot::new
        );
    }

    public static final Codec<RulerCardData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Spot.CODEC.listOf().optionalFieldOf("spots", List.of()).forGetter(RulerCardData::spots),
            Codec.INT.optionalFieldOf("selected", 0).forGetter(RulerCardData::selected)
    ).apply(instance, RulerCardData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RulerCardData> STREAM_CODEC = StreamCodec.composite(
            Spot.STREAM_CODEC.apply(ByteBufCodecs.list()), RulerCardData::spots,
            ByteBufCodecs.VAR_INT, RulerCardData::selected,
            RulerCardData::new
    );

    public RulerCardData {
        spots = List.copyOf(spots);
    }

    public Spot current() {
        if (selected < 0 || selected >= spots.size()) {
            return Spot.EMPTY;
        }
        return spots.get(selected);
    }

    public RulerCardData withCurrent(Spot spot) {
        List<Spot> next = new ArrayList<>(spots);
        while (next.size() <= selected) {
            next.add(Spot.EMPTY);
        }
        if (selected >= 0) {
            next.set(selected, spot);
        }
        return new RulerCardData(next, selected);
    }

    /**
     * Orig {@code cycleLocationSelection}: wrap in {@code 0 .. min(count, 7)} so an extra
     * empty slot can be selected (up to 8 spots).
     */
    public RulerCardData cycle(boolean reverse) {
        int max = Math.min(spots.size(), 7);
        int next = selected + (reverse ? -1 : 1);
        if (next < 0) {
            next = max;
        } else if (next > max) {
            next = 0;
        }
        return new RulerCardData(spots, next);
    }

    public RulerCardData withSelected(int index) {
        return new RulerCardData(spots, Math.max(0, index));
    }
}
