package dev.celli.enderutilities.data;

import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PetContractData(UUID owner, UUID subject, String entityId, Optional<String> customName, float health) {
    public static final Codec<PetContractData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(PetContractData::owner),
            UUIDUtil.CODEC.fieldOf("subject").forGetter(PetContractData::subject),
            Codec.STRING.fieldOf("entityId").forGetter(PetContractData::entityId),
            Codec.STRING.optionalFieldOf("customName").forGetter(PetContractData::customName),
            Codec.FLOAT.fieldOf("health").forGetter(PetContractData::health)
    ).apply(instance, PetContractData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PetContractData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PetContractData::owner,
            UUIDUtil.STREAM_CODEC, PetContractData::subject,
            ByteBufCodecs.STRING_UTF8, PetContractData::entityId,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), PetContractData::customName,
            ByteBufCodecs.FLOAT, PetContractData::health,
            PetContractData::new
    );
}
