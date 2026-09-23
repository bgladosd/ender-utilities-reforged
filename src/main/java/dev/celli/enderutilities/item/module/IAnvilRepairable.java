package dev.celli.enderutilities.item.module;

import net.minecraft.world.item.ItemStack;

public interface IAnvilRepairable {
    boolean repairItem(ItemStack stack, int amount);

    boolean isRepairItem(ItemStack stackTool, ItemStack stackMaterial);
}
