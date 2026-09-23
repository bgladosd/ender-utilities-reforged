package dev.celli.enderutilities.data;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record CarriedBlock(
        ResourceLocation blockId,
        CompoundTag blockState,
        Optional<CompoundTag> blockEntity,
        Direction pickupFacing,
        String displayName
) {
    public static final Codec<CarriedBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("block").forGetter(CarriedBlock::blockId),
            CompoundTag.CODEC.optionalFieldOf("state", new CompoundTag()).forGetter(CarriedBlock::blockState),
            CompoundTag.CODEC.optionalFieldOf("block_entity").forGetter(CarriedBlock::blockEntity),
            Direction.CODEC.optionalFieldOf("facing", Direction.NORTH).forGetter(CarriedBlock::pickupFacing),
            Codec.STRING.optionalFieldOf("name", "").forGetter(CarriedBlock::displayName)
    ).apply(instance, CarriedBlock::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CarriedBlock> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CarriedBlock::blockId,
            ByteBufCodecs.COMPOUND_TAG, CarriedBlock::blockState,
            ByteBufCodecs.optional(ByteBufCodecs.COMPOUND_TAG), CarriedBlock::blockEntity,
            Direction.STREAM_CODEC, CarriedBlock::pickupFacing,
            ByteBufCodecs.STRING_UTF8, CarriedBlock::displayName,
            CarriedBlock::new
    );

    public static CompoundTag encodeState(BlockState state) {
        return net.minecraft.nbt.NbtUtils.writeBlockState(state);
    }

    public BlockState decodeState(net.minecraft.core.HolderLookup.Provider registries) {
        Block fallback = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockId);
        return net.minecraft.nbt.NbtUtils.readBlockState(registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), blockState)
                .isAir() ? fallback.defaultBlockState() : net.minecraft.nbt.NbtUtils.readBlockState(
                registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), blockState);
    }
}
