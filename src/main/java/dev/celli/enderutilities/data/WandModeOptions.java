package dev.celli.enderutilities.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public record WandModeOptions(
        List<Integer> options,
        List<Integer> selections,
        List<ItemStack> blocks,
        List<Integer> areas,
        List<CompoundTag> states,
        Area3D stackArea,
        List<Integer> secondary,
        List<Integer> facings,
        List<Integer> mirrors
) {
    public static final int MODE_COUNT = 16;
    public static final int MAX_BLOCKS = 16;
    public static final int BLOCK_TYPE_TARGETED = -1;
    public static final int BLOCK_TYPE_ADJACENT = -2;
    public static final int MAX_RADIUS = 64;

    public static final int AFFECT_ENTITIES = 1 << 0;
    public static final int ALLOW_DIAGONALS = 1 << 1;
    public static final int AREA_FLIPPED = 1 << 2;
    public static final int BIND_MODE = 1 << 3;
    public static final int CHISELS_AND_BITS_CROSSWORLD = 1 << 4;
    public static final int CONTINUE_THROUGH = 1 << 5;
    public static final int MIRRORED = 1 << 6;
    public static final int MOVE_POSITION = 1 << 7;
    public static final int RENDER_GHOST = 1 << 8;
    public static final int REPLACE_EXISTING = 1 << 9;
    public static final int REPLACE_MODE_IS_AREA = 1 << 10;

    public static final WandModeOptions EMPTY = new WandModeOptions(
            List.of(), List.of(), List.of(), List.of(), List.of(), Area3D.EMPTY, List.of(), List.of(), List.of());

    public static final Codec<WandModeOptions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.listOf().optionalFieldOf("options", List.of()).forGetter(WandModeOptions::options),
            Codec.INT.listOf().optionalFieldOf("selections", List.of()).forGetter(WandModeOptions::selections),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(WandModeOptions::blocks),
            Codec.INT.listOf().optionalFieldOf("areas", List.of()).forGetter(WandModeOptions::areas),
            CompoundTag.CODEC.listOf().optionalFieldOf("states", List.of()).forGetter(WandModeOptions::states),
            Area3D.CODEC.optionalFieldOf("stack_area", Area3D.EMPTY).forGetter(WandModeOptions::stackArea),
            Codec.INT.listOf().optionalFieldOf("secondary", List.of()).forGetter(WandModeOptions::secondary),
            Codec.INT.listOf().optionalFieldOf("facings", List.of()).forGetter(WandModeOptions::facings),
            Codec.INT.listOf().optionalFieldOf("mirrors", List.of()).forGetter(WandModeOptions::mirrors)
    ).apply(instance, WandModeOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WandModeOptions> STREAM_CODEC = StreamCodec.of(
            WandModeOptions::encode, WandModeOptions::decode);

    private static void encode(RegistryFriendlyByteBuf buf, WandModeOptions value) {
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, value.options);
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, value.selections);
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, value.blocks);
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, value.areas);
        ByteBufCodecs.COMPOUND_TAG.apply(ByteBufCodecs.list()).encode(buf, value.states);
        Area3D.STREAM_CODEC.encode(buf, value.stackArea);
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, value.secondary);
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, value.facings);
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, value.mirrors);
    }

    private static WandModeOptions decode(RegistryFriendlyByteBuf buf) {
        return new WandModeOptions(
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf),
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf),
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf),
                ByteBufCodecs.COMPOUND_TAG.apply(ByteBufCodecs.list()).decode(buf),
                Area3D.STREAM_CODEC.decode(buf),
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf),
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf),
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf)
        );
    }

    public WandModeOptions {
        options = List.copyOf(options);
        selections = List.copyOf(selections);
        blocks = List.copyOf(blocks);
        areas = List.copyOf(areas);
        states = List.copyOf(states);
        stackArea = stackArea == null ? Area3D.EMPTY : stackArea;
        secondary = List.copyOf(secondary == null ? List.of() : secondary);
        facings = List.copyOf(facings == null ? List.of() : facings);
        mirrors = List.copyOf(mirrors == null ? List.of() : mirrors);
    }

    public int optionBits(int mode) {
        return mode >= 0 && mode < options.size() ? options.get(mode) : 0;
    }

    public boolean option(int mode, int bit) {
        return (optionBits(mode) & bit) != 0;
    }

    public WandModeOptions toggle(int mode, int bit) {
        return withOptionBits(mode, optionBits(mode) ^ bit);
    }

    public WandModeOptions withOptionBits(int mode, int bits) {
        List<Integer> next = paddedInts(options, MODE_COUNT);
        if (mode >= 0 && mode < MODE_COUNT) {
            next.set(mode, bits);
        }
        return new WandModeOptions(next, selections, blocks, areas, states, stackArea, secondary, facings, mirrors);
    }

    public int selection(int mode) {
        return mode >= 0 && mode < selections.size() ? selections.get(mode) : 0;
    }

    public WandModeOptions withSelection(int mode, int value) {
        List<Integer> next = paddedInts(selections, MODE_COUNT);
        if (mode >= 0 && mode < MODE_COUNT) {
            next.set(mode, value);
        }
        return new WandModeOptions(options, next, blocks, areas, states, stackArea, secondary, facings, mirrors);
    }

    public int secondarySelection(int mode) {
        return mode >= 0 && mode < secondary.size() ? secondary.get(mode) : 0;
    }

    public WandModeOptions withSecondarySelection(int mode, int value) {
        List<Integer> next = paddedInts(secondary, MODE_COUNT);
        if (mode >= 0 && mode < MODE_COUNT) {
            next.set(mode, value);
        }
        return new WandModeOptions(options, selections, blocks, areas, states, stackArea, next, facings, mirrors);
    }

    public Direction facing(int mode) {
        if (mode < 0 || mode >= facings.size()) {
            return null;
        }
        int value = facings.get(mode);
        if (value < 0 || value > 5) {
            return null;
        }
        return Direction.from3DDataValue(value);
    }

    public WandModeOptions withFacing(int mode, Direction facing) {
        List<Integer> next = paddedInts(facings, MODE_COUNT, -1);
        if (mode >= 0 && mode < MODE_COUNT) {
            next.set(mode, facing == null ? -1 : facing.get3DDataValue());
        }
        return new WandModeOptions(options, selections, blocks, areas, states, stackArea, secondary, next, mirrors);
    }

    public int mirrorOrdinal(int mode) {
        return mode >= 0 && mode < mirrors.size() ? mirrors.get(mode) : 0;
    }

    public WandModeOptions withMirror(int mode, int ordinal) {
        List<Integer> next = paddedInts(mirrors, MODE_COUNT);
        if (mode >= 0 && mode < MODE_COUNT) {
            next.set(mode, ordinal);
        }
        return new WandModeOptions(options, selections, blocks, areas, states, stackArea, secondary, facings, next);
    }

    public int areaPacked(int mode) {
        return mode >= 0 && mode < areas.size() ? areas.get(mode) : 0;
    }

    public int rPosH(int mode) {
        return areaPacked(mode) & 0xFF;
    }

    public int rNegH(int mode) {
        return (areaPacked(mode) >> 8) & 0xFF;
    }

    public int rPosV(int mode) {
        return (areaPacked(mode) >> 16) & 0xFF;
    }

    public int rNegV(int mode) {
        return (areaPacked(mode) >> 24) & 0xFF;
    }

    public WandModeOptions withAreaPacked(int mode, int packed) {
        List<Integer> next = paddedInts(areas, MODE_COUNT);
        if (mode >= 0 && mode < MODE_COUNT) {
            next.set(mode, packed);
        }
        return new WandModeOptions(options, selections, blocks, next, states, stackArea, secondary, facings, mirrors);
    }

    public WandModeOptions adjustColumn(int mode, int amount) {
        return withArea(mode, clamp(rPosH(mode) + amount), rNegH(mode), rPosV(mode), rNegV(mode));
    }

    public WandModeOptions withArea(int mode, int posH, int negH, int posV, int negV) {
        int packed = clamp(posH) | (clamp(negH) << 8) | (clamp(posV) << 16) | (clamp(negV) << 24);
        return withAreaPacked(mode, packed);
    }

    public WandModeOptions withStackArea(Area3D area) {
        return new WandModeOptions(options, selections, blocks, areas, states, area, secondary, facings, mirrors);
    }

    public WandModeOptions adjustStackArea(Direction facing, int amount) {
        return withStackArea(stackArea.adjust(facing, amount));
    }

    public ItemStack boundBlock(int index) {
        if (index < 0 || index >= blocks.size()) {
            return ItemStack.EMPTY;
        }
        return blocks.get(index);
    }

    public Optional<BlockState> boundState(int index, net.minecraft.core.HolderLookup.Provider registries) {
        if (index < 0 || index >= states.size()) {
            return Optional.empty();
        }
        CompoundTag tag = states.get(index);
        if (tag == null || tag.isEmpty()) {
            return Optional.empty();
        }
        BlockState state = NbtUtils.readBlockState(registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), tag);
        return state.isAir() ? Optional.empty() : Optional.of(state);
    }

    public WandModeOptions withBoundBlock(int index, ItemStack stack, BlockState state) {
        List<ItemStack> nextBlocks = new ArrayList<>(blocks);
        List<CompoundTag> nextStates = new ArrayList<>(states);
        while (nextBlocks.size() < MAX_BLOCKS) {
            nextBlocks.add(ItemStack.EMPTY);
        }
        while (nextStates.size() < MAX_BLOCKS) {
            nextStates.add(new CompoundTag());
        }
        if (index >= 0 && index < MAX_BLOCKS) {
            nextBlocks.set(index, stack.copyWithCount(1));
            nextStates.set(index, state == null || state.isAir() ? new CompoundTag() : NbtUtils.writeBlockState(state));
        }
        return new WandModeOptions(options, selections, nextBlocks, areas, nextStates, stackArea, secondary, facings, mirrors);
    }

    public WandModeOptions withBoundBlock(int index, ItemStack stack) {
        return withBoundBlock(index, stack, null);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX_RADIUS, value));
    }

    private static List<Integer> paddedInts(List<Integer> source, int size) {
        return paddedInts(source, size, 0);
    }

    private static List<Integer> paddedInts(List<Integer> source, int size, int fill) {
        List<Integer> next = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            next.add(i < source.size() ? source.get(i) : fill);
        }
        return next;
    }

    public record Area3D(int posPacked, int negPacked) {
        public static final Area3D EMPTY = new Area3D(0, 0);

        public static final Codec<Area3D> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("pos", 0).forGetter(Area3D::posPacked),
                Codec.INT.optionalFieldOf("neg", 0).forGetter(Area3D::negPacked)
        ).apply(instance, Area3D::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Area3D> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Area3D::posPacked,
                ByteBufCodecs.VAR_INT, Area3D::negPacked,
                Area3D::new
        );

        public int xp() {
            return (posPacked >> 16) & 0xFF;
        }

        public int yp() {
            return (posPacked >> 8) & 0xFF;
        }

        public int zp() {
            return posPacked & 0xFF;
        }

        public int xn() {
            return (negPacked >> 16) & 0xFF;
        }

        public int yn() {
            return (negPacked >> 8) & 0xFF;
        }

        public int zn() {
            return negPacked & 0xFF;
        }

        public Area3D adjust(Direction facing, int amount) {
            if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                return new Area3D(pack(
                        xp() + step(facing, Direction.Axis.X, amount),
                        yp() + step(facing, Direction.Axis.Y, amount),
                        zp() + step(facing, Direction.Axis.Z, amount)
                ), negPacked);
            }
            return new Area3D(posPacked, pack(
                    xn() + step(facing, Direction.Axis.X, amount),
                    yn() + step(facing, Direction.Axis.Y, amount),
                    zn() + step(facing, Direction.Axis.Z, amount)
            ));
        }

        private static int step(Direction facing, Direction.Axis axis, int amount) {
            return facing.getAxis() == axis ? amount : 0;
        }

        private static int pack(int x, int y, int z) {
            return (clamp(x) << 16) | (clamp(y) << 8) | clamp(z);
        }
    }
}
