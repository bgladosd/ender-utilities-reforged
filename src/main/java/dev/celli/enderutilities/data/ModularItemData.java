package dev.celli.enderutilities.data;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record ModularItemData(List<StoredModule> modules, int selectedLinkCrystal, boolean bindLocked) {
    public static final ModularItemData EMPTY = new ModularItemData(List.of(), 0, false);

    public static final Codec<ModularItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StoredModule.CODEC.listOf().optionalFieldOf("modules", List.of()).forGetter(ModularItemData::modules),
            Codec.INT.optionalFieldOf("selected_link_crystal", 0).forGetter(ModularItemData::selectedLinkCrystal),
            Codec.BOOL.optionalFieldOf("bind_locked", false).forGetter(ModularItemData::bindLocked)
    ).apply(instance, ModularItemData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModularItemData> STREAM_CODEC = StreamCodec.composite(
            StoredModule.STREAM_CODEC.apply(ByteBufCodecs.list()), ModularItemData::modules,
            ByteBufCodecs.VAR_INT, ModularItemData::selectedLinkCrystal,
            ByteBufCodecs.BOOL, ModularItemData::bindLocked,
            ModularItemData::new
    );

    public ModularItemData {
        modules = List.copyOf(modules);
    }

    public ItemStack stackInSlot(int slot) {
        for (StoredModule module : modules) {
            if (module.slot() == slot) {
                return module.stack().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    public ModularItemData withSlot(int slot, ItemStack stack) {
        List<StoredModule> next = new ArrayList<>();
        for (StoredModule module : modules) {
            if (module.slot() != slot) {
                next.add(module);
            }
        }
        if (!stack.isEmpty()) {
            next.add(new StoredModule(slot, stack.copy()));
        }
        next.sort((a, b) -> Integer.compare(a.slot(), b.slot()));
        return new ModularItemData(next, selectedLinkCrystal, bindLocked);
    }

    public ModularItemData withSelectedLinkCrystal(int selected) {
        return new ModularItemData(modules, selected, bindLocked);
    }

    public ModularItemData withBindLocked(boolean locked) {
        return new ModularItemData(modules, selectedLinkCrystal, locked);
    }
}
