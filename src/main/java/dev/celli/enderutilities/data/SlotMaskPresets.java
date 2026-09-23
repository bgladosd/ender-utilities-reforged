package dev.celli.enderutilities.data;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SlotMaskPresets(List<Long> masks) {
    public static final int PRESETS = 4;
    public static final SlotMaskPresets EMPTY = new SlotMaskPresets(List.of(0L, 0L, 0L, 0L));

    public static final Codec<SlotMaskPresets> CODEC = Codec.LONG.listOf().xmap(SlotMaskPresets::new, SlotMaskPresets::masks);

    public static final StreamCodec<RegistryFriendlyByteBuf, SlotMaskPresets> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, presets -> presets.mask(0),
            ByteBufCodecs.VAR_LONG, presets -> presets.mask(1),
            ByteBufCodecs.VAR_LONG, presets -> presets.mask(2),
            ByteBufCodecs.VAR_LONG, presets -> presets.mask(3),
            (a, b, c, d) -> new SlotMaskPresets(List.of(a, b, c, d))
    );

    public SlotMaskPresets {
        masks = List.copyOf(masks);
    }

    public long mask(int selected) {
        if (selected < 0 || selected >= masks.size()) {
            return 0L;
        }
        return masks.get(selected);
    }

    public SlotMaskPresets withMask(int selected, long mask) {
        List<Long> next = new ArrayList<>(masks);
        while (next.size() < PRESETS) {
            next.add(0L);
        }
        if (selected >= 0 && selected < PRESETS) {
            next.set(selected, mask);
        }
        return new SlotMaskPresets(next);
    }
}
