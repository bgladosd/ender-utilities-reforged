package dev.celli.enderutilities.data;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record ItemInventoryData(List<StoredModule> slots, int selected, boolean disabled) {
    public static final ItemInventoryData EMPTY = new ItemInventoryData(List.of(), 0, false);

    public static final Codec<ItemInventoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StoredModule.CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(ItemInventoryData::slots),
            Codec.INT.optionalFieldOf("selected", 0).forGetter(ItemInventoryData::selected),
            Codec.BOOL.optionalFieldOf("disabled", false).forGetter(ItemInventoryData::disabled)
    ).apply(instance, ItemInventoryData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemInventoryData> STREAM_CODEC = StreamCodec.composite(
            StoredModule.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemInventoryData::slots,
            ByteBufCodecs.VAR_INT, ItemInventoryData::selected,
            ByteBufCodecs.BOOL, ItemInventoryData::disabled,
            ItemInventoryData::new
    );

    public ItemInventoryData {
        slots = List.copyOf(slots);
    }

    public ItemStack stackInSlot(int slot) {
        for (StoredModule stored : slots) {
            if (stored.slot() == slot) {
                return stored.stack().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemInventoryData withSlot(int slot, ItemStack stack) {
        List<StoredModule> next = new ArrayList<>();
        for (StoredModule stored : slots) {
            if (stored.slot() != slot) {
                next.add(stored);
            }
        }
        if (!stack.isEmpty()) {
            next.add(new StoredModule(slot, stack.copy()));
        }
        next.sort((a, b) -> Integer.compare(a.slot(), b.slot()));
        return new ItemInventoryData(next, selected, disabled);
    }

    public ItemInventoryData withSelected(int value) {
        return new ItemInventoryData(slots, value, disabled);
    }

    public ItemInventoryData withDisabled(boolean value) {
        return new ItemInventoryData(slots, selected, value);
    }
}
