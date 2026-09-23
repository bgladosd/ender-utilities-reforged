package dev.celli.enderutilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PortalScale(byte x, byte y, byte z) {
    public static final PortalScale DEFAULT = new PortalScale((byte) 8, (byte) 1, (byte) 8);

    public static final Codec<PortalScale> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BYTE.optionalFieldOf("x", (byte) 8).forGetter(PortalScale::x),
            Codec.BYTE.optionalFieldOf("y", (byte) 1).forGetter(PortalScale::y),
            Codec.BYTE.optionalFieldOf("z", (byte) 8).forGetter(PortalScale::z)
    ).apply(instance, PortalScale::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortalScale> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, PortalScale::x,
            ByteBufCodecs.BYTE, PortalScale::y,
            ByteBufCodecs.BYTE, PortalScale::z,
            PortalScale::new
    );

    public boolean isSet() {
        return x != 0 && y != 0 && z != 0;
    }
}
