package dev.celli.enderutilities.item;

import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModuleType;
import net.minecraft.world.item.ItemStack;

public class ItemModulePart extends ItemEnderUtilities implements IModule {
    private final ModuleType type;
    private final int tier;

    public ItemModulePart(Properties properties, ModuleType type, int tier) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    @Override
    public ModuleType getModuleType(ItemStack stack) {
        return type;
    }

    @Override
    public int getModuleTier(ItemStack stack) {
        return tier;
    }
}
