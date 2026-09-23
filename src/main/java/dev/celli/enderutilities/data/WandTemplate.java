package dev.celli.enderutilities.data;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record WandTemplate(List<Entry> blocks, String name, List<EntityEntry> entities, Direction facing) {
    public static final WandTemplate EMPTY = new WandTemplate(List.of(), "", List.of(), Direction.NORTH);

    public record Entry(BlockPos offset, BlockState state, Optional<CompoundTag> nbt) {
        public Entry(BlockPos offset, BlockState state) {
            this(offset, state, Optional.empty());
        }

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("offset").forGetter(Entry::offset),
                BlockState.CODEC.fieldOf("state").forGetter(Entry::state),
                CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(Entry::nbt)
        ).apply(instance, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Entry::offset,
                ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), Entry::state,
                ByteBufCodecs.optional(ByteBufCodecs.COMPOUND_TAG), Entry::nbt,
                Entry::new
        );
    }

    public record EntityEntry(Vec3 pos, BlockPos blockPos, CompoundTag nbt) {
        public static final Codec<EntityEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Vec3.CODEC.fieldOf("pos").forGetter(EntityEntry::pos),
                BlockPos.CODEC.fieldOf("block_pos").forGetter(EntityEntry::blockPos),
                CompoundTag.CODEC.fieldOf("nbt").forGetter(EntityEntry::nbt)
        ).apply(instance, EntityEntry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, EntityEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, e -> e.pos().x,
                ByteBufCodecs.DOUBLE, e -> e.pos().y,
                ByteBufCodecs.DOUBLE, e -> e.pos().z,
                BlockPos.STREAM_CODEC, EntityEntry::blockPos,
                ByteBufCodecs.COMPOUND_TAG, EntityEntry::nbt,
                (x, y, z, blockPos, nbt) -> new EntityEntry(new Vec3(x, y, z), blockPos, nbt)
        );
    }

    public static final Codec<WandTemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("blocks").forGetter(WandTemplate::blocks),
            Codec.STRING.optionalFieldOf("name", "").forGetter(WandTemplate::name),
            EntityEntry.CODEC.listOf().optionalFieldOf("entities", List.of()).forGetter(WandTemplate::entities),
            Direction.CODEC.optionalFieldOf("facing", Direction.NORTH).forGetter(WandTemplate::facing)
    ).apply(instance, WandTemplate::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WandTemplate> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), WandTemplate::blocks,
            ByteBufCodecs.STRING_UTF8, WandTemplate::name,
            EntityEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), WandTemplate::entities,
            Direction.STREAM_CODEC, WandTemplate::facing,
            WandTemplate::new
    );

    public WandTemplate(List<Entry> blocks, String name) {
        this(blocks, name, List.of(), Direction.NORTH);
    }

    public WandTemplate {
        blocks = List.copyOf(blocks);
        name = name == null ? "" : name;
        entities = List.copyOf(entities);
        facing = facing == null ? Direction.NORTH : facing;
    }

    public WandTemplate withName(String name) {
        return new WandTemplate(blocks, name, entities, facing);
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }
}
