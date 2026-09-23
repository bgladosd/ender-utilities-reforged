package dev.celli.enderutilities.registry;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.item.ItemEnderCapacitor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnderUtilities.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("enderutilities",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.enderutilities"))
                    .icon(() -> new ItemStack(ModItems.ENDER_PEARL_REUSABLE_ELITE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ENDER_ALLOY_BASIC.get());
                        output.accept(ModItems.ENDER_ALLOY_ENHANCED.get());
                        output.accept(ModItems.ENDER_ALLOY_ADVANCED.get());
                        output.accept(ModItems.ENDER_CORE_BASIC_INACTIVE.get());
                        output.accept(ModItems.ENDER_CORE_BASIC_ACTIVE.get());
                        output.accept(ModItems.ENDER_CORE_ENHANCED_INACTIVE.get());
                        output.accept(ModItems.ENDER_CORE_ENHANCED_ACTIVE.get());
                        output.accept(ModItems.ENDER_CORE_ADVANCED_INACTIVE.get());
                        output.accept(ModItems.ENDER_CORE_ADVANCED_ACTIVE.get());
                        output.accept(ModItems.ENDER_STICK.get());
                        output.accept(ModItems.ENDER_ROPE.get());
                        output.accept(ModItems.ENDER_RELIC.get());
                        output.accept(ModItems.JAILER.get());
                        output.accept(ModItems.CREATIVE_BREAKING.get());
                        output.accept(ModItems.MEMORY_CARD_MISC.get());
                        output.accept(ModItems.MEMORY_CARD_ITEMS_6B.get());
                        output.accept(ModItems.MEMORY_CARD_ITEMS_8B.get());
                        output.accept(ModItems.MEMORY_CARD_ITEMS_10B.get());
                        output.accept(ModItems.MEMORY_CARD_ITEMS_12B.get());
                        output.accept(ModItems.BARREL_LABEL.get());
                        output.accept(ModItems.BARREL_STRUCTURAL.get());
                        output.accept(ModItems.BARREL_CAPACITY.get());
                        output.accept(ModItems.BARREL_VOID.get());
                        output.accept(ModItems.STORAGE_KEY.get());
                        output.accept(ModItems.STORAGE_KEY_CREATIVE.get());
                        output.accept(ModItems.LINK_CRYSTAL_LOCATION.get());
                        output.accept(ModItems.LINK_CRYSTAL_BLOCK.get());
                        output.accept(ModItems.LINK_CRYSTAL_PORTAL.get());
                        acceptCapacitor(output, ModItems.ENDER_CAPACITOR_BASIC);
                        acceptCapacitor(output, ModItems.ENDER_CAPACITOR_ENHANCED);
                        acceptCapacitor(output, ModItems.ENDER_CAPACITOR_ADVANCED);
                        acceptCapacitor(output, ModItems.ENDER_CAPACITOR_CREATIVE);
                        output.accept(ModItems.ENDER_LASSO.get());
                        output.accept(ModItems.ENDER_BAG.get());
                        output.accept(ModItems.ENDER_PEARL_REUSABLE.get());
                        output.accept(ModItems.ENDER_PEARL_REUSABLE_ELITE.get());
                        output.accept(ModItems.ENDER_PORTER.get());
                        output.accept(ModItems.ENDER_PORTER_ADVANCED.get());
                        output.accept(ModItems.ICE_MELTER.get());
                        output.accept(ModItems.ICE_MELTER_SUPER.get());
                        output.accept(ModItems.PET_CONTRACT.get());
                        output.accept(ModItems.MOB_HARNESS.get());
                        output.accept(ModItems.SYRINGE_EMPTY.get());
                        output.accept(ModItems.SYRINGE_PARALYZER.get());
                        output.accept(ModItems.SYRINGE_STIMULANT.get());
                        output.accept(ModItems.SYRINGE_PASSIFIER.get());
                        output.accept(ModItems.RULER.get());
                        output.accept(ModItems.ENDER_ARROW.get());
                        output.accept(ModItems.ENDER_BOW.get());
                        output.accept(ModItems.ENDER_BUCKET.get());
                        output.accept(ModItems.CHAIR_WAND.get());
                        output.accept(ModItems.BUILDERS_WAND.get());
                        output.accept(ModItems.DOLLY.get());
                        output.accept(ModItems.PORTAL_SCALER.get());
                        output.accept(ModItems.NULLIFIER.get());
                        output.accept(ModItems.QUICK_STACKER.get());
                        output.accept(ModItems.PICKUP_MANAGER.get());
                        output.accept(ModItems.INVENTORY_SWAPPER.get());
                        output.accept(ModItems.LIVING_MANIPULATOR.get());
                        output.accept(ModItems.HANDY_BAG.get());
                        output.accept(ModItems.HANDY_BAG_LARGE.get());
                        output.accept(ModItems.ENDER_PICKAXE.get());
                        output.accept(ModItems.ENDER_AXE.get());
                        output.accept(ModItems.ENDER_SHOVEL.get());
                        output.accept(ModItems.ENDER_HOE.get());
                        output.accept(ModItems.ENDER_SWORD.get());
                        output.accept(ModItems.VOID_PICKAXE.get());
                        output.accept(ModItems.TOOL_WORKSTATION.get());
                        output.accept(ModItems.ENDER_INFUSER.get());
                        output.accept(ModItems.ENERGY_BRIDGE_RESONATOR.get());
                        output.accept(ModItems.ENERGY_BRIDGE_RECEIVER.get());
                        output.accept(ModItems.ENERGY_BRIDGE_TRANSMITTER.get());
                        output.accept(ModItems.ENDER_ELEVATOR.get());
                        output.accept(ModItems.ENDER_ELEVATOR_SLAB.get());
                        output.accept(ModItems.ENDER_ELEVATOR_LAYER.get());
                        output.accept(ModItems.ASU.get());
                        output.accept(ModItems.BARREL.get());
                        output.accept(ModItems.DRAW_BRIDGE.get());
                        output.accept(ModItems.DRAW_BRIDGE_ADVANCED.get());
                        output.accept(ModItems.ENDER_FURNACE.get());
                        output.accept(ModItems.FLOOR.get());
                        output.accept(ModItems.FLOOR_CRACKED.get());
                        output.accept(ModItems.INSERTER.get());
                        output.accept(ModItems.INSERTER_FILTERED.get());
                        output.accept(ModItems.CREATION_STATION.get());
                        output.accept(ModItems.QUICK_STACKER_ADVANCED.get());
                        output.accept(ModItems.MOLECULAR_EXCITER.get());
                        output.accept(ModItems.MSU.get());
                        output.accept(ModItems.MSU_BUNDLE.get());
                        output.accept(ModItems.PHASING.get());
                        output.accept(ModItems.PHASING_INVERTED.get());
                        output.accept(ModItems.PORTAL_FRAME.get());
                        output.accept(ModItems.PORTAL_PANEL.get());
                        output.accept(ModItems.SOUND_BLOCK.get());
                        output.accept(ModItems.MEMORY_CHEST_0.get());
                        output.accept(ModItems.MEMORY_CHEST_1.get());
                        output.accept(ModItems.MEMORY_CHEST_2.get());
                        output.accept(ModItems.HANDY_CHEST_0.get());
                        output.accept(ModItems.HANDY_CHEST_1.get());
                        output.accept(ModItems.HANDY_CHEST_2.get());
                        output.accept(ModItems.HANDY_CHEST_3.get());
                        output.accept(ModItems.JSU.get());
                    })
                    .build());

    private static void acceptCapacitor(CreativeModeTab.Output output, DeferredItem<Item> item) {
        ItemEnderCapacitor capacitor = (ItemEnderCapacitor) item.get();
        output.accept(item.get());
        output.accept(capacitor.createChargedStack());
    }

    private ModCreativeTabs() {
    }
}
