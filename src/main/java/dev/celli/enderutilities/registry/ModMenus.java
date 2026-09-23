package dev.celli.enderutilities.registry;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.menu.EnderInfuserMenu;
import dev.celli.enderutilities.menu.ToolWorkstationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EnderUtilities.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ToolWorkstationMenu>> TOOL_WORKSTATION =
            MENUS.register("tool_workstation", () -> IMenuTypeExtension.create(ToolWorkstationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<EnderInfuserMenu>> ENDER_INFUSER =
            MENUS.register("ender_infuser", () -> IMenuTypeExtension.create(EnderInfuserMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.NullifierMenu>> NULLIFIER =
            MENUS.register("nullifier", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.NullifierMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.QuickStackerMenu>> QUICK_STACKER =
            MENUS.register("quickstacker", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.QuickStackerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.HandyBagMenu>> HANDY_BAG =
            MENUS.register("handybag", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.HandyBagMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.PickupManagerMenu>> PICKUP_MANAGER =
            MENUS.register("pickup_manager", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.PickupManagerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.InventorySwapperMenu>> INVENTORY_SWAPPER =
            MENUS.register("inventory_swapper", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.InventorySwapperMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.AsuMenu>> ASU =
            MENUS.register("asu", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.AsuMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.BarrelMenu>> BARREL =
            MENUS.register("barrel", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.BarrelMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.DrawbridgeMenu>> DRAWBRIDGE =
            MENUS.register("draw_bridge", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.DrawbridgeMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.EnderFurnaceMenu>> ENDER_FURNACE =
            MENUS.register("ender_furnace", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.EnderFurnaceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.InserterMenu>> INSERTER =
            MENUS.register("inserter", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.InserterMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.CreationStationMenu>> CREATION_STATION =
            MENUS.register("creation_station", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.CreationStationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.QuickStackerAdvancedMenu>> QUICK_STACKER_ADVANCED =
            MENUS.register("quick_stacker_advanced", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.QuickStackerAdvancedMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.MsuMenu>> MSU =
            MENUS.register("msu", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.MsuMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.SoundBlockMenu>> SOUND_BLOCK =
            MENUS.register("sound_block", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.SoundBlockMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.MemoryChestMenu>> MEMORY_CHEST =
            MENUS.register("memory_chest", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.MemoryChestMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.HandyChestMenu>> HANDY_CHEST =
            MENUS.register("handy_chest", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.HandyChestMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.JsuMenu>> JSU =
            MENUS.register("jsu", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.JsuMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<dev.celli.enderutilities.menu.PortalPanelMenu>> PORTAL_PANEL =
            MENUS.register("portal_panel", () -> IMenuTypeExtension.create(dev.celli.enderutilities.menu.PortalPanelMenu::new));

    private ModMenus() {
    }
}
