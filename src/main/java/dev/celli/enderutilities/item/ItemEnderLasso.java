package dev.celli.enderutilities.item;

import dev.celli.enderutilities.item.module.IModule;
import dev.celli.enderutilities.item.module.ModuleType;
import net.minecraft.world.item.ItemStack;

public class ItemEnderLasso extends ItemLocationBoundModular {
    public static final int ENDER_CHARGE_COST = 1000;

    public ItemEnderLasso(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean useBindLocking(ItemStack stack) {
        return true;
    }

    @Override
    protected String commonTooltipKey() {
        return "enderutilities.tooltips.itemlocationboundmodular";
    }

    @Override
    public int getMaxModules(ItemStack container) {
        return 10;
    }

    @Override
    public int getMaxModules(ItemStack container, ModuleType moduleType) {
        return switch (moduleType) {
            case ENDERCAPACITOR, MOBPERSISTENCE -> 1;
            case LINKCRYSTAL -> 8;
            default -> 0;
        };
    }

    @Override
    public int getMaxModules(ItemStack container, ItemStack moduleStack) {
        if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof IModule module)) {
            return 0;
        }
        ModuleType type = module.getModuleType(moduleStack);
        if (type != ModuleType.LINKCRYSTAL || module.getModuleTier(moduleStack) == ItemLinkCrystal.TYPE_LOCATION) {
            return getMaxModules(container, type);
        }
        return 0;
    }
}
