package dev.celli.enderutilities.compat.rei;

import java.util.ArrayList;
import java.util.List;

import dev.celli.enderutilities.menu.CreationStationMenu;
import dev.celli.enderutilities.registry.ModBlocks;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.world.inventory.Slot;

@REIPluginClient
public class EnderUtilitiesReiClientPlugin implements REIClientPlugin {
    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.addWorkstations(BuiltinPlugin.CRAFTING, EntryStacks.of(ModBlocks.CREATION_STATION.get()));
        registry.addWorkstations(BuiltinPlugin.SMELTING, EntryStacks.of(ModBlocks.CREATION_STATION.get()));
        registry.addWorkstations(BuiltinPlugin.FUEL, EntryStacks.of(ModBlocks.CREATION_STATION.get()));
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(new CreationStationTransferHandler());
    }

    private static final class CreationStationTransferHandler implements SimpleTransferHandler {
        @Override
        public ApplicabilityResult checkApplicable(TransferHandler.Context context) {
            if (!(context.getMenu() instanceof CreationStationMenu)
                    || !BuiltinPlugin.CRAFTING.equals(context.getDisplay().getCategoryIdentifier())
                    || context.getContainerScreen() == null) {
                return ApplicabilityResult.createNotApplicable();
            }
            return ApplicabilityResult.createApplicable();
        }

        @Override
        public Iterable<SlotAccessor> getInputSlots(TransferHandler.Context context) {
            CreationStationMenu menu = (CreationStationMenu) context.getMenu();
            List<SlotAccessor> slots = new ArrayList<>(9);
            for (Slot slot : menu.recipeTransferCraftingSlots()) {
                slots.add(SlotAccessor.fromSlot(slot));
            }
            return slots;
        }

        @Override
        public Iterable<SlotAccessor> getInventorySlots(TransferHandler.Context context) {
            CreationStationMenu menu = (CreationStationMenu) context.getMenu();
            List<SlotAccessor> slots = new ArrayList<>();
            for (Slot slot : menu.recipeTransferInventorySlots()) {
                slots.add(SlotAccessor.fromSlot(slot));
            }
            return slots;
        }
    }
}
