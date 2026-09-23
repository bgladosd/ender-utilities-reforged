package dev.celli.enderutilities.item;

import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModuleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemLinkCrystal extends Item implements IModule {
    public static final int TYPE_LOCATION = 0;
    public static final int TYPE_BLOCK = 1;
    public static final int TYPE_PORTAL = 2;

    private final int crystalType;

    public ItemLinkCrystal(Properties properties, int crystalType) {
        super(properties);
        this.crystalType = crystalType;
    }

    public int crystalType() {
        return crystalType;
    }

    @Override
    public ModuleType getModuleType(ItemStack stack) {
        return ModuleType.LINKCRYSTAL;
    }

    @Override
    public int getModuleTier(ItemStack stack) {
        return crystalType;
    }
}
