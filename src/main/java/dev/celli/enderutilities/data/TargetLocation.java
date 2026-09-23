package dev.celli.enderutilities.data;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record TargetLocation(
        ResourceKey<Level> dimension,
        BlockPos pos,
        Vec3 exact,
        Direction face,
        Optional<ResourceLocation> blockId,
        boolean hasRotation,
        float yaw,
        float pitch
) {
    public static final Codec<TargetLocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(TargetLocation::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(TargetLocation::pos),
            Vec3.CODEC.fieldOf("exact").forGetter(TargetLocation::exact),
            Direction.CODEC.optionalFieldOf("face", Direction.UP).forGetter(TargetLocation::face),
            ResourceLocation.CODEC.optionalFieldOf("block_id").forGetter(TargetLocation::blockId),
            Codec.BOOL.optionalFieldOf("has_rotation", false).forGetter(TargetLocation::hasRotation),
            Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(TargetLocation::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0f).forGetter(TargetLocation::pitch)
    ).apply(instance, TargetLocation::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z,
            Vec3::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TargetLocation> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TargetLocation decode(RegistryFriendlyByteBuf buf) {
            ResourceKey<Level> dimension = ResourceKey.streamCodec(Registries.DIMENSION).decode(buf);
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
            Vec3 exact = VEC3_STREAM_CODEC.decode(buf);
            Direction face = Direction.STREAM_CODEC.decode(buf);
            Optional<ResourceLocation> blockId = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf);
            boolean hasRotation = buf.readBoolean();
            float yaw = buf.readFloat();
            float pitch = buf.readFloat();
            return new TargetLocation(dimension, pos, exact, face, blockId, hasRotation, yaw, pitch);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TargetLocation value) {
            ResourceKey.streamCodec(Registries.DIMENSION).encode(buf, value.dimension());
            BlockPos.STREAM_CODEC.encode(buf, value.pos());
            VEC3_STREAM_CODEC.encode(buf, value.exact());
            Direction.STREAM_CODEC.encode(buf, value.face());
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, value.blockId());
            buf.writeBoolean(value.hasRotation());
            buf.writeFloat(value.yaw());
            buf.writeFloat(value.pitch());
        }
    };

    public boolean isEnderChest() {
        return blockId.filter(id -> id.equals(ResourceLocation.withDefaultNamespace("ender_chest"))).isPresent();
    }

    public boolean isTargetBlockUnchanged(Level level) {
        if (blockId.isEmpty()) {
            return true;
        }
        ResourceLocation current = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        return blockId.get().equals(current);
    }
}
