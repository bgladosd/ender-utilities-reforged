package dev.celli.enderutilities.item.module;

import net.minecraft.world.item.ItemStack;

public interface IModular {
    int getMaxModules(ItemStack container);

    int getMaxModules(ItemStack container, ModuleType type);

    int getMaxModules(ItemStack container, ItemStack moduleStack);
}
