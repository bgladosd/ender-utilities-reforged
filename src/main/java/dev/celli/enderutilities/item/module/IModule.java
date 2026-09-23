package dev.celli.enderutilities.item.module;

import net.minecraft.world.item.ItemStack;

public interface IModule {
    ModuleType getModuleType(ItemStack stack);

    int getModuleTier(ItemStack stack);
}
