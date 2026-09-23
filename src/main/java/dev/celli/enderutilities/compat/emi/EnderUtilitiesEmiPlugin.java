package dev.celli.enderutilities.compat.emi;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.menu.CreationStationMenu;
import dev.celli.enderutilities.registry.ModBlocks;
import dev.celli.enderutilities.registry.ModMenus;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.inventory.Slot;

@EmiEntrypoint
public class EnderUtilitiesEmiPlugin implements EmiPlugin {
    public EnderUtilitiesEmiPlugin() {
    }

    @Override
    public void register(EmiRegistry registry) {
        EmiStack station = EmiStack.of(ModBlocks.CREATION_STATION.get());
        registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, station);
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, station);
        registry.addWorkstation(VanillaEmiRecipeCategories.FUEL, station);
        CreationStationRecipeHandler handler = new CreationStationRecipeHandler();
        registry.addRecipeHandler(ModMenus.CREATION_STATION.get(), handler);
        EnderUtilities.LOGGER.info("Registered EMI Creation Station recipe transfer");
    }

    private static final class CreationStationRecipeHandler implements StandardRecipeHandler<CreationStationMenu> {
        @Override
        public List<Slot> getInputSources(CreationStationMenu menu) {
            List<Slot> slots = new ArrayList<>();
            slots.addAll(menu.recipeTransferInventorySlots());
            slots.addAll(menu.recipeTransferCraftingSlots());
            return slots;
        }

        @Override
        public List<Slot> getCraftingSlots(CreationStationMenu menu) {
            return menu.recipeTransferCraftingSlots();
        }

        @Override
        public Slot getOutputSlot(CreationStationMenu menu) {
            return menu.getSlot(menu.getCraftingResultIndex());
        }

        @Override
        public boolean supportsRecipe(EmiRecipe recipe) {
            return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING;
        }
    }
}
