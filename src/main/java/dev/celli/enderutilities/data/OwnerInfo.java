package dev.celli.enderutilities.data;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public record OwnerInfo(UUID owner, String name, boolean isPublic) {
    public static final Codec<OwnerInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(OwnerInfo::owner),
            Codec.STRING.fieldOf("name").forGetter(OwnerInfo::name),
            Codec.BOOL.fieldOf("public").forGetter(OwnerInfo::isPublic)
    ).apply(instance, OwnerInfo::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, OwnerInfo> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, OwnerInfo::owner,
            ByteBufCodecs.STRING_UTF8, OwnerInfo::name,
            ByteBufCodecs.BOOL, OwnerInfo::isPublic,
            OwnerInfo::new
    );

    public static OwnerInfo of(Entity entity, boolean isPublic) {
        return new OwnerInfo(entity.getUUID(), entity.getName().getString(), isPublic);
    }

    public boolean canAccess(Entity entity) {
        return isPublic || entity != null && owner.equals(entity.getUUID());
    }

    public boolean isOwner(Player player) {
        return owner.equals(player.getUUID());
    }

    public OwnerInfo withPublic(boolean value) {
        return new OwnerInfo(owner, name, value);
    }
}
