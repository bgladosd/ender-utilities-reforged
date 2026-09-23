package dev.celli.enderutilities.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record StoredModule(int slot, ItemStack stack) {
    public static final Codec<StoredModule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("slot").forGetter(StoredModule::slot),
            ItemStack.CODEC.fieldOf("stack").forGetter(StoredModule::stack)
    ).apply(instance, StoredModule::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoredModule> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StoredModule::slot,
            ItemStack.STREAM_CODEC, StoredModule::stack,
            StoredModule::new
    );
}
