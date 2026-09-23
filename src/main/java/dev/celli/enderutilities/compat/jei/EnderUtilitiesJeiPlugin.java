package dev.celli.enderutilities.compat.jei;

import java.util.List;
import java.util.Optional;

import dev.celli.enderutilities.EnderUtilities;
import dev.celli.enderutilities.client.CreationStationScreen;
import dev.celli.enderutilities.client.EnderFurnaceScreen;
import dev.celli.enderutilities.menu.CreationStationMenu;
import dev.celli.enderutilities.registry.ModBlocks;
import dev.celli.enderutilities.registry.ModMenus;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class EnderUtilitiesJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(EnderUtilities.MOD_ID, "jei");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new CreationStationTransferInfo());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CREATION_STATION.get()),
                RecipeTypes.CRAFTING, RecipeTypes.SMELTING, RecipeTypes.FUELING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ENDER_FURNACE.get()),
                RecipeTypes.SMELTING, RecipeTypes.FUELING);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(EnderFurnaceScreen.class, 58, 35, 22, 15, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(EnderFurnaceScreen.class, 34, 36, 15, 14, RecipeTypes.FUELING);
        registration.addRecipeClickArea(CreationStationScreen.class, 27, 11, 10, 10, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(CreationStationScreen.class, 9, 29, 15, 14, RecipeTypes.FUELING);
        registration.addRecipeClickArea(CreationStationScreen.class, 203, 11, 10, 10, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(CreationStationScreen.class, 217, 29, 15, 14, RecipeTypes.FUELING);
        registration.addRecipeClickArea(CreationStationScreen.class, 97, 36, 10, 10, RecipeTypes.CRAFTING);
        registration.addRecipeClickArea(CreationStationScreen.class, 133, 72, 10, 10, RecipeTypes.CRAFTING);
    }

    private static final class CreationStationTransferInfo implements IRecipeTransferInfo<CreationStationMenu, RecipeHolder<CraftingRecipe>> {
        @Override
        public Class<? extends CreationStationMenu> getContainerClass() {
            return CreationStationMenu.class;
        }

        @Override
        public Optional<MenuType<CreationStationMenu>> getMenuType() {
            return Optional.of(ModMenus.CREATION_STATION.get());
        }

        @Override
        public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        @Override
        public boolean canHandle(CreationStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
            return true;
        }

        @Override
        public List<Slot> getRecipeSlots(CreationStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
            return container.recipeTransferCraftingSlots();
        }

        @Override
        public List<Slot> getInventorySlots(CreationStationMenu container, RecipeHolder<CraftingRecipe> recipe) {
            return container.recipeTransferInventorySlots();
        }
    }
}
