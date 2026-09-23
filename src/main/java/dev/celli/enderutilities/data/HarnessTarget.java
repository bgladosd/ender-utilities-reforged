package dev.celli.enderutilities.data;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HarnessTarget(UUID uuid, String name, byte mode) {
    public static final Codec<HarnessTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(HarnessTarget::uuid),
            Codec.STRING.fieldOf("name").forGetter(HarnessTarget::name),
            Codec.BYTE.fieldOf("mode").forGetter(HarnessTarget::mode)
    ).apply(instance, HarnessTarget::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HarnessTarget> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, HarnessTarget::uuid,
            ByteBufCodecs.STRING_UTF8, HarnessTarget::name,
            ByteBufCodecs.BYTE, HarnessTarget::mode,
            HarnessTarget::new
    );
}
